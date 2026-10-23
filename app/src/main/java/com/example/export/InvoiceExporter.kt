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
        accentColorHex: Int = Color.rgb(160, 124, 35)): File {
        val seller = invoice.seller ?: company
        val qr = if (invoice.status == InvoiceStatus.DRAFT) null
            else InvoiceQr.bitmap(invoice.generateZatcaTlvBase64(seller.companyName, seller.vatNumber))
        val document = PdfDocument()
        val output = File(context.cacheDir, "Invoice_${invoice.id.replace(Regex("[^A-Za-z0-9_-]"), "_")}.pdf")
        try {
            PdfInvoiceLayout(document, invoice, seller, accentColorHex, context).use { layout ->
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
    private val context: Context
) : java.io.Closeable {
    // At 60 PDF points, 256 pixels gives about 300 dpi without embedding the source photograph.
    private val logo: Bitmap? = loadLogo(context)
    override fun close() { logo?.recycle() }

    private fun loadLogo(context: Context): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true; inScaled = false }
        BitmapFactory.decodeResource(context.resources, R.drawable.fawraty_logo, options)
        options.inSampleSize = 1
        while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 512) options.inSampleSize *= 2
        options.inJustDecodeBounds = false
        val source = BitmapFactory.decodeResource(context.resources, R.drawable.fawraty_logo, options) ?: return null
        val ratio = minOf(1f, 256f / maxOf(source.width, source.height))
        if (ratio == 1f) return source
        val scaled = Bitmap.createScaledBitmap(source, maxOf(1, (source.width * ratio).toInt()),
            maxOf(1, (source.height * ratio).toInt()), true)
        if (scaled !== source) source.recycle()
        return scaled
    }

    private var page: PdfDocument.Page? = null
    private lateinit var canvas: Canvas
    private var pageNumber = 0
    private var y = 0f
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
        val heading = layout(company.companyName, bodyWidth - 75f, 14f, true)
        drawLayout(heading, margin, 24f)
        val title = layout(if (invoice.status == InvoiceStatus.DRAFT) "مسودة — ${invoice.invoiceNumber}" else "فاتورة — ${invoice.invoiceNumber}", bodyWidth - 75f, 10f)
        drawLayout(title, margin, 30f + heading.height)
        if (logo != null) {
            val scale = minOf(60f / logo.width, 60f / logo.height)
            canvas.drawBitmap(logo, null, RectF(499f, 22f, 499f + logo.width * scale, 22f + logo.height * scale), paint)
        }
        y = maxOf(96f, 36f + heading.height + title.height)
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

    fun draw(qr: Bitmap?) {
        newPage()
        block("بيانات المنشأة", bold = true)
        block("الرقم الضريبي: ${company.vatNumber} | السجل التجاري: ${company.commercialRegistration}")
        block("العنوان: ${company.address} | الهاتف: ${company.phone}")
        block("العميل: ${invoice.clientName} | ${invoice.clientCompany}", bold = true)
        block("رقم العميل الضريبي: ${invoice.clientVatNumber.ifBlank { "غير محدد" }}")
        block("العنوان: ${invoice.clientAddress} | الهاتف: ${invoice.clientPhone}")
        block("تاريخ الفاتورة: ${invoice.issueDate} | الاستحقاق: ${invoice.dueDate}")
        if (invoice.issuedAt.isNotBlank()) block("وقت الإصدار الفعلي (UTC): ${invoice.issuedAt}")
        block("المشروع: ${invoice.projectCategory} | الحالة: ${invoice.status.labelAr}")
        block("البنود", 12f, true)
        invoice.items.forEachIndexed { index, item ->
            block("${index + 1}. ${item.description}", bold = true)
            block("الكمية: ${item.quantity} ${item.unit} | السعر: ${format(item.unitPrice)} | الخصم: ${format(item.discount)} | إجمالي البند: ${format(item.totalWithoutVat)} ${company.currencyAr}")
            ensureSpace(12f)
            paint.color = Color.LTGRAY
            canvas.drawLine(margin, y, margin + bodyWidth, y, paint)
            y += 10f
        }
        block("المجموع قبل الضريبة: ${format(invoice.subtotal)} ${company.currencyAr}", bold = true)
        block("الضريبة (${java.math.BigDecimal.valueOf(invoice.taxRate * 100).stripTrailingZeros().toPlainString()}%): ${format(invoice.vatAmount)} ${company.currencyAr}")
        block("الإجمالي: ${format(invoice.grandTotal)} ${company.currencyAr}", 13f, true)
        block("البنك: ${invoice.bankName} | الآيبان: ${invoice.iban}")
        block("شروط السداد: ${invoice.paymentTerms}")
        block("الملاحظات: ${invoice.notes}")
        if (qr != null) {
            ensureSpace(160f)
            canvas.drawBitmap(qr, null, RectF(margin + bodyWidth - 135, y, margin + bodyWidth, y + 135), paint)
            drawLayout(layout("QR بيانات الفاتورة", bodyWidth - 150, 10f), margin, y + 30)
            y += 150
        }
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
