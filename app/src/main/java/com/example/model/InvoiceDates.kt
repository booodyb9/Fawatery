package com.example.model

import java.text.SimpleDateFormat
import java.text.ParsePosition
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object InvoiceDates {
    private fun format() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    fun parse(value: String): Date? {
        if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(value)) return null
        val position = ParsePosition(0)
        return format().parse(value, position)?.takeIf { position.index == value.length }
    }
    fun today(): String = format().format(Date())
    fun plusDays(date: String, days: Int): String {
        val calendar = Calendar.getInstance().apply { time = requireNotNull(parse(date)) }
        calendar.add(Calendar.DATE, days)
        return format().format(calendar.time)
    }
    fun utcNow(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
    fun calendarDaysBetween(from: String, to: String): Long {
        // Parse calendar dates at UTC midnight to avoid DST and time-of-day truncation.
        val utc = format().apply { timeZone = TimeZone.getTimeZone("UTC") }
        return (requireNotNull(utc.parse(to)).time - requireNotNull(utc.parse(from)).time) / 86_400_000L
    }
}

fun parseInvoiceDecimal(value: String): Double? {
    val normalized = value.trim().map { character ->
        when {
            character == '٫' -> '.'
            character.isDigit() -> ('0'.code + (character.digitToIntOrNull() ?: return null)).toChar()
            else -> character
        }
    }.joinToString("")
    return normalized.toDoubleOrNull()?.takeIf { it.isFinite() }
}
