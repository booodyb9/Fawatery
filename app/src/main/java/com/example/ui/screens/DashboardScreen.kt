package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Invoice
import com.example.model.InvoiceStatus
import com.example.reminders.ReminderManager
import com.example.ui.components.GlassCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.ZatcaVerificationTag
import com.example.ui.theme.CardBackgroundGlass
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.GlassBlue
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassGoldBorder
import com.example.ui.theme.IosShadowColor
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalGoldLight
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPending
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.FawratyViewModel
import com.example.viewmodel.NavigationTab
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: FawratyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val company by viewModel.companyProfile.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val allInvoices by viewModel.allInvoices.collectAsState()
    val filteredInvoices by viewModel.filteredInvoices.collectAsState()
    val selectedFilter by viewModel.statusFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val reminderOptions by viewModel.reminderOptions.collectAsState()
    val today by androidx.compose.runtime.produceState(com.example.model.InvoiceDates.today()) {
        while (true) { kotlinx.coroutines.delay(60_000); value = com.example.model.InvoiceDates.today() }
    }
    val automatedReminders = remember(allInvoices, reminderOptions, today) {
        ReminderManager.evaluateReminders(allInvoices, options = reminderOptions)
    }

    var showReminderModal by remember { mutableStateOf(false) }

    val numberFormat = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Executive Top Bar
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.fawraty_logo),
                        contentDescription = "Fawraty Logo",
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "فاتورتي للفوترة الذكية",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = company.companyName.take(30) + "..",
                            color = RoyalGold,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ZatcaVerificationTag()
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, if (automatedReminders.isNotEmpty()) RoyalGold else GlassBorder, CircleShape)
                            .clickable { showReminderModal = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Reminders",
                            tint = if (automatedReminders.isNotEmpty()) StatusPending else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        if (automatedReminders.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(StatusPending)
                            )
                        }
                    }
                }
            }
        }

        // Cinematic Architectural Hero Showcase (iOS Frosted White)
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                hasGoldHighlight = true,
                onClick = { viewModel.openCreateInvoice() }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.hero_riyadh_glass),
                    contentDescription = "Riyadh Glass Facade",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // High-end iOS frosted light glassmorphism overlay gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x10FFFFFF),
                                    Color(0x60FFFFFF),
                                    Color(0xF5FFFFFF)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(RoyalGold)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "فخامة الزجاج المعماري والتصميم النخبوي",
                                color = RoyalGold,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "إصدار وإدارة الفواتير المعمارية الفاخرة",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "فواتير إلكترونية متوافقة كلياً مع اشتراطات هيئة الزكاة والضريبة",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }
        }

        // Automated Reminders Notice Banner (if any due)
        if (automatedReminders.isNotEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0x35F59E0B),
                    borderColor = StatusPending,
                    hasGoldHighlight = true,
                    onClick = { showReminderModal = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x30F59E0B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = "Schedule",
                                    tint = StatusPending,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "اقتراحات تذكير السداد (${automatedReminders.size})",
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "يوجد فواتير تستحق خلال 3 أيام أو اليوم بانتظار إرسال التذكير",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                                )
                            }
                        }

                        Button(
                            onClick = { showReminderModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusPending),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = "إرسال التذكيرات",
                                color = DeepObsidian,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Quick Actions Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // New Invoice Action
                Button(
                    onClick = { viewModel.openCreateInvoice() },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("new_invoice_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalGold)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New",
                        tint = DeepObsidian,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "فاتورة جديدة",
                        color = DeepObsidian,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Quick Clients
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    onClick = { viewModel.setTab(NavigationTab.CLIENTS) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Group,
                            contentDescription = "Clients",
                            tint = GlassBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "العملاء",
                            color = TextPrimary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                // Quick Catalog
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    onClick = { viewModel.setTab(NavigationTab.CATALOG) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Architecture,
                            contentDescription = "Catalog",
                            tint = RoyalGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "الكتالوج",
                            color = TextPrimary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        // Executive Financial Stat Cards (2x2 Grid)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Sales
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        hasGoldHighlight = true
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "إجمالي الفواتير",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = RoyalGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${numberFormat.format(stats.totalInvoiced)} ${company.currencyAr}",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${stats.totalCount} فواتير معتمدة",
                                color = RoyalGold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    // Collected / Paid
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        borderColor = StatusPaid.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "المحصل فعلياً",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusPaid,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${numberFormat.format(stats.totalPaid)} ${company.currencyAr}",
                                color = StatusPaid,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${stats.paidCount} فواتير مسددة",
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pending Receivables
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        borderColor = StatusPending.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "بانتظار التحصيل",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = StatusPending,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${numberFormat.format(stats.totalPending)} ${company.currencyAr}",
                                color = StatusPending,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${stats.pendingCount + stats.overdueCount} مستحقات قيد المتابعة",
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    // VAT Collected 15%
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        borderColor = GlassBlue.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ضريبة القيمة 15%",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = GlassBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${numberFormat.format(stats.totalVat)} ${company.currencyAr}",
                                color = GlassBlue,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "مجهزة للإقرار الضريبي",
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }

        // Search and Filter Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("invoice_search_input"),
                    placeholder = {
                        Text(
                            text = "ابحث برقم الفاتورة، اسم العميل، أو المشروع..",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = GlassBlue
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlassBlue,
                        unfocusedBorderColor = Color(0x30007AFF),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xEAFFFFFF),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = GlassBlue
                    )
                )

                // Status Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChipItem(
                        title = "الكل (${stats.totalCount})",
                        isSelected = selectedFilter == null,
                        onClick = { viewModel.setStatusFilter(null) }
                    )
                    FilterChipItem(
                        title = "مدفوعة (${stats.paidCount})",
                        isSelected = selectedFilter == InvoiceStatus.PAID,
                        activeColor = StatusPaid,
                        onClick = { viewModel.setStatusFilter(InvoiceStatus.PAID) }
                    )
                    FilterChipItem(
                        title = "بانتظار التحصيل (${stats.pendingCount})",
                        isSelected = selectedFilter == InvoiceStatus.PENDING,
                        activeColor = StatusPending,
                        onClick = { viewModel.setStatusFilter(InvoiceStatus.PENDING) }
                    )
                    FilterChipItem(
                        title = "متأخرة (${stats.overdueCount})",
                        isSelected = selectedFilter == InvoiceStatus.OVERDUE,
                        activeColor = MaterialTheme.colorScheme.error,
                        onClick = { viewModel.setStatusFilter(InvoiceStatus.OVERDUE) }
                    )
                    FilterChipItem(
                        title = "مسودات (${stats.draftCount})",
                        isSelected = selectedFilter == InvoiceStatus.DRAFT,
                        activeColor = Color(0xFF8B5CF6),
                        onClick = { viewModel.setStatusFilter(InvoiceStatus.DRAFT) }
                    )
                }
            }
        }

        // Section Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل الفواتير (${filteredInvoices.size})",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "عرض الكل",
                    color = GlassBlue,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.clickable { viewModel.setTab(NavigationTab.INVOICES) }
                )
            }
        }

        // Invoices List
        if (filteredInvoices.isEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا توجد فواتير مطابقة للبحث",
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "جرب تغيير مصطلح البحث أو الفلتر المختار",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        } else {
            items(filteredInvoices, key = { it.id }) { invoice ->
                InvoiceCardItem(
                    invoice = invoice,
                    currency = company.currencyAr,
                    numberFormat = numberFormat,
                    onClick = { viewModel.openInvoiceDetail(invoice) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Automated Reminders Modal Bottom Sheet / Dialog
    if (showReminderModal) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showReminderModal = false }
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shapeRadius = 20.dp,
                backgroundColor = CardBackgroundGlass,
                hasGoldHighlight = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "جدول اقتراحات التذكير للسداد",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "تذكير قبل 3 أيام وفي يوم الاستحقاق",
                                color = RoyalGold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        IconButton(onClick = { showReminderModal = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (automatedReminders.isEmpty()) {
                        Text(
                            text = "رائع! لا توجد فواتير مستحقة التذكير حالياً.",
                            color = StatusPaid,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(300.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(automatedReminders) { reminder ->
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    backgroundColor = CardSurfaceElevated
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = reminder.invoice.invoiceNumber,
                                                color = RoyalGold,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = reminder.type.titleAr,
                                                color = StatusPending,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${reminder.invoice.clientName} (${reminder.invoice.clientCompany})",
                                            color = TextPrimary,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "المبلغ: ${numberFormat.format(reminder.invoice.grandTotal)} ${company.currencyAr} | الاستحقاق: ${reminder.targetDate}",
                                            color = TextSecondary,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    ReminderManager.sendWhatsAppReminder(
                                                        context,
                                                        reminder.invoice,
                                                        company,
                                                        reminder.type
                                                    )
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Send,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "واتساب",
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    ReminderManager.sendSmsReminder(
                                                        context,
                                                        reminder.invoice,
                                                        company,
                                                        reminder.type
                                                    )
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = GlassBlue),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "رسالة SMS",
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showReminderModal = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CardSurfaceElevated),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "إغلاق النافذة", color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChipItem(
    title: String,
    isSelected: Boolean,
    activeColor: Color = GlassBlue,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) activeColor else Color(0xF4FFFFFF)
    val borderColor = if (isSelected) activeColor else Color(0x35007AFF)
    val textColor = if (isSelected) Color.White else TextPrimary

    Box(
        modifier = Modifier
            .shadow(
                elevation = if (isSelected) 4.dp else 1.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = IosShadowColor
            )
            .clip(RoundedCornerShape(22.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = textColor,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        )
    }
}

@Composable
fun InvoiceCardItem(
    invoice: Invoice,
    currency: String,
    numberFormat: NumberFormat,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("invoice_item_${invoice.invoiceNumber}"),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = invoice.invoiceNumber,
                        color = RoyalGold,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${invoice.projectCategory}",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                StatusBadge(status = invoice.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = invoice.clientCompany.ifEmpty { invoice.clientName },
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            if (invoice.clientCompany.isNotEmpty() && invoice.clientCompany != invoice.clientName) {
                Text(
                    text = "عناية: ${invoice.clientName}",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.6.dp)
                    .background(GlassBorder)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "المبلغ الإجمالي (شامل الضريبة)",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "${numberFormat.format(invoice.grandTotal)} $currency",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "الاستحقاق: ${invoice.dueDate}",
                            color = if (invoice.status == InvoiceStatus.OVERDUE) MaterialTheme.colorScheme.error else TextSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "${invoice.totalItemsCount} بنود",
                            color = GlassBlue,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = "View",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
