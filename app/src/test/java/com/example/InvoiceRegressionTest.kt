package com.example

import com.example.model.*
import com.example.repository.FawratyRepository
import com.example.reminders.ReminderManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class InvoiceRegressionTest {
    @org.junit.Before fun resetFixtureStorage() {
        val testContext = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        testContext.deleteDatabase("fawraty_records.db")
        androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
            .getSharedPreferences("fawraty_records", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        FawratyRepository().updateCompanyProfile(CompanyProfile(companyName = "منشأة اختبار", vatNumber = "300000000000003"))
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeDiscount() { InvoiceItem("i", "test", 1.0, 100.0, discount = -50.0) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonFiniteQuantity() { InvoiceItem("i", "test", Double.NaN, 100.0) }

    @Test fun roundsLineTotalsToHalalas() {
        assertEquals(0.34, InvoiceItem("i", "test", 1.0, 0.335).totalWithoutVat, 0.0)
    }

    @Test fun freshRepositoryHasNoInventedInvoices() {
        assertTrue(FawratyRepository().invoices.value.isEmpty())
    }

    @Test fun deletingAnInvoiceDoesNotReuseItsNumber() {
        val repository = FawratyRepository()
        val number = repository.generateNextInvoiceNumber()
        val invoice = sampleInvoice(number)
        repository.addInvoice(invoice)
        repository.deleteInvoice(invoice.id)
        assertNotEquals(number, repository.generateNextInvoiceNumber())
    }

    @Test fun remindersUseTheActualCalendarDate() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val invoice = sampleInvoice("TODAY").copy(dueDate = today)
        val reminders = ReminderManager.evaluateReminders(listOf(invoice))
        assertEquals(1, reminders.size)
        assertEquals(0L, reminders.single().daysUntilDue)
    }
}

internal fun sampleInvoice(number: String = "TEST-001", status: InvoiceStatus = InvoiceStatus.PENDING) = Invoice(
    id = java.util.UUID.randomUUID().toString(), invoiceNumber = number,
    clientName = "عميل تجريبي", clientCompany = "شركة اختبار", clientPhone = "",
    clientEmail = "", clientVatNumber = "", clientAddress = "عنوان اختبار",
    issueDate = "2026-10-01", dueDate = "2026-10-31", projectCategory = "اختبار",
    items = listOf(InvoiceItem("i", "خدمة تجريبية", 1.0, 100.0)), status = status
)
