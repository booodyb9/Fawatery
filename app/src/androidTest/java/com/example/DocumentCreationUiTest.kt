package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    @Test fun quotationsHaveTheirOwnCreationEntry() {
        compose.setContent { FawratyApp(FawratyViewModel()) }
        compose.onNodeWithTag("nav_quotes").assertIsDisplayed().performClick()
        compose.onNodeWithText("عرض سعر جديد").assertIsDisplayed().performClick()
        compose.onNodeWithText("إنشاء عرض سعر").assertIsDisplayed()
    }
}
