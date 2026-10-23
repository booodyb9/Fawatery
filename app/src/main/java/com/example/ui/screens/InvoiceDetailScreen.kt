package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.export.InvoiceExporter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.export.ReminderType
import com.example.model.Invoice
import com.example.model.InvoiceStatus
import com.example.reminders.ReminderManager
import com.example.ui.components.GlassCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.ZatcaQrCodeView
import com.example.ui.components.ZatcaVerificationTag
import com.example.ui.theme.CardBackgroundGlass
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.CardSurfaceVariant
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.GlassBlue
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassGoldBorder
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalGoldLight
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPending
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.FawratyViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun InvoiceDetailScreen(
    invoice: Invoice,
    viewModel: FawratyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentCompany by viewModel.companyProfile.collectAsState()
    val company = invoice.seller ?: currentCompany
    val exportScope = androidx.compose.runtime.rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }

    var showStatusDropdown by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }

    val numberFormat = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    val zatcaTlvString = remember(invoice, company) {
        if (invoice.status == InvoiceStatus.DRAFT) null else runCatching { invoice.generateZatcaTlvBase64(company.companyName, company.vatNumber) }.getOrNull()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Navigation Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CardBackgroundGlass)
                            .border(1.dp, GlassBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = invoice.invoiceNumber,
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "فاتورة محفوظة محليًا",
                            color = RoyalGold,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ZatcaVerificationTag()
                    Spacer(modifier = Modifier.width(8.dp))
                    Box {
                        Box(
                            modifier = Modifier
                                .clickable { showStatusDropdown = true }
                        ) {
                            StatusBadge(status = invoice.status)
                        }

                        DropdownMenu(
                            expanded = showStatusDropdown,
                            onDismissRequest = { showStatusDropdown = false },
                            modifier = Modifier.background(CardBackgroundGlass)
                        ) {
                            InvoiceStatus.entries.forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(text = status.labelAr, color = TextPrimary) },
                                    onClick = {
                                        if (viewModel.updateInvoiceStatus(invoice.id, status)) {
                                            showStatusDropdown = false
                                            Toast.makeText(context, "تم تحديث حالة الفاتورة", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Export Actions Toolbar (PDF, CSV, Automated Reminders)
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                hasGoldHighlight = true
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "خيارات التصدير والمشاركة والتذكيرات",
                        color = RoyalGold,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // PDF Export Button
                        Button(
                            enabled = !exporting,
                            onClick = {
                                exportScope.launch {
                                exporting = true
                                try {
                                    val pdfFile = withContext(kotlinx.coroutines.Dispatchers.IO) { InvoiceExporter.generatePdf(context, invoice, company) }
                                    InvoiceExporter.shareFile(
                                        context,
                                        pdfFile,
                                        "application/pdf",
                                        "فاتورة ضريبية ${invoice.invoiceNumber}"
                                    )
                                    Toast.makeText(context, "تم تجهيز ملف PDF بنجاح", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "فشل إنشاء PDF: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally { exporting = false }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("export_pdf_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalGold),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "PDF",
                                tint = DeepObsidian,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تصدير PDF",
                                color = DeepObsidian,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        // CSV Export Button
                        Button(
                            enabled = !exporting,
                            onClick = {
                                exportScope.launch {
                                exporting = true
                                try {
                                    val csvFile = withContext(kotlinx.coroutines.Dispatchers.IO) { InvoiceExporter.generateCsv(context, invoice, company) }
                                    InvoiceExporter.shareFile(
                                        context,
                                        csvFile,
                                        "text/csv",
                                        "بيانات فاتورة ${invoice.invoiceNumber}"
                                    )
                                    Toast.makeText(context, "تم تجهيز ملف CSV بنجاح", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "فشل إنشاء CSV: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally { exporting = false }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("export_csv_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = GlassBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = "CSV",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تصدير CSV",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        // Reminder Button
                        Button(
                            onClick = { showReminderDialog = true },
                            modifier = Modifier
                                .weight(1.1f)
                                .height(44.dp)
                                .testTag("send_reminder_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusPending),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Reminder",
                                tint = DeepObsidian,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تذكير بالسداد",
                                color = DeepObsidian,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Full Architectural Executive Invoice Preview Canvas
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shapeRadius = 20.dp,
                backgroundColor = Color.White,
                hasGoldHighlight = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Top Letterhead
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.fawraty_logo),
                                contentDescription = "Company Logo",
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, GlassGoldBorder, RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = company.companyName,
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = company.companyNameEn,
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = "سجل تجاري: ${company.commercialRegistration}",
                                    color = RoyalGold,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = "الرقم الضريبي: ${company.vatNumber}",
                                    color = GlassBlue,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(GlassBorder)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Meta Row: Issue Date, Due Date, Category
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "تاريخ الإصدار", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            Text(text = invoice.issueDate, color = TextPrimary, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Column {
                            Text(text = "تاريخ الاستحقاق", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            Text(text = invoice.dueDate, color = RoyalGold, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Column {
                            Text(text = "تصنيف المشروع", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            Text(text = invoice.projectCategory, color = GlassBlue, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Billed To Section
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = CardSurfaceElevated,
                        borderColor = GlassBorder
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "بيانات العميل والمشروع المكلف:",
                                color = RoyalGold,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = invoice.clientCompany.ifEmpty { invoice.clientName },
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "المسؤول: ${invoice.clientName} | هاتف: ${invoice.clientPhone}",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "الرقم الضريبي للعميل: ${invoice.clientVatNumber}",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "العنوان: ${invoice.clientAddress}",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Itemized Table Header
                    Text(
                        text = "تفاصيل بنود الزجاج المعماري والخدمات",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardSurfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(text = "البند", color = RoyalGold, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(2f))
                            Text(text = "الكمية", color = RoyalGold, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)
                            Text(text = "السعر", color = RoyalGold, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text(text = "الإجمالي", color = RoyalGold, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                        }
                    }

                    // Itemized Rows
                    invoice.items.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(2f)) {
                                Text(
                                    text = item.description,
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                )
                                if (item.discount > 0) {
                                    Text(
                                        text = "خصم: ${numberFormat.format(item.discount)} ${company.currencyAr}",
                                        color = StatusPaid,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            Text(
                                text = "${item.quantity} ${item.unit}",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(0.9f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = numberFormat.format(item.unitPrice),
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${numberFormat.format(item.totalWithoutVat)}",
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1.2f),
                                textAlign = TextAlign.End
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(GlassBorder)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Calculation & Totals Summary Card
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFFF6F9FD),
                        borderColor = GlassGoldBorder
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "المجموع الفرعي قبل الضريبة", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                                Text(text = "${numberFormat.format(invoice.subtotal)} ${company.currencyAr}", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "ضريبة القيمة المضافة (${invoice.taxRate * 100}%):", color = GlassBlue, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                Text(text = "${numberFormat.format(invoice.vatAmount)} ${company.currencyAr}", color = GlassBlue, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(GlassGoldBorder)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "المبلغ الإجمالي المستحق", color = RoyalGold, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    text = "${numberFormat.format(invoice.grandTotal)} ${company.currencyAr}",
                                    color = RoyalGold,
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // ZATCA QR Code & Payment Information
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = RoyalGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "الحساب البنكي المعتمد للتحويل:",
                                    color = RoyalGold,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "البنك: ${company.bankName}", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                            Text(text = "الآيبان IBAN:", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = company.iban,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = invoice.notes,
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (zatcaTlvString != null) {
                                ZatcaQrCodeView(dataString = zatcaTlvString, sizeDp = 130.dp)
                            } else {
                                Text("QR غير متاح: أكمل بيانات المنشأة وأصدر الفاتورة", color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "QR بيانات الفاتورة",
                                color = RoyalGoldLight,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                            )
                        }
                    }
                }
            }
        }

        // Secondary Action Buttons: Duplicate & Delete
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (viewModel.duplicateInvoice(invoice.id)) Toast.makeText(context, "تم تكرار الفاتورة بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Duplicate",
                        tint = GlassBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "تكرار الفاتورة", color = TextPrimary)
                }

                OutlinedButton(
                    onClick = {
                        if (viewModel.deleteInvoice(invoice.id)) Toast.makeText(context, "تم حذف الفاتورة", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "حذف", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Automated Reminder Dialog
    if (showReminderDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showReminderDialog = false }
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = CardBackgroundGlass,
                hasGoldHighlight = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "إرسال تذكير سداد آلي للعميل",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "العميل: ${invoice.clientName} (${invoice.clientCompany})",
                        color = RoyalGold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "اختر صيغة التذكير:",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ReminderType.entries.forEach { type ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            backgroundColor = CardSurfaceElevated,
                            onClick = {
                                ReminderManager.sendWhatsAppReminder(context, invoice, company, type)
                                showReminderDialog = false
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = type.titleAr,
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = type.subtitleAr,
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = Color(0xFF25D366),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showReminderDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CardSurfaceElevated)
                    ) {
                        Text(text = "إلغاء", color = TextPrimary)
                    }
                }
            }
        }
    }
}
