package com.example.model

import java.math.BigDecimal
import java.math.RoundingMode

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
    val vat = BigDecimal.valueOf(subtotal).multiply(BigDecimal.valueOf(taxRate))
        .setScale(2, RoundingMode.HALF_UP).toDouble()
    return InvoiceTotals(subtotal, vat, moneySum(listOf(subtotal, vat)))
}
