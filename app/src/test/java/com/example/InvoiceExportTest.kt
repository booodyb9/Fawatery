package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.export.InvoiceExporter
import com.example.export.InvoiceQr
import com.example.model.*
import com.example.reminders.*
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class InvoiceExportTest {
    @Test fun qrRoundTripsTheExactTlvPayload() {
        val invoice = sampleInvoice().copy(issuedAt = "2026-10-23T08:30:00Z")
        val payload = invoice.generateZatcaTlvBase64("منشأة اختبار", "300000000000003")
        val image = InvoiceQr.bitmap(payload)
        val colors = IntArray(image.width * image.height)
        image.getPixels(colors, 0, image.width, 0, 0, image.width, image.height)
        val result = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(image.width, image.height, colors))))
        assertEquals(payload, result.text)
        val bytes = android.util.Base64.decode(result.text, android.util.Base64.DEFAULT)
        var offset = 0
        val fields = mutableMapOf<Int, String>()
        while (offset < bytes.size) {
            val tag = bytes[offset++].toInt() and 255
            val length = bytes[offset++].toInt() and 255
            fields[tag] = String(bytes, offset, length, Charsets.UTF_8)
            offset += length
        }
        assertEquals(invoice.issuedAt, fields[3])
        assertEquals("115.00", fields[4])
        assertEquals("15.00", fields[5])
    }

    @Test fun csvContainsEveryInvoiceWithoutRepeatingInvoiceTotals() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val first = sampleInvoice("FIRST").copy(items = listOf(
            InvoiceItem("1", "سطر أول", 1.0, 100.0), InvoiceItem("2", "سطر ثان", 1.0, 100.0)
        ))
        val second = sampleInvoice("SECOND")
        val content = InvoiceExporter.generateCsv(context, listOf(first, second), CompanyProfile()).readText()
        assertTrue(content.contains("FIRST"))
        assertTrue(content.contains("SECOND"))
        assertEquals(1, Regex("230.0").findAll(content).count())
    }

    @Test fun csvEscapesQuotesNewlinesAndSpreadsheetFormulas() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val invoice = sampleInvoice().copy(clientName = "=1+1", projectCategory = "مشروع \"اختبار\"")
        val content = InvoiceExporter.generateCsv(context, invoice, CompanyProfile()).readText()
        assertTrue(content.contains("\"'=1+1\""))
        assertTrue(content.contains("\"مشروع \"\"اختبار\"\"\""))
    }

    @Test fun reminderOptionsAreHonoredAndTimeOfDayDoesNotShiftTheDueDay() {
        val today = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse("2026-10-23 23:59")!!
        val invoice = sampleInvoice().copy(dueDate = "2026-10-23")
        assertEquals(0L, ReminderManager.evaluateReminders(listOf(invoice), today).single().daysUntilDue)
        assertTrue(ReminderManager.evaluateReminders(listOf(invoice), today, ReminderOptions(false, false, false)).isEmpty())
    }

    @Test fun totalsUseTheSameRoundingAsTheInvoiceAndSupportArabicDigits() {
        val lines = listOf(InvoiceItem("1", "a", 1.0, 0.335), InvoiceItem("2", "b", 1.0, 0.335))
        val totals = calculateInvoiceTotals(lines, 0.15)
        assertEquals(0.68, totals.subtotal, 0.0)
        assertEquals(0.10, totals.vat, 0.0)
        assertEquals(0.78, totals.grandTotal, 0.0)
        assertEquals(12.5, parseInvoiceDecimal("١٢٫٥")!!, 0.0)
        assertNull(parseInvoiceDecimal("NaN"))
    }
}
