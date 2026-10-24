package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.*
import com.example.repository.FawratyRepository
import com.example.viewmodel.FawratyViewModel
import com.example.viewmodel.calculateFinancialStats
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class QuoteStorageTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    @Before fun resetSyntheticStorage() {
        context.deleteDatabase("fawraty_records.db")
        context.getSharedPreferences("fawraty_records", Context.MODE_PRIVATE).edit().clear().commit()
    }
    private fun draft(number: String = "QTE-TEST") = sampleInvoice(number, InvoiceStatus.DRAFT).copy(
        issueDate = InvoiceDates.today(), dueDate = InvoiceDates.plusDays(InvoiceDates.today(), 14),
        seller = CompanyProfile(companyName = "منشأة اختبار"), issuedAt = InvoiceDates.utcNow()
    )

    @Test fun quotesPersistWithoutIncreasingSalesOrClientDebt() {
        val repository = FawratyRepository(context)
        val saved = repository.saveQuote(draft())
        val recreated = FawratyRepository(context)
        assertEquals(saved, recreated.quotes.value.single())
        assertTrue(recreated.invoices.value.isEmpty())
        assertEquals(0.0, calculateFinancialStats(recreated.invoices.value).totalInvoiced, 0.0)
        assertEquals(0.0, recreated.clients.value.single().outstandingBalance, 0.0)
        assertEquals(0, recreated.clients.value.single().invoicesCount)
    }

    @Test fun quoteNumbersPersistSeparatelyAndAreNotReusedAfterDeletion() {
        val repository = FawratyRepository(context)
        val first = repository.generateNextQuoteNumber()
        assertTrue(first.startsWith("QTE-" + InvoiceDates.today().take(4)))
        val quote = repository.saveQuote(draft(first))
        repository.deleteQuote(quote.id)
        val recreated = FawratyRepository(context)
        assertNotEquals(first, recreated.generateNextQuoteNumber())
        assertTrue(recreated.generateNextInvoiceNumber().endsWith("-001"))
    }

    @Test fun conversionPreservesAgreedAmountsAndBrandingAndIsIdempotent() {
        val repository = FawratyRepository(context)
        val seller = CompanyProfile(companyName = "هوية العرض", logoFileName = "logo_" + "a".repeat(64) + ".png")
        val saved = repository.saveQuote(draft().copy(seller = seller, items = listOf(
            InvoiceItem("a", "خدمة", 2.0, 10.335, discount = 0.50)
        )))
        repository.updateCompanyProfile(CompanyProfile(companyName = "هوية أخرى"))
        val invoice = repository.convertQuoteToInvoice(saved.id)
        assertTrue(invoice.invoiceNumber.startsWith("INV-"))
        assertNotEquals(saved.id, invoice.id)
        assertEquals(saved.document.items, invoice.items)
        assertEquals(saved.document.grandTotal, invoice.grandTotal, 0.0)
        assertEquals(seller, invoice.seller)
        assertEquals(InvoiceDates.today(), invoice.issueDate)
        assertEquals(InvoiceStatus.PENDING, invoice.status)
        assertEquals(invoice.id, FawratyRepository(context).convertQuoteToInvoice(saved.id).id)
        assertEquals(1, FawratyRepository(context).invoices.value.size)
        assertEquals(invoice.id, repository.quotes.value.single().convertedInvoiceId)
    }

    @Test fun convertedQuotesCannotBeEditedOrTurnIntoASecondInvoice() {
        val repository = FawratyRepository(context)
        val quote = repository.saveQuote(draft())
        val invoice = repository.convertQuoteToInvoice(quote.id)
        try { repository.saveQuote(quote.document.copy(notes = "changed")); fail("Converted quote must be immutable") }
        catch (_: IllegalArgumentException) {}
        assertEquals(invoice.id, repository.convertQuoteToInvoice(quote.id).id)
        repository.deleteInvoice(invoice.id)
        try { repository.convertQuoteToInvoice(quote.id); fail("Deleted linked invoice must not be recreated silently") }
        catch (_: IllegalArgumentException) {}
    }

    @Test fun conversionFailureRollsBackTheInvoiceLinkAndNumberReservation() {
        val repository = FawratyRepository(context)
        val quote = repository.saveQuote(draft())
        context.openOrCreateDatabase("fawraty_records.db", Context.MODE_PRIVATE, null).use {
            it.execSQL("CREATE TRIGGER fail_quote_write BEFORE INSERT ON invoice_store BEGIN SELECT RAISE(ABORT, 'synthetic failure'); END")
        }
        try { repository.convertQuoteToInvoice(quote.id); fail("Must fail atomically") }
        catch (_: android.database.sqlite.SQLiteException) {}
        catch (_: IllegalStateException) {}
        repository.refreshDates()
        assertTrue(repository.invoices.value.isEmpty())
        assertNull(repository.quotes.value.single().convertedInvoiceId)
        val recreated = FawratyRepository(context)
        assertTrue(recreated.invoices.value.isEmpty())
        assertNull(recreated.quotes.value.single().convertedInvoiceId)
        context.openOrCreateDatabase("fawraty_records.db", Context.MODE_PRIVATE, null).use {
            it.execSQL("DROP TRIGGER fail_quote_write")
        }
        assertTrue(recreated.generateNextInvoiceNumber().endsWith("-001"))
    }

    @Test fun expiredQuotesNeedAnExplicitValidityUpdateBeforeConversion() {
        val repository = FawratyRepository(context)
        val quote = repository.saveQuote(draft().copy(
            issueDate = InvoiceDates.plusDays(InvoiceDates.today(), -10),
            dueDate = InvoiceDates.plusDays(InvoiceDates.today(), -1)
        ))
        try { repository.convertQuoteToInvoice(quote.id); fail("Expired quote requires a renewed validity") }
        catch (_: IllegalArgumentException) {}
        assertTrue(repository.invoices.value.isEmpty())
    }

    @Test fun deletingAConvertedQuoteDoesNotDeleteItsInvoice() {
        val repository = FawratyRepository(context)
        val quote = repository.saveQuote(draft())
        val invoice = repository.convertQuoteToInvoice(quote.id)
        repository.deleteQuote(quote.id)
        val recreated = FawratyRepository(context)
        assertTrue(recreated.quotes.value.isEmpty())
        assertEquals(invoice.id, recreated.invoices.value.single().id)
    }

    @Test fun oldSnapshotsWithoutQuotesLoadAndKeepTheirInvoices() {
        val invoice = sampleInvoice("OLD")
        val adapter = com.squareup.moshi.Moshi.Builder()
            .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build().adapter(Invoice::class.java)
        val raw = """{"version":1,"company":{"companyName":"منشأة اختبار"},"invoices":[${adapter.toJson(invoice)}]}"""
        context.getSharedPreferences("fawraty_records", Context.MODE_PRIVATE).edit().putString("snapshot", raw).commit()
        val repository = FawratyRepository(context)
        assertNull(repository.storageError.value)
        assertTrue(repository.quotes.value.isEmpty())
        repository.saveQuote(draft())
        assertEquals(invoice.id, FawratyRepository(context).invoices.value.single().id)
    }

    @Test fun invoiceCreationCanOverrideItsLogoWithoutChangingTheCompanyProfile() {
        val repository = FawratyRepository(context)
        val model = FawratyViewModel(repository)
        val fixture = sampleInvoice()
        val custom = "logo_" + "c".repeat(64) + ".png"
        val invoice = model.createInvoice(fixture.clientName, fixture.clientCompany, "", "", "",
            fixture.clientAddress, fixture.issueDate, fixture.dueDate, fixture.projectCategory,
            fixture.items, "", "", logoFileName = custom)
        assertEquals(custom, invoice.seller!!.logoFileName)
        assertNull(repository.companyProfile.value.logoFileName)
    }
}
