package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.core.app.ApplicationProvider
import com.example.model.*
import com.example.repository.FawratyRepository
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import org.junit.Assert.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ui.navigation.FawratyApp
import com.example.ui.screens.InvoiceEditorScreen
import com.example.viewmodel.FawratyViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DocumentCreationUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun invoiceCreationOffersACompanyLogoPicker() {
        compose.setContent { MaterialTheme { InvoiceEditorScreen(FawratyViewModel()) } }
        compose.onNodeWithTag("document_logo_picker").assertIsDisplayed()
        compose.onNodeWithText("اختيار شعار من الصور").assertIsDisplayed()
    }

    @Test fun quotationCanBeCreatedAndConvertedFromTheApp() {
        val model = FawratyViewModel()
        val baseline = model.allInvoices.value.size
        val customer = "عميل اختبار " + java.util.UUID.randomUUID().toString().take(8)
        compose.setContent { FawratyApp(model) }
        compose.onNodeWithTag("nav_quotes").performClick()
        compose.onNodeWithText("عرض سعر جديد").performClick()
        compose.onNodeWithTag("document_editor_scroll").performScrollToNode(hasTestTag("document_client_name"))
        compose.onNodeWithTag("document_client_name").performTextInput(customer)
        compose.onNodeWithTag("document_editor_scroll").performScrollToNode(hasTestTag("document_item_description_0"))
        compose.onNodeWithTag("document_item_description_0").performTextInput("خدمة اختبار")
        compose.onNodeWithTag("document_item_price_0").performTextReplacement("100")
        compose.onNodeWithTag("document_editor_scroll").performScrollToNode(hasTestTag("issue_invoice_submit_button"))
        compose.onNodeWithTag("issue_invoice_submit_button").performClick()
        compose.runOnIdle {
            assertEquals(baseline, model.allInvoices.value.size)
            assertTrue(model.quotes.value.any { it.document.clientName == customer })
        }
        compose.onNodeWithTag("quote_detail_scroll").performScrollToNode(hasText("تحويل إلى فاتورة"))
        compose.onNodeWithText("تحويل إلى فاتورة").performClick()
        compose.runOnIdle {
            val quote = model.quotes.value.first { it.document.clientName == customer }
            assertNotNull(quote.convertedInvoiceId)
            assertEquals(baseline + 1, model.allInvoices.value.size)
            assertEquals(115.0, model.allInvoices.value.first { it.id == quote.convertedInvoiceId }.grandTotal, 0.0)
        }
    }


    private fun editingFixture(): Pair<FawratyViewModel, Quote> {
        val repository = FawratyRepository(ApplicationProvider.getApplicationContext())
        val doc = Invoice(
            id = java.util.UUID.randomUUID().toString(), invoiceNumber = repository.generateNextQuoteNumber(),
            clientName = "عميل استعادة تجريبي", clientCompany = "شركة اختبار", clientPhone = "",
            clientEmail = "", clientVatNumber = "", clientAddress = "", issueDate = InvoiceDates.today(),
            dueDate = InvoiceDates.plusDays(InvoiceDates.today(), 14), projectCategory = "اختبار",
            items = listOf(InvoiceItem(java.util.UUID.randomUUID().toString(), "خدمة تجريبية", 1.0, 100.0)),
            status = InvoiceStatus.DRAFT, seller = CompanyProfile(companyName = "منشأة اختبار")
        )
        val quote = repository.saveQuote(doc)
        return FawratyViewModel(repository).also { it.openCreateQuote(quote) } to quote
    }

    @Test fun restoringTheEditorKeepsChangedQuoteAmounts() {
        val (model, quote) = editingFixture()
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MaterialTheme { InvoiceEditorScreen(model, quoteMode = true) } }
        compose.onNodeWithTag("document_editor_scroll").performScrollToNode(hasTestTag("document_item_price_0"))
        compose.onNodeWithTag("document_item_price_0").performTextReplacement("250")
        compose.onNodeWithTag("document_item_quantity_0").performTextReplacement("2")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("document_editor_scroll").performScrollToNode(hasTestTag("issue_invoice_submit_button"))
        compose.onNodeWithTag("issue_invoice_submit_button").performClick()
        compose.runOnIdle {
            val saved = model.quotes.value.first { it.id == quote.id }.document
            assertEquals(250.0, saved.items.single().unitPrice, 0.0)
            assertEquals(2.0, saved.items.single().quantity, 0.0)
            assertEquals(575.0, saved.grandTotal, 0.0)
        }
    }

    @Test fun restoringInvalidInputCannotSilentlySaveTheOldPrice() {
        val (model, quote) = editingFixture()
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MaterialTheme { InvoiceEditorScreen(model, quoteMode = true) } }
        compose.onNodeWithTag("document_editor_scroll").performScrollToNode(hasTestTag("document_item_price_0"))
        compose.onNodeWithTag("document_item_price_0").performTextReplacement("not-a-number")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("document_editor_scroll").performScrollToNode(hasTestTag("issue_invoice_submit_button"))
        compose.onNodeWithTag("issue_invoice_submit_button").performClick()
        compose.runOnIdle {
            assertEquals(com.example.viewmodel.SubScreen.CREATE_QUOTE, model.activeSubScreen.value)
            assertEquals(100.0, model.quotes.value.first { it.id == quote.id }.document.items.single().unitPrice, 0.0)
        }
    }

    @Test fun quotationsHaveTheirOwnCreationEntry() {
        compose.setContent { FawratyApp(FawratyViewModel()) }
        compose.onNodeWithTag("nav_quotes").assertIsDisplayed().performClick()
        compose.onNodeWithText("عرض سعر جديد").assertIsDisplayed().performClick()
        compose.onNodeWithText("إنشاء عرض سعر").assertIsDisplayed()
    }
}
