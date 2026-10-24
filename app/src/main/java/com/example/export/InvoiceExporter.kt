package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.R
import com.example.model.*
import java.io.File
import java.text.NumberFormat
import java.util.Locale

object InvoiceExporter {
    private fun money(value: Double) = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2; maximumFractionDigits = 2
    }.format(value)

    private fun percentage(rate: Double) = java.math.BigDecimal.valueOf(rate).multiply(java.math.BigDecimal(100))
        .stripTrailingZeros().toPlainString() + "%"

    fun generatePdf(context: Context, invoice: Invoice, company: CompanyProfile,
        accentColorHex: Int = Color.rgb(160, 124, 35)): File =
        generateDocument(context, invoice, company, accentColorHex, false)

    fun generateQuotePdf(context: Context, quote: Quote, company: CompanyProfile,
        accentColorHex: Int = Color.rgb(160, 124, 35)): File =
        generateDocument(context, quote.document, company, accentColorHex, true)

    private fun generateDocument(context: Context, invoice: Invoice, company: CompanyProfile,
        accent: Int, isQuote: Boolean): File {
        val seller = invoice.seller ?: company
        val qr = if (isQuote || invoice.status == InvoiceStatus.DRAFT || seller.vatNumber.isBlank()) null
            else InvoiceQr.bitmap(invoice.generateZatcaTlvBase64(seller.companyName, seller.vatNumber))
        val document = PdfDocument()
        val prefix = if (isQuote) "Quote" else "Invoice"
        val output = File(context.cacheDir, "${prefix}_${invoice.id.replace(Regex("[^A-Za-z0-9_-]"), "_")}.pdf")
        try {
            PdfInvoiceLayout(document, invoice, seller, accent, context, isQuote).use { layout ->
                layout.draw(qr)
                output.outputStream().use { document.writeTo(it) }
            }
        } catch (error: Exception) {
            output.delete()
            throw error
        } finally {
            document.close()
            qr?.recycle()
        }
        check(output.length() > 0) { "لم يتم إنشاء ملف PDF" }
        return output
    }

    fun generateCsv(context: Context, invoice: Invoice, company: CompanyProfile): File =
        generateCsv(context, listOf(invoice), company)

    /** One row per line item; invoice totals are carried only on its first row to avoid double-counting. */
    fun generateCsv(context: Context, invoices: List<Invoice>, company: CompanyProfile): File {
        val file = File(context.cacheDir, "Invoices_${java.util.UUID.randomUUID()}.csv")
        file.outputStream().bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write("\uFEFF")
            writer.write(listOf("رقم الفاتورة", "تاريخ الإصدار", "تاريخ الاستحقاق", "الحالة", "اسم العميل", "المنشأة", "الهاتف", "رقم العميل الضريبي", "المشروع", "البند", "الكمية", "الوحدة", "السعر", "الخصم", "إجمالي البند", "المجموع قبل الضريبة", "معدل الضريبة", "الضريبة", "الإجمالي", "البنك", "الآيبان").joinToString(",") { csvText(it) })
            writer.write("\r\n")
            invoices.forEach { invoice ->
                val seller = invoice.seller ?: company
                invoice.items.forEachIndexed { index, item ->
                    val text = listOf(invoice.invoiceNumber, invoice.issueDate, invoice.dueDate, invoice.status.labelAr,
                        invoice.clientName, invoice.clientCompany, invoice.clientPhone, invoice.clientVatNumber,
                        invoice.projectCategory, item.description)
                    val row = text.map { csvText(it) } + listOf(
                        item.quantity.toString(), csvText(item.unit), decimal(item.unitPrice), decimal(item.discount),
                        decimal(item.totalWithoutVat),
                        if (index == 0) decimal(invoice.subtotal) else "",
                        decimal(invoice.taxRate),
                        if (index == 0) decimal(invoice.vatAmount) else "",
                        if (index == 0) decimal(invoice.grandTotal) else "",
                        csvText(seller.bankName), csvText(seller.iban)
                    )
                    writer.write(row.joinToString(","))
                    writer.write("\r\n")
                }
            }
        }
        return file
    }

    private fun decimal(number: Double) = java.math.BigDecimal.valueOf(number).toPlainString()

    /** Excel also executes formulas in quoted CSV cells, so escape leading formula markers. */
    private fun csvText(value: String): String {
        val safe = if (value.trimStart().firstOrNull() in listOf('=', '+', '-', '@') ||
            value.firstOrNull() in listOf('\t', '\r', '\n')) "'" + value else value
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }

    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        check(file.isFile && file.length() > 0) { "ملف التصدير غير متاح" }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun createReminderMessage(invoice: Invoice, company: CompanyProfile, reminderType: ReminderType): String {
        val seller = invoice.seller ?: company
        val title = when (reminderType) {
            ReminderType.THREE_DAYS_BEFORE -> "تذكير باقتراب الاستحقاق"
            ReminderType.ON_DUE_DATE -> "تذكير باستحقاق اليوم"
            ReminderType.OVERDUE -> "تذكير بتأخر السداد"
        }
        return """
$title
عزيزنا العميل: ${invoice.clientName}
الفاتورة: ${invoice.invoiceNumber}
تاريخ الاستحقاق: ${invoice.dueDate}
المبلغ: ${money(invoice.grandTotal)} ${seller.currencyAr} (الضريبة ${percentage(invoice.taxRate)})
البنك: ${invoice.bankName}
الآيبان: ${invoice.iban}
${seller.companyName}
${seller.phone}
""".trimIndent()
    }
}

private class PdfInvoiceLayout(
    private val document: PdfDocument,
    private val invoice: Invoice,
    private val company: CompanyProfile,
    private val accent: Int,
    private val context: Context,
    private val isQuote: Boolean = false
) : java.io.Closeable {
    // At 60 PDF points, 256 pixels gives about 300 dpi without embedding the source photograph.
    private val logo = com.example.branding.CompanyLogoStore.load(context, company.logoFileName)
    override fun close() { logo.recycle() }

    private var page: PdfDocument.Page? = null
    private lateinit var canvas: Canvas
    private var pageNumber = 0
    private var y = 0f
    private var contentTop = 0f
    private val margin = 36f
    private val bodyWidth = 523f
    private val bottom = 774f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun layout(text: String, width: Float, size: Float = 10f, bold: Boolean = false): StaticLayout {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = size
            typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
        return StaticLayout.Builder.obtain(text, 0, text.length, textPaint, width.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
            .setIncludePad(false).setLineSpacing(3f, 1f).build()
    }

    private fun drawLayout(block: StaticLayout, x: Float, top: Float) {
        canvas.save()
        canvas.translate(x, top)
        block.draw(canvas)
        canvas.restore()
    }

    private fun finishPage() {
        val active = page ?: return
        drawLayout(layout("صفحة $pageNumber — ${invoice.invoiceNumber}", bodyWidth, 8f), margin, 803f)
        document.finishPage(active)
        page = null
    }

    private fun newPage() {
        finishPage()
        pageNumber++
        page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
        canvas = page!!.canvas
        canvas.drawColor(Color.WHITE)
        paint.color = accent
        canvas.drawRect(0f, 0f, 595f, 6f, paint)
        val heading = layout(company.companyName.lineSequence().filter { it.isNotBlank() }.joinToString(" "), bodyWidth - 75f, 14f, true)
        drawLayout(heading, margin, 24f)
        val title = layout(when {
            isQuote -> "عرض سعر — ${invoice.invoiceNumber}"
            invoice.status == InvoiceStatus.DRAFT -> "مسودة — ${invoice.invoiceNumber}"
            else -> "فاتورة — ${invoice.invoiceNumber}"
        }, bodyWidth - 75f, 11f, true)
        drawLayout(title, margin, 30f + heading.height)
        run {
            val scale = minOf(60f / logo.width, 60f / logo.height)
            canvas.drawBitmap(logo, null, RectF(499f, 22f, 499f + logo.width * scale, 22f + logo.height * scale), paint)
        }
        y = maxOf(96f, 36f + heading.height + title.height)
        contentTop = y
        check(y < bottom - 40f) { "اسم المنشأة أطول من المساحة المتاحة" }
        paint.color = Color.LTGRAY
        canvas.drawLine(margin, y - 8, margin + bodyWidth, y - 8, paint)
    }

    private fun ensureSpace(height: Float) {
        if (y + height > bottom) newPage()
    }

    /** Paginate by layout line boundaries, preserving Arabic shaping and every character. */
    private fun block(text: String, size: Float = 10f, bold: Boolean = false) {
        if (text.isBlank()) return
        val block = layout(text, bodyWidth, size, bold)
        var firstLine = 0
        while (firstLine < block.lineCount) {
            val lineHeight = block.getLineBottom(firstLine) - block.getLineTop(firstLine)
            ensureSpace(lineHeight.toFloat())
            var lastLine = firstLine
            while (lastLine + 1 < block.lineCount &&
                block.getLineBottom(lastLine + 1) - block.getLineTop(firstLine) <= bottom - y) lastLine++
            val height = block.getLineBottom(lastLine) - block.getLineTop(firstLine)
            canvas.save()
            canvas.clipRect(margin, y, margin + bodyWidth, y + height)
            drawLayout(block, margin, y - block.getLineTop(firstLine))
            canvas.restore()
            y += height
            firstLine = lastLine + 1
        }
        y += 8f
    }


    private val columnWidths = listOf(100f, 80f, 80f, 60f, 177f, 26f)
    private fun tableHeader(): Float {
        val labels = listOf("الإجمالي", "الخصم", "سعر الوحدة", "الكمية / الوحدة", "الوصف", "م")
        val blocks = labels.mapIndexed { index, label -> layout(label, columnWidths[index] - 12f, 9f, true) }
        val height = blocks.maxOf { it.height }.toFloat() + 14f
        paint.color = Color.rgb(235, 240, 244)
        canvas.drawRect(margin, y, margin + bodyWidth, y + height, paint)
        var x = margin
        blocks.forEachIndexed { index, block ->
            drawLayout(block, x + 6f, y + 7f)
            x += columnWidths[index]
        }
        y += height
        return height
    }

    /** Wrap every cell and repeat the header. Even a row taller than a page is split by whole lines. */
    private fun itemTable() {
        ensureSpace(90f)
        var headerHeight = tableHeader()
        invoice.items.forEachIndexed { index, item ->
            val quantity = java.math.BigDecimal.valueOf(item.quantity).stripTrailingZeros().toPlainString()
            val values = listOf(format(item.totalWithoutVat), format(item.discount), format(item.unitPrice),
                quantity + "\n" + item.unit, item.description, (index + 1).toString())
            val blocks = values.mapIndexed { column, value ->
                layout(value, columnWidths[column] - 12f, if (column == 4) 10f else 9f)
            }
            val rowHeight = blocks.maxOf { it.height }.toFloat() + 12f
            if (rowHeight <= bottom - contentTop - headerHeight && y + rowHeight > bottom) {
                newPage()
                headerHeight = tableHeader()
            }
            val lines = IntArray(blocks.size)
            while (blocks.indices.any { lines[it] < blocks[it].lineCount }) {
                val minimum = blocks.indices.filter { lines[it] < blocks[it].lineCount }
                    .maxOf { blocks[it].getLineBottom(lines[it]) - blocks[it].getLineTop(lines[it]) }.toFloat() + 12f
                if (y + minimum > bottom) {
                    newPage()
                    headerHeight = tableHeader()
                }
                val available = bottom - y - 12f
                val ends = IntArray(blocks.size) { lines[it] - 1 }
                var drawnHeight = 0f
                blocks.forEachIndexed { column, block ->
                    val first = lines[column]
                    if (first < block.lineCount) {
                        var last = first
                        while (last + 1 < block.lineCount &&
                            block.getLineBottom(last + 1) - block.getLineTop(first) <= available) last++
                        ends[column] = last
                        drawnHeight = maxOf(drawnHeight, (block.getLineBottom(last) - block.getLineTop(first)).toFloat())
                    }
                }
                paint.color = if (index % 2 == 0) Color.WHITE else Color.rgb(248, 250, 252)
                canvas.drawRect(margin, y, margin + bodyWidth, y + drawnHeight + 12f, paint)
                var x = margin
                blocks.forEachIndexed { column, block ->
                    if (lines[column] < block.lineCount) {
                        canvas.save()
                        canvas.clipRect(x + 6f, y + 6f, x + columnWidths[column] - 6f, y + 6f + drawnHeight)
                        drawLayout(block, x + 6f, y + 6f - block.getLineTop(lines[column]))
                        canvas.restore()
                        lines[column] = ends[column] + 1
                    }
                    x += columnWidths[column]
                }
                y += drawnHeight + 12f
                paint.color = Color.rgb(223, 230, 237)
                canvas.drawLine(margin, y, margin + bodyWidth, y, paint)
            }
        }
        y += 16f
    }

    private fun totalsSection() {
        ensureSpace(110f)
        fun row(label: String, amount: Double, emphasized: Boolean = false) {
            val height = if (emphasized) 40f else 28f
            if (emphasized) {
                paint.color = Color.rgb(237, 243, 246)
                canvas.drawRoundRect(RectF(margin, y, margin + bodyWidth, y + height), 6f, 6f, paint)
            }
            val size = if (emphasized) 13f else 10f
            drawLayout(layout(label, 220f, size, emphasized), margin + bodyWidth - 232f, y + 7f)
            drawLayout(layout(format(amount) + " " + company.currencyAr, bodyWidth - 256f, size, emphasized),
                margin + 12f, y + 7f)
            y += height
        }
        row("المجموع قبل الضريبة", invoice.subtotal)
        val rate = java.math.BigDecimal.valueOf(invoice.taxRate).multiply(java.math.BigDecimal(100)).stripTrailingZeros().toPlainString()
        row("ضريبة القيمة المضافة ($rate%)", invoice.vatAmount)
        row("الإجمالي", invoice.grandTotal, true)
        y += 14f
    }

    fun draw(qr: Bitmap?) {
        newPage()
        block("بيانات المنشأة", bold = true)
        if (company.vatNumber.isNotBlank()) block("الرقم الضريبي: ${company.vatNumber}")
        if (company.commercialRegistration.isNotBlank()) block("السجل التجاري: ${company.commercialRegistration}")
        block("العنوان: ${company.address} | الهاتف: ${company.phone}")
        block("العميل: ${invoice.clientName} | ${invoice.clientCompany}", bold = true)
        block("رقم العميل الضريبي: ${invoice.clientVatNumber.ifBlank { "غير محدد" }}")
        block("العنوان: ${invoice.clientAddress} | الهاتف: ${invoice.clientPhone}")
        block(if (isQuote) "تاريخ العرض: ${invoice.issueDate} | صالح حتى: ${invoice.dueDate}" else "تاريخ الفاتورة: ${invoice.issueDate} | الاستحقاق: ${invoice.dueDate}")
        if (invoice.issuedAt.isNotBlank()) block((if (isQuote) "وقت إنشاء العرض (UTC): " else "وقت الإصدار الفعلي (UTC): ") + invoice.issuedAt)
        block(if (isQuote) "المشروع: ${invoice.projectCategory} | عرض أسعار" else "المشروع: ${invoice.projectCategory} | الحالة: ${invoice.status.labelAr}")
        block("البنود — القيم بالـ${company.currencyAr}", 12f, true)
        itemTable()
        totalsSection()
        block("البنك: ${invoice.bankName} | الآيبان: ${invoice.iban}")
        block("شروط السداد: ${invoice.paymentTerms}")
        block("الملاحظات: ${invoice.notes}")
        if (qr != null) {
            ensureSpace(160f)
            canvas.drawBitmap(qr, null, RectF(margin + bodyWidth - 135, y, margin + bodyWidth, y + 135), paint)
            drawLayout(layout("QR بيانات الفاتورة", bodyWidth - 150, 10f), margin, y + 30)
            y += 150
        }
        if (isQuote) block("عرض سعر للتقييم والموافقة، ويُحوّل إلى فاتورة عند الاعتماد.", 9f)
        // Technical data encoding is not a clearance/certification claim.
        finishPage()
    }

    private fun format(value: Double) = String.format(Locale.US, "%.2f", value)
}

enum class ReminderType(val titleAr: String, val subtitleAr: String) {
    THREE_DAYS_BEFORE("قبل الاستحقاق", "تذكير يدوي خلال الأيام الثلاثة السابقة للاستحقاق"),
    ON_DUE_DATE("يوم الاستحقاق", "تذكير يدوي في يوم الاستحقاق"),
    OVERDUE("متأخرة الدفع", "متابعة يدوية للمستحقات المتأخرة")
}
