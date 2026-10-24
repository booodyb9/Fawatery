package com.example.model

/** A quotation is stored separately from billed invoices and never contributes to customer debt. */
data class Quote(
    val document: Invoice,
    val convertedInvoiceId: String? = null
) {
    init { require(document.status == InvoiceStatus.DRAFT) { "عرض السعر ليس فاتورة مستحقة" } }
    val id: String get() = document.id
    val number: String get() = document.invoiceNumber
    val validUntil: String get() = document.dueDate
    fun isExpired(today: String = InvoiceDates.today()): Boolean = validUntil < today
    val labelAr: String get() = when {
        convertedInvoiceId != null -> "تم التحويل لفاتورة"
        isExpired() -> "منتهي الصلاحية"
        else -> "عرض مفتوح"
    }
}
