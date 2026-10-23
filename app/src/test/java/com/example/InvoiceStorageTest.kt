package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.*
import com.example.repository.FawratyRepository
import com.example.reminders.ReminderOptions
import com.example.viewmodel.calculateFinancialStats
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class InvoiceStorageTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    @Before fun resetFixtureStorage() {
        context.deleteDatabase("fawraty_records.db")
        context.getSharedPreferences("fawraty_records", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun invoicesClientsAndProfileSurviveRepositoryRecreation() {
        val repository = FawratyRepository(context)
        val company = CompanyProfile(companyName = "منشأة اختبار", vatNumber = "300000000000003")
        repository.updateCompanyProfile(company)
        val saved = repository.addInvoice(sampleInvoice())
        val reloaded = FawratyRepository(context)
        assertNull(reloaded.storageError.value)
        assertEquals(saved, reloaded.invoices.value.single())
        assertEquals(company, reloaded.companyProfile.value)
        assertEquals(115.0, reloaded.clients.value.single().outstandingBalance, 0.0)
        reloaded.updateInvoiceStatus(saved.id, InvoiceStatus.PAID)
        assertEquals(0.0, reloaded.clients.value.single().outstandingBalance, 0.0)
        assertEquals(115.0, reloaded.clients.value.single().totalBilled, 0.0)
        reloaded.deleteInvoice(saved.id)
        assertEquals(0, reloaded.clients.value.single().invoicesCount)
        assertTrue(FawratyRepository(context).invoices.value.isEmpty())
    }

    @Test fun independentInstancesReserveDistinctNumbers() {
        val first = FawratyRepository(context).generateNextInvoiceNumber()
        val second = FawratyRepository(context).generateNextInvoiceNumber()
        assertNotEquals(first, second)
    }

    @Test fun corruptRecordsArePreservedAndWritesAreBlocked() {
        val prefs = context.getSharedPreferences("fawraty_records", Context.MODE_PRIVATE)
        prefs.edit().putString("snapshot", "{broken").commit()
        val repository = FawratyRepository(context)
        assertNotNull(repository.storageError.value)
        try { repository.addInvoice(sampleInvoice()); fail("Write should be blocked") }
        catch (_: IllegalStateException) {}
        assertEquals("{broken", prefs.getString("snapshot", null))
    }

    @Test fun reminderSwitchesSurviveRecreation() {
        val options = ReminderOptions(false, true, false)
        FawratyRepository(context).updateReminderOptions(options)
        assertEquals(options, FawratyRepository(context).reminderOptions.value)
    }

    @Test fun draftsDoNotInflateFinancialTotalsOrClientBalances() {
        val repository = FawratyRepository(context)
        repository.addInvoice(sampleInvoice("DRAFT", InvoiceStatus.DRAFT))
        val stats = calculateFinancialStats(repository.invoices.value)
        assertEquals(0.0, stats.totalInvoiced, 0.0)
        assertEquals(0.0, stats.totalVat, 0.0)
        assertEquals(1, stats.draftCount)
        assertEquals(0.0, repository.clients.value.single().totalBilled, 0.0)
    }

    @Test fun failedTransactionCannotAppearAfterRefreshOrRecreation() {
        val repository = FawratyRepository(context)
        repository.updateCompanyProfile(CompanyProfile(companyName = "منشأة اختبار", vatNumber = "300000000000003"))
        val before = repository.addInvoice(sampleInvoice("SAVED"))
        context.openOrCreateDatabase("fawraty_records.db", Context.MODE_PRIVATE, null).use { database ->
            database.execSQL("CREATE TRIGGER fail_write BEFORE INSERT ON invoice_store BEGIN SELECT RAISE(ABORT, 'forced test failure'); END")
        }
        try { repository.addInvoice(sampleInvoice("FAILED")); fail("The transaction must fail") }
        catch (_: android.database.sqlite.SQLiteException) {}
        catch (_: IllegalStateException) {}
        assertEquals(listOf(before), repository.invoices.value)
        repository.refreshDates()
        assertEquals(listOf(before), repository.invoices.value)
        assertEquals(listOf(before), FawratyRepository(context).invoices.value)
    }

    @Test fun draftsCaptureValidatedSellerOnlyAtFirstIssuance() {
        val repository = FawratyRepository(context)
        val draft = repository.addInvoice(sampleInvoice("DRAFT", InvoiceStatus.DRAFT))
        assertNull(draft.seller)
        assertEquals("", draft.issuedAt)
        try { repository.updateInvoiceStatus(draft.id, InvoiceStatus.PENDING); fail("Missing seller VAT must block issuance") }
        catch (_: IllegalArgumentException) {}
        assertEquals(InvoiceStatus.DRAFT, repository.invoices.value.single().status)
        val seller = CompanyProfile(companyName = "منشأة مكتملة", vatNumber = "300000000000003", iban = "TEST ONLY")
        repository.updateCompanyProfile(seller)
        repository.updateInvoiceStatus(draft.id, InvoiceStatus.PENDING)
        val issued = repository.invoices.value.single()
        assertEquals(seller, issued.seller)
        assertTrue(issued.issuedAt.isNotBlank())
        repository.updateCompanyProfile(seller.copy(companyName = "اسم أحدث"))
        repository.updateInvoiceStatus(draft.id, InvoiceStatus.PAID)
        assertEquals(seller, repository.invoices.value.single().seller)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsFiniteOperandsWhoseProductExceedsTheFinancialRange() {
        InvoiceItem("x", "test", 1e200, 1e200)
    }
}
