package com.example.model

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.math.BigDecimal
import java.math.RoundingMode

enum class InvoiceStatus(val labelAr: String, val labelEn: String) {
    PAID("مدفوعة", "Paid"),
    PENDING("بانتظار التحصيل", "Pending"),
    OVERDUE("متأخرة", "Overdue"),
    DRAFT("مسودة", "Draft")
}

data class InvoiceItem(
    val id: String,
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val unit: String = "متر مربع",
    val discount: Double = 0.0
) {
    init {
        require(quantity.isFinite() && quantity > 0) { "الكمية يجب أن تكون رقمًا موجبًا" }
        require(unitPrice.isFinite() && unitPrice >= 0) { "السعر يجب أن يكون رقمًا غير سالب" }
        require(discount.isFinite() && discount >= 0) { "الخصم يجب أن يكون رقمًا غير سالب" }
        require(BigDecimal.valueOf(discount) <= BigDecimal.valueOf(quantity).multiply(BigDecimal.valueOf(unitPrice))) {
            "الخصم أكبر من قيمة البند"
        }
    }
    val totalWithoutVat: Double
        get() = BigDecimal.valueOf(quantity).multiply(BigDecimal.valueOf(unitPrice))
            .subtract(BigDecimal.valueOf(discount)).setScale(2, RoundingMode.HALF_UP).toDouble()
}

data class Invoice(
    val id: String,
    val invoiceNumber: String,
    val clientName: String,
    val clientCompany: String,
    val clientPhone: String,
    val clientEmail: String,
    val clientVatNumber: String,
    val clientAddress: String,
    val issueDate: String,
    val dueDate: String,
    val projectCategory: String,
    val items: List<InvoiceItem>,
    val taxRate: Double = 0.15,
    val status: InvoiceStatus = InvoiceStatus.PENDING,
    val notes: String = "الضمان 10 سنوات على أعمال الزجاج المعماري والعزل الحراري.",
    val paymentTerms: String = "الدفعة الأولى 50% عند التعاقد، 40% عند التوريد، 10% عند التسليم النهائي.",
    val bankName: String = "مصرف الراجحي",
    val iban: String = "",
    val issuedAt: String = "",
    val seller: CompanyProfile? = null,
    val clientId: String? = null
) {
    init {
        require(clientName.isNotBlank()) { "يرجى إدخال اسم العميل" }
        require(items.isNotEmpty() && items.all { it.description.isNotBlank() }) { "يرجى إدخال وصف لكل بند" }
        require(taxRate.isFinite() && taxRate in 0.0..1.0) { "نسبة الضريبة غير صالحة" }
        val issue = InvoiceDates.parse(issueDate)
        val due = InvoiceDates.parse(dueDate)
        require(issue != null && due != null && !due.before(issue)) { "راجع تاريخ الفاتورة والاستحقاق بصيغة yyyy-MM-dd" }
        require(clientVatNumber.isBlank() || Regex("3[0-9]{13}3").matches(clientVatNumber)) { "الرقم الضريبي للعميل غير صالح" }
    }
    val subtotal: Double get() = calculateInvoiceTotals(items, taxRate).subtotal
    val vatAmount: Double get() = calculateInvoiceTotals(items, taxRate).vat
    val grandTotal: Double get() = calculateInvoiceTotals(items, taxRate).grandTotal

    val totalItemsCount: Int
        get() = items.size

    /** Five-field TLV payload. This is not ZATCA Phase 2 signing or clearance. */
    fun generateZatcaTlvBase64(sellerName: String, sellerVat: String): String {
        require(sellerName.isNotBlank() && Regex("3[0-9]{13}3").matches(sellerVat)) {
            "أكمل اسم المنشأة ورقمها الضريبي الصحيح قبل إنشاء QR"
        }
        require(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z").matches(issuedAt)) {
            "وقت الإصدار الفعلي غير متاح لهذه الفاتورة"
        }
        val bos = ByteArrayOutputStream()
        fun writeTlv(tag: Int, value: String) {
            val bytes = value.toByteArray(StandardCharsets.UTF_8)
            require(bytes.size <= 255) { "بيانات QR أطول من الحد المدعوم" }
            bos.write(tag)
            bos.write(bytes.size)
            bos.write(bytes)
        }
        writeTlv(1, sellerName)
        writeTlv(2, sellerVat)
        writeTlv(3, issuedAt)
        writeTlv(4, String.format(java.util.Locale.US, "%.2f", grandTotal))
        writeTlv(5, String.format(java.util.Locale.US, "%.2f", vatAmount))
        return android.util.Base64.encodeToString(bos.toByteArray(), android.util.Base64.NO_WRAP)
    }
}

data class Client(
    val id: String,
    val name: String,
    val company: String,
    val phone: String,
    val email: String,
    val vatNumber: String,
    val address: String,
    val totalBilled: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val invoicesCount: Int = 0
)

data class CatalogProduct(
    val id: String,
    val name: String,
    val nameEn: String,
    val category: String,
    val description: String,
    val defaultPrice: Double,
    val unit: String,
    val specifications: String,
    val warranty: String
)

data class CompanyProfile(
    val companyName: String = "شركة زجاج الرياض للصناعات المعمارية",
    val companyNameEn: String = "Riyadh Glass Architectural Industries Co.",
    val commercialRegistration: String = "",
    val vatNumber: String = "",
    val address: String = "طريق الملك فهد، حي الصحافة، الرياض، المملكة العربية السعودية",
    val phone: String = "",
    val email: String = "",
    val website: String = "www.riyadhglass.com.sa",
    val bankName: String = "مصرف الراجحي | Al Rajhi Bank",
    val iban: String = "",
    val defaultTaxRate: Double = 0.15,
    val currencyAr: String = "ر.س",
    val currencyEn: String = "SAR"
)
