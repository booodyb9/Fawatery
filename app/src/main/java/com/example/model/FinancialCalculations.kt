package com.example.model

import java.math.BigDecimal
import java.math.RoundingMode

val MAX_INVOICE_AMOUNT: BigDecimal = BigDecimal("999999999999.99")

fun moneySum(values: List<Double>): Double = values.fold(BigDecimal.ZERO) { total, value ->
    total.add(BigDecimal.valueOf(value))
}.setScale(2, RoundingMode.HALF_UP).toDouble()

fun Invoice.effectiveStatus(today: String = InvoiceDates.today()): InvoiceStatus =
    if (status == InvoiceStatus.PAID || status == InvoiceStatus.DRAFT) status
    else if (dueDate < today) InvoiceStatus.OVERDUE else InvoiceStatus.PENDING

data class InvoiceTotals(val subtotal: Double, val vat: Double, val grandTotal: Double)

fun calculateInvoiceTotals(items: List<InvoiceItem>, taxRate: Double): InvoiceTotals {
    require(taxRate.isFinite() && taxRate in 0.0..1.0)
    val subtotal = moneySum(items.map { it.totalWithoutVat })
    require(BigDecimal.valueOf(subtotal) <= MAX_INVOICE_AMOUNT) { "مجموع الفاتورة يتجاوز الحد المالي المدعوم" }
    val vat = BigDecimal.valueOf(subtotal).multiply(BigDecimal.valueOf(taxRate))
        .setScale(2, RoundingMode.HALF_UP).toDouble()
    val grand = moneySum(listOf(subtotal, vat))
    require(BigDecimal.valueOf(grand) <= MAX_INVOICE_AMOUNT) { "إجمالي الفاتورة يتجاوز الحد المالي المدعوم" }
    return InvoiceTotals(subtotal, vat, grand)
}


/** Display adapter shared by the UI and PDF; line totals keep their independent halala rounding. */
fun formatInvoiceRate(value: Double): String {
    require(value.isFinite()) { "قيمة غير صالحة للعرض" }
    val decimal = BigDecimal.valueOf(value).stripTrailingZeros()
    return decimal.setScale(maxOf(2, decimal.scale())).toPlainString()
}
