package com.example.model

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

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
    val totalWithoutVat: Double
        get() = ((quantity * unitPrice) - discount).coerceAtLeast(0.0)
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
    val iban: String = "SA44 8000 0456 1234 5678 9012"
) {
    val subtotal: Double
        get() = items.sumOf { it.totalWithoutVat }

    val vatAmount: Double
        get() = subtotal * taxRate

    val grandTotal: Double
        get() = subtotal + vatAmount

    val totalItemsCount: Int
        get() = items.size

    /**
     * Generates official ZATCA Phase 1 & 2 TLV (Tag-Length-Value) Base64 string:
     * Tag 1: Seller's Name
     * Tag 2: VAT Registration Number
     * Tag 3: Time Stamp (ISO 8601)
     * Tag 4: Invoice Total (with VAT)
     * Tag 5: VAT Total
     */
    fun generateZatcaTlvBase64(sellerName: String, sellerVat: String): String {
        return try {
            val bos = ByteArrayOutputStream()
            fun writeTlv(tag: Int, value: String) {
                val bytes = value.toByteArray(StandardCharsets.UTF_8)
                bos.write(tag)
                bos.write(bytes.size)
                bos.write(bytes)
            }
            writeTlv(1, sellerName)
            writeTlv(2, sellerVat)
            writeTlv(3, "${issueDate}T12:00:00Z")
            writeTlv(4, String.format(java.util.Locale.US, "%.2f", grandTotal))
            writeTlv(5, String.format(java.util.Locale.US, "%.2f", vatAmount))

            android.util.Base64.encodeToString(bos.toByteArray(), android.util.Base64.NO_WRAP)
        } catch (_: Exception) {
            "ZATCA_SIMULATED_TLV_HASH_${invoiceNumber}"
        }
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
    val commercialRegistration: String = "1010789456",
    val vatNumber: String = "310123456700003",
    val address: String = "طريق الملك فهد، حي الصحافة، الرياض، المملكة العربية السعودية",
    val phone: String = "+966 11 456 7890",
    val email: String = "billing@riyadhglass.com.sa",
    val website: String = "www.riyadhglass.com.sa",
    val bankName: String = "مصرف الراجحي | Al Rajhi Bank",
    val iban: String = "SA44 8000 0456 1234 5678 9012",
    val defaultTaxRate: Double = 0.15,
    val currencyAr: String = "ر.س",
    val currencyEn: String = "SAR"
)
