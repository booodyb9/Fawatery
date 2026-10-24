package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.export.InvoiceExporter
import com.example.model.Quote
import com.example.ui.components.CompanyLogo
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.viewmodel.FawratyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private fun quoteMoney(value: Double) = String.format(Locale.US, "%,.2f", value)

@Composable
fun QuotesScreen(viewModel: FawratyViewModel) {
    val quotes by viewModel.quotes.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).testTag("quotes_list_scroll"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text("عروض الأسعار", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
            Text("عروض مستقلة عن الفواتير والتحصيل", color = TextSecondary)
        }
        item {
            Button(onClick = { viewModel.openCreateQuote() }, modifier = Modifier.fillMaxWidth().testTag("create_quote_button")) {
                Text("عرض سعر جديد")
            }
        }
        if (quotes.isEmpty()) item {
            Text("لا توجد عروض أسعار بعد. أنشئ عرضًا وأرسله PDF لعميلك.", color = TextSecondary,
                modifier = Modifier.padding(vertical = 28.dp))
        }
        items(quotes, key = { it.id }) { quote ->
            GlassCard(modifier = Modifier.fillMaxWidth().clickable { viewModel.openQuoteDetail(quote) }
                .testTag("quote_${quote.id}")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(quote.number, color = RoyalGold, fontWeight = FontWeight.Bold)
                    Text(quote.document.clientName, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text("صالح حتى: ${quote.validUntil}", color = TextSecondary)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(quote.labelAr, color = if (quote.isExpired() && quote.convertedInvoiceId == null) StatusOverdue else GlassBlue)
                        Text("${quoteMoney(quote.document.grandTotal)} ${quote.document.seller?.currencyAr ?: "ر.س"}",
                            color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun QuoteDetailScreen(quote: Quote, viewModel: FawratyViewModel) {
    val context = LocalContext.current
    val currentCompany by viewModel.companyProfile.collectAsState()
    val company = quote.document.seller ?: currentCompany
    val document = quote.document
    val scope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    if (deleting) AlertDialog(
        onDismissRequest = { deleting = false },
        title = { Text("حذف عرض السعر؟") },
        text = { Text("ستبقى أي فاتورة ناتجة عنه محفوظة.") },
        confirmButton = { TextButton(onClick = { deleting = false; viewModel.deleteQuote(quote.id) }) { Text("حذف العرض") } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("إلغاء") } }
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).testTag("quote_detail_scroll"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
                Column {
                    Text("عرض سعر", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                    Text(quote.number, color = RoyalGold, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CompanyLogo(company.logoFileName, Modifier.size(64.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(company.companyName, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    }
                    Text("العميل: ${document.clientName}", color = TextPrimary)
                    Text("تاريخ العرض: ${document.issueDate}", color = TextSecondary)
                    Text("صالح حتى: ${quote.validUntil}", color = TextSecondary)
                    Text(quote.labelAr, color = GlassBlue)
                    if (quote.isExpired() && quote.convertedInvoiceId == null)
                        Text("حدّث تاريخ الصلاحية من تعديل العرض قبل تحويله لفاتورة.", color = StatusOverdue)
                }
            }
        }
        item {
            Button(
                enabled = !exporting, modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (!exporting) scope.launch {
                        exporting = true
                        try {
                            val file = withContext(Dispatchers.IO) { InvoiceExporter.generateQuotePdf(context, quote, company) }
                            InvoiceExporter.shareFile(context, file, "application/pdf", "عرض السعر ${quote.number}")
                        } catch (cancelled: kotlinx.coroutines.CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            Toast.makeText(context, error.message ?: "تعذر تصدير العرض", Toast.LENGTH_LONG).show()
                        } finally { exporting = false }
                    }
                }
            ) { Text(if (exporting) "جارٍ تجهيز PDF…" else "تصدير عرض السعر PDF") }
        }
        item {
            if (quote.convertedInvoiceId == null) {
                Button(onClick = { viewModel.convertQuote(quote.id) }, enabled = !quote.isExpired(),
                    modifier = Modifier.fillMaxWidth()) { Text("تحويل إلى فاتورة") }
                TextButton(onClick = { viewModel.openCreateQuote(quote) }) { Text("تعديل العرض") }
            } else {
                Button(onClick = { viewModel.openQuoteInvoice(quote) }, modifier = Modifier.fillMaxWidth()) { Text("فتح الفاتورة الناتجة") }
            }
        }
        items(document.items, key = { it.id }) { item ->
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.description, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("${item.quantity} ${item.unit} × ${quoteMoney(item.unitPrice)}", color = TextSecondary)
                    if (item.discount > 0) Text("الخصم: ${quoteMoney(item.discount)}", color = TextSecondary)
                    Text("${quoteMoney(item.totalWithoutVat)} ${company.currencyAr}", color = RoyalGold)
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("المجموع: ${quoteMoney(document.subtotal)} ${company.currencyAr}", color = TextPrimary)
                    Text("الضريبة: ${quoteMoney(document.vatAmount)} ${company.currencyAr}", color = TextSecondary)
                    Text("إجمالي العرض: ${quoteMoney(document.grandTotal)} ${company.currencyAr}",
                        color = RoyalGold, style = MaterialTheme.typography.titleLarge)
                    if (document.paymentTerms.isNotBlank()) Text("شروط الدفع: ${document.paymentTerms}", color = TextSecondary)
                    if (document.notes.isNotBlank()) Text("ملاحظات: ${document.notes}", color = TextSecondary)
                }
            }
        }
        item { TextButton(onClick = { deleting = true }) { Text("حذف عرض السعر") } }
    }
}
