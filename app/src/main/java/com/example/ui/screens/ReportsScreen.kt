package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.export.InvoiceExporter
import com.example.model.InvoiceStatus
import com.example.ui.components.GlassCard
import com.example.ui.components.ZatcaVerificationTag
import com.example.ui.theme.CardBackgroundGlass
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.GlassBlue
import com.example.ui.theme.GlassBorder
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
fun ReportsScreen(
    viewModel: FawratyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val stats by viewModel.stats.collectAsState()
    val company by viewModel.companyProfile.collectAsState()
    val invoices by viewModel.allInvoices.collectAsState()

    val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }

    val collectionRate = if (stats.totalInvoiced > 0) {
        (stats.totalPaid / stats.totalInvoiced).toFloat().coerceIn(0f, 1f)
    } else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "التقارير المالية والإقرار الضريبي",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "إحصائيات المبيعات وضريبة القيمة المضافة ZATCA",
                        color = RoyalGold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                ZatcaVerificationTag()
            }
        }

        // ZATCA VAT Declaration Summary Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                hasGoldHighlight = true
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ملخص إقرار ضريبة القيمة المضافة (الربع الرابع 2026)",
                            color = RoyalGold,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "إجمالي المبيعات الخاضعة للضريبة", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = "${numberFormat.format(stats.totalInvoiced - stats.totalVat)} ${company.currencyAr}",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "ضريبة المخرجات المستحقة (15%)", color = GlassBlue, style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = "${numberFormat.format(stats.totalVat)} ${company.currencyAr}",
                                color = GlassBlue,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(0.6.dp).background(GlassBorder))
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "معدل التحصيل المالي الفعلي: ${(collectionRate * 100).toInt()}%",
                        color = TextPrimary,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { collectionRate },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = StatusPaid,
                        trackColor = CardSurfaceElevated
                    )
                }
            }
        }

        // Financial Metrics Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StatusPaid.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Payments, contentDescription = null, tint = StatusPaid, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "المبالغ المحصلة فعلياً", color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text(text = "${stats.paidCount} فواتير مسددة بالكامل", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Text(
                            text = "${numberFormat.format(stats.totalPaid)} ${company.currencyAr}",
                            color = StatusPaid,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StatusPending.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, tint = StatusPending, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "المستحقات المعلقة والمتأخرة", color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text(text = "${stats.pendingCount + stats.overdueCount} فواتير قيد التحصيل", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Text(
                            text = "${numberFormat.format(stats.totalPending)} ${company.currencyAr}",
                            color = StatusPending,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Export All Invoices to CSV
        item {
            Button(
                onClick = {
                    if (invoices.isNotEmpty()) {
                        val first = invoices.first()
                        val csv = InvoiceExporter.generateCsv(context, first, company)
                        InvoiceExporter.shareFile(context, csv, "text/csv", "سجل الفواتير الشامل")
                        Toast.makeText(context, "تم تجهيز ملف التقرير CSV", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GlassBlue)
            ) {
                Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "تصدير التقرير المالي كـ CSV", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
