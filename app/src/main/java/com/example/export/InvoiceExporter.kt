package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.R
import com.example.model.CompanyProfile
import com.example.model.Invoice
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.NumberFormat
import java.util.Locale

object InvoiceExporter {

    private val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    /**
     * Generates a luxury, high-definition A4 PDF invoice.
     */
    fun generatePdf(
        context: Context,
        invoice: Invoice,
        company: CompanyProfile,
        accentColorHex: Int = Color.parseColor("#D4AF37")
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 points
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Background: clean executive parchment/white with luxury slate borders
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        val darkNavy = Color.parseColor("#0B1320")
        val slateGray = Color.parseColor("#475569")
        val lightBorder = Color.parseColor("#E2E8F0")
        val tableHeaderBg = Color.parseColor("#0F172A")
        val zebraBg = Color.parseColor("#F8FAFC")

        // Top decorative accent bar
        paint.color = accentColorHex
        canvas.drawRect(0f, 0f, 595f, 8f, paint)

        // Draw Company Logo if available
        try {
            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
            val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.fawraty_logo, options)
            if (logoBitmap != null) {
                val destRect = RectF(35f, 25f, 95f, 85f)
                canvas.drawBitmap(logoBitmap, null, destRect, paint)
            }
        } catch (_: Exception) {}

        // Company Details (Left header area)
        textPaint.color = darkNavy
        textPaint.textSize = 15f
        textPaint.isFakeBoldText = true
        canvas.drawText(company.companyName, 105f, 42f, textPaint)

        textPaint.textSize = 9f
        textPaint.isFakeBoldText = false
        textPaint.color = slateGray
        canvas.drawText(company.companyNameEn, 105f, 55f, textPaint)
        canvas.drawText("س.ت: ${company.commercialRegistration} | الرقم الضريبي: ${company.vatNumber}", 105f, 68f, textPaint)
        canvas.drawText("${company.address} | هاتف: ${company.phone}", 105f, 80f, textPaint)

        // Tax Invoice Title Box (Right header area)
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(RectF(395f, 25f, 560f, 85f), 6f, 6f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = accentColorHex
        canvas.drawRoundRect(RectF(395f, 25f, 560f, 85f), 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        textPaint.color = darkNavy
        textPaint.textSize = 13f
        textPaint.isFakeBoldText = true
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("فاتورة ضريبية معتمدة", 477f, 45f, textPaint)

        textPaint.textSize = 8.5f
        textPaint.color = slateGray
        textPaint.isFakeBoldText = false
        canvas.drawText("فاتورة ضريبية - معتمدة ومطابقة", 477f, 58f, textPaint)

        textPaint.color = darkNavy
        textPaint.isFakeBoldText = true
        canvas.drawText(invoice.invoiceNumber, 477f, 74f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT

        // Divider
        paint.color = lightBorder
        paint.strokeWidth = 1f
        canvas.drawLine(35f, 98f, 560f, 98f, paint)

        // Invoice Meta & Client Cards (Two Columns)
        // Left Box: Buyer / Client Info
        val cardWidth = 240f
        paint.color = Color.parseColor("#FAFAFA")
        canvas.drawRoundRect(RectF(35f, 108f, 35f + cardWidth, 195f), 6f, 6f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = lightBorder
        canvas.drawRoundRect(RectF(35f, 108f, 35f + cardWidth, 195f), 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        textPaint.color = accentColorHex
        textPaint.textSize = 10f
        textPaint.isFakeBoldText = true
        canvas.drawText("بيانات العميل (المشتري):", 45f, 124f, textPaint)

        textPaint.color = darkNavy
        textPaint.textSize = 11f
        textPaint.isFakeBoldText = true
        canvas.drawText(invoice.clientCompany.ifEmpty { invoice.clientName }, 45f, 140f, textPaint)

        textPaint.textSize = 9f
        textPaint.isFakeBoldText = false
        textPaint.color = slateGray
        canvas.drawText("المسؤول: ${invoice.clientName}", 45f, 153f, textPaint)
        canvas.drawText("الرقم الضريبي: ${invoice.clientVatNumber}", 45f, 166f, textPaint)
        canvas.drawText("العنوان: ${invoice.clientAddress} | ${invoice.clientPhone}", 45f, 179f, textPaint)

        // Right Box: Dates & Project Type
        val rightX = 320f
        paint.color = Color.parseColor("#FAFAFA")
        canvas.drawRoundRect(RectF(rightX, 108f, 560f, 195f), 6f, 6f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = lightBorder
        canvas.drawRoundRect(RectF(rightX, 108f, 560f, 195f), 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        textPaint.color = accentColorHex
        textPaint.textSize = 10f
        textPaint.isFakeBoldText = true
        canvas.drawText("تفاصيل الفاتورة والمشروع:", rightX + 10f, 124f, textPaint)

        textPaint.color = darkNavy
        textPaint.textSize = 9.5f
        textPaint.isFakeBoldText = false
        canvas.drawText("تاريخ الإصدار: ${invoice.issueDate}", rightX + 10f, 142f, textPaint)
        canvas.drawText("تاريخ الاستحقاق: ${invoice.dueDate}", rightX + 10f, 156f, textPaint)
        canvas.drawText("تصنيف المشروع: ${invoice.projectCategory}", rightX + 10f, 170f, textPaint)
        canvas.drawText("حالة السداد: ${invoice.status.labelAr} (${invoice.status.labelEn})", rightX + 10f, 184f, textPaint)

        // Table Header
        val tableTop = 210f
        paint.color = tableHeaderBg
        canvas.drawRoundRect(RectF(35f, tableTop, 560f, tableTop + 24f), 4f, 4f, paint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 9.5f
        textPaint.isFakeBoldText = true
        canvas.drawText("#", 45f, tableTop + 16f, textPaint)
        canvas.drawText("بيان الصنف / الخدمة المعمارية", 70f, tableTop + 16f, textPaint)
        canvas.drawText("الكمية", 320f, tableTop + 16f, textPaint)
        canvas.drawText("الوحدة", 365f, tableTop + 16f, textPaint)
        canvas.drawText("سعر الوحدة", 420f, tableTop + 16f, textPaint)
        canvas.drawText("الخصم", 485f, tableTop + 16f, textPaint)
        canvas.drawText("الإجمالي", 525f, tableTop + 16f, textPaint)

        // Table Rows
        var currentY = tableTop + 24f
        textPaint.isFakeBoldText = false
        textPaint.textSize = 9f

        invoice.items.forEachIndexed { index, item ->
            val rowHeight = 24f
            if (index % 2 == 1) {
                paint.color = zebraBg
                canvas.drawRect(35f, currentY, 560f, currentY + rowHeight, paint)
            }
            paint.color = lightBorder
            paint.strokeWidth = 0.5f
            canvas.drawLine(35f, currentY + rowHeight, 560f, currentY + rowHeight, paint)

            textPaint.color = darkNavy
            canvas.drawText("${index + 1}", 45f, currentY + 16f, textPaint)

            val shortDesc = if (item.description.length > 44) item.description.take(42) + ".." else item.description
            canvas.drawText(shortDesc, 70f, currentY + 16f, textPaint)

            canvas.drawText("${item.quantity}", 320f, currentY + 16f, textPaint)
            canvas.drawText(item.unit, 365f, currentY + 16f, textPaint)
            canvas.drawText(numberFormat.format(item.unitPrice), 420f, currentY + 16f, textPaint)
            canvas.drawText(numberFormat.format(item.discount), 485f, currentY + 16f, textPaint)

            textPaint.isFakeBoldText = true
            canvas.drawText(numberFormat.format(item.totalWithoutVat), 525f, currentY + 16f, textPaint)
            textPaint.isFakeBoldText = false

            currentY += rowHeight
        }

        // Totals & Bank Summary Section
        val totalsTop = currentY + 16f

        // Left box: Bank info & Notes
        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(RectF(35f, totalsTop, 320f, totalsTop + 115f), 6f, 6f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = lightBorder
        canvas.drawRoundRect(RectF(35f, totalsTop, 320f, totalsTop + 115f), 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        textPaint.color = accentColorHex
        textPaint.isFakeBoldText = true
        textPaint.textSize = 9.5f
        canvas.drawText("معلومات السداد والحساب البنكي:", 45f, totalsTop + 18f, textPaint)

        textPaint.color = darkNavy
        textPaint.isFakeBoldText = false
        textPaint.textSize = 8.5f
        canvas.drawText("البنك: ${company.bankName}", 45f, totalsTop + 33f, textPaint)
        canvas.drawText("رقم الآيبان IBAN: ${company.iban}", 45f, totalsTop + 47f, textPaint)
        canvas.drawText("الشروط: ${invoice.paymentTerms.take(55)}", 45f, totalsTop + 62f, textPaint)
        canvas.drawText("الملاحظات: ${invoice.notes.take(55)}", 45f, totalsTop + 76f, textPaint)
        textPaint.color = slateGray
        canvas.drawText("خاضعة لكود البناء السعودي واشتراطات هيئة الزكاة والضريبة والجمارك.", 45f, totalsTop + 95f, textPaint)

        // Right box: Calculation breakdown (Subtotal, VAT 15%, Grand Total)
        val calcLeft = 340f
        paint.color = Color.parseColor("#0F172A")
        canvas.drawRoundRect(RectF(calcLeft, totalsTop, 560f, totalsTop + 115f), 6f, 6f, paint)

        textPaint.color = Color.parseColor("#94A3B8")
        textPaint.textSize = 9.5f
        canvas.drawText("المجموع قبل الضريبة:", calcLeft + 12f, totalsTop + 22f, textPaint)
        textPaint.color = Color.WHITE
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("${numberFormat.format(invoice.subtotal)} ${company.currencyAr}", 545f, totalsTop + 22f, textPaint)

        textPaint.color = Color.parseColor("#94A3B8")
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("ضريبة القيمة المضافة (15%):", calcLeft + 12f, totalsTop + 45f, textPaint)
        textPaint.color = Color.WHITE
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("${numberFormat.format(invoice.vatAmount)} ${company.currencyAr}", 545f, totalsTop + 45f, textPaint)

        paint.color = accentColorHex
        canvas.drawLine(calcLeft + 12f, totalsTop + 60f, 548f, totalsTop + 60f, paint)

        textPaint.color = accentColorHex
        textPaint.isFakeBoldText = true
        textPaint.textSize = 11.5f
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("المبلغ الإجمالي المستحق:", calcLeft + 12f, totalsTop + 85f, textPaint)

        textPaint.textSize = 13f
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("${numberFormat.format(invoice.grandTotal)} ${company.currencyAr}", 545f, totalsTop + 85f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT

        // Signature & Stamp Section at Bottom
        val bottomY = 740f
        paint.color = lightBorder
        canvas.drawLine(35f, bottomY, 560f, bottomY, paint)

        textPaint.color = slateGray
        textPaint.textSize = 8.5f
        canvas.drawText("الختم المعتمد لشركة زجاج الرياض", 65f, bottomY + 25f, textPaint)
        canvas.drawText("التوقيع المعتمد للإدارة المالية", 400f, bottomY + 25f, textPaint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawRoundRect(RectF(60f, bottomY + 32f, 180f, bottomY + 75f), 4f, 4f, paint)
        canvas.drawRoundRect(RectF(380f, bottomY + 32f, 510f, bottomY + 75f), 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        // Stamp text inside box
        textPaint.color = accentColorHex
        textPaint.textSize = 8f
        textPaint.isFakeBoldText = true
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("شركة زجاج الرياض", 120f, bottomY + 52f, textPaint)
        canvas.drawText("معتمد ومطابق للأنظمة", 120f, bottomY + 64f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "Invoice_${invoice.invoiceNumber}.pdf")
        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        outputStream.close()
        pdfDocument.close()

        return file
    }

    /**
     * Generates a clean, UTF-8 encoded CSV file with BOM for Microsoft Excel / Accounting.
     */
    fun generateCsv(context: Context, invoice: Invoice, company: CompanyProfile): File {
        val file = File(context.cacheDir, "Invoice_${invoice.invoiceNumber}.csv")
        val writer = OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)

        // UTF-8 BOM for Excel Arabic character compatibility
        writer.write("\uFEFF")

        // CSV Header
        writer.write("رقم الفاتورة,تاريخ الإصدار,تاريخ الاستحقاق,حالة الفاتورة,اسم العميل,اسم المنشأة,هاتف العميل,الرقم الضريبي للعميل,تصنيف المشروع,بيان الصنف,الكمية,الوحدة,سعر الوحدة,الخصم,إجمالي الصنف,المجموع قبل الضريبة,الضريبة 15%,المبلغ الإجمالي,البنك,الآيبان\n")

        // For each item write a row
        invoice.items.forEach { item ->
            val row = buildString {
                append("\"${invoice.invoiceNumber}\",")
                append("\"${invoice.issueDate}\",")
                append("\"${invoice.dueDate}\",")
                append("\"${invoice.status.labelAr}\",")
                append("\"${invoice.clientName.replace("\"", "\"\"")}\",")
                append("\"${invoice.clientCompany.replace("\"", "\"\"")}\",")
                append("\"${invoice.clientPhone}\",")
                append("\"${invoice.clientVatNumber}\",")
                append("\"${invoice.projectCategory}\",")
                append("\"${item.description.replace("\"", "\"\"")}\",")
                append("${item.quantity},")
                append("\"${item.unit}\",")
                append("${item.unitPrice},")
                append("${item.discount},")
                append("${item.totalWithoutVat},")
                append("${invoice.subtotal},")
                append("${invoice.vatAmount},")
                append("${invoice.grandTotal},")
                append("\"${company.bankName}\",")
                append("\"${company.iban}\"\n")
            }
            writer.write(row)
        }

        writer.flush()
        writer.close()
        return file
    }

    /**
     * Shares a file via Android Intent with FileProvider.
     */
    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * Creates a formal Arabic WhatsApp / SMS reminder template.
     */
    fun createReminderMessage(
        invoice: Invoice,
        company: CompanyProfile,
        reminderType: ReminderType
    ): String {
        val typeGreeting = when (reminderType) {
            ReminderType.THREE_DAYS_BEFORE -> "تذكير بموعد الاستحقاق القادم (خلال 3 أيام)"
            ReminderType.ON_DUE_DATE -> "تذكير بموعد الاستحقاق اليوم"
            ReminderType.OVERDUE -> "إشعار تأخر سداد الفاتورة المستحقة"
        }

        return """
السلام عليكم ورحمة الله وبركاته،
عزيزنا العميل: ${invoice.clientName} (${invoice.clientCompany})

نحيطكم علماً بأنه قد تم إصدار $typeGreeting للفاتورة الضريبية رقم:
*${invoice.invoiceNumber}*

📌 تفاصيل الفاتورة:
• المشروع: ${invoice.projectCategory}
• تاريخ الإصدار: ${invoice.issueDate}
• تاريخ الاستحقاق: ${invoice.dueDate}
• المبلغ الإجمالي المستحق: ${numberFormat.format(invoice.grandTotal)} ${company.currencyAr} (شامل ضريبة القيمة المضافة 15%)

🏦 بيانات التحويل البنكي:
• البنك: ${company.bankName}
• الحساب باسم: ${company.companyName}
• الآيبان (IBAN): ${company.iban}

شاكرين لكم حسن تعاونكم الدائم وثقتكم بشركة زجاج الرياض.
للتواصل أو الاستفسار: ${company.phone} | ${company.email}
""".trimIndent()
    }
}

enum class ReminderType(val titleAr: String, val subtitleAr: String) {
    THREE_DAYS_BEFORE("تذكير قبل 3 أيام", "إشعار استباقي قبل موعد الاستحقاق بـ 3 أيام"),
    ON_DUE_DATE("تذكير يوم الاستحقاق", "إشعار في نفس يوم الاستحقاق المالي"),
    OVERDUE("تنبيه متأخرة الدفع", "متابعة فورية للمستحقات المتأخرة")
}
