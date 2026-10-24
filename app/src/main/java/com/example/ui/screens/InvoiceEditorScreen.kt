package com.example.ui.screens

import android.widget.Toast
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import com.example.ui.components.DocumentLogoPicker
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Client
import com.example.model.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.model.InvoiceStatus
import com.example.ui.components.GlassCard
import com.example.ui.theme.CardBackgroundGlass
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.GlassBlue
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassGoldBorder
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.FawratyViewModel
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

@Composable
fun InvoiceEditorScreen(
    viewModel: FawratyViewModel,
    modifier: Modifier = Modifier,
    quoteMode: Boolean = false
) {
    val context = LocalContext.current
    val clients by viewModel.clients.collectAsState()
    val catalog by viewModel.catalog.collectAsState()
    val company by viewModel.companyProfile.collectAsState()
    val prefillProduct by viewModel.prefillProduct.collectAsState()
    val editingQuote by viewModel.editingQuote.collectAsState()
    val seed = if (quoteMode) editingQuote?.document else null
    val taxRate = seed?.taxRate ?: company.defaultTaxRate
    val startingLogo = if (seed?.seller != null) seed.seller.logoFileName else company.logoFileName
    var selectedLogo by rememberSaveable(seed?.id) { mutableStateOf(startingLogo) }
    var logoBusy by remember { mutableStateOf(false) }


    val formErrors = rememberSaveable(seed?.id, saver = documentErrorsSaver) { mutableStateMapOf<String, String>() }
    var selectedClientId by rememberSaveable(seed?.id) { mutableStateOf(seed?.clientId) }
    var clientName by rememberSaveable(seed?.id) { mutableStateOf(seed?.clientName.orEmpty()) }
    var clientCompany by rememberSaveable(seed?.id) { mutableStateOf(seed?.clientCompany.orEmpty()) }
    var clientPhone by rememberSaveable(seed?.id) { mutableStateOf(seed?.clientPhone.orEmpty()) }
    var clientEmail by rememberSaveable(seed?.id) { mutableStateOf(seed?.clientEmail.orEmpty()) }
    var clientVatNumber by rememberSaveable(seed?.id) { mutableStateOf(seed?.clientVatNumber.orEmpty()) }
    var clientAddress by rememberSaveable(seed?.id) { mutableStateOf(seed?.clientAddress.orEmpty()) }

    var issueDate by rememberSaveable(seed?.id) { mutableStateOf(seed?.issueDate ?: InvoiceDates.today()) }
    var dueDate by rememberSaveable(seed?.id) { mutableStateOf(seed?.dueDate ?: InvoiceDates.plusDays(InvoiceDates.today(), 14)) }
    var projectCategory by rememberSaveable(seed?.id) { mutableStateOf(seed?.projectCategory ?: "واجهات أبراج ومباني تجارية") }
    var notes by rememberSaveable(seed?.id) { mutableStateOf(seed?.notes ?: "الضمان 10 سنوات على أعمال الزجاج المعماري والعزل الحراري وكود البناء السعودي.") }
    var paymentTerms by rememberSaveable(seed?.id) { mutableStateOf(seed?.paymentTerms ?: "50% دفعة أولى، 40% عند التوريد، 10% عند التسليم النهائي.") }

    var showClientDropdown by remember { mutableStateOf(false) }
    var showCatalogDropdown by remember { mutableStateOf(false) }

    val initialItem = remember {
        prefillProduct?.let { product ->
            runCatching {
                InvoiceItem(UUID.randomUUID().toString(), product.name, 1.0, product.defaultPrice, unit = product.unit)
                    .also { calculateInvoiceTotals(listOf(it), taxRate) }
            }
        }
    }
    val items = rememberSaveable(seed?.id, saver = documentItemsSaver) {
        mutableStateListOf<InvoiceItem>().apply {
            if (seed != null) addAll(seed.items)
            else add(initialItem?.getOrNull() ?: InvoiceItem(UUID.randomUUID().toString(), "", 1.0, 0.0, unit = "متر مربع"))
        }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        initialItem?.exceptionOrNull()?.let {
            Toast.makeText(context, it.message ?: "قيمة المنتج تتجاوز الحد المسموح للفاتورة", Toast.LENGTH_LONG).show()
        }
    }

    val numberFormat = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    fun addItemSafely(newItem: InvoiceItem) {
        try {
            calculateInvoiceTotals(items.toList() + newItem, taxRate)
            items.add(newItem)
        } catch (error: IllegalArgumentException) {
            Toast.makeText(context, error.message ?: "قيمة البند غير صالحة", Toast.LENGTH_LONG).show()
        }
    }
    val totals = calculateInvoiceTotals(items, taxRate)
    val subtotal = totals.subtotal
    val vatAmount = totals.vat
    val grandTotal = totals.grandTotal

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("document_editor_scroll")
            .background(Color.Transparent)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (quoteMode) { if (seed != null) "تعديل عرض السعر" else "إنشاء عرض سعر" } else "إنشاء فاتورة جديدة",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (quoteMode) "حفظ عرض مستقل عن المبيعات والتحصيل" else "إنشاء فاتورة وحفظها على الجهاز",
                        color = RoyalGold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                DocumentLogoPicker(selectedLogo, company.logoFileName,
                    onLogoChanged = { selectedLogo = it }, onBusyChanged = { logoBusy = it })
            }
        }

        // Client Selection & Information Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                hasGoldHighlight = true
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "بيانات العميل (المشتري)",
                            color = RoyalGold,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )

                        Box {
                            Button(
                                onClick = { showClientDropdown = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CardSurfaceElevated),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = GlassBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "اختر عميل مسجل",
                                    color = GlassBlue,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            DropdownMenu(
                                expanded = showClientDropdown,
                                onDismissRequest = { showClientDropdown = false },
                                modifier = Modifier.background(CardBackgroundGlass)
                            ) {
                                clients.forEach { client ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(text = client.name, color = TextPrimary, fontWeight = FontWeight.Bold)
                                                Text(text = client.company, color = TextSecondary, fontSize = 11.sp)
                                            }
                                        },
                                        onClick = {
                                            selectedClientId = client.id
                                            clientName = client.name
                                            clientCompany = client.company
                                            clientPhone = client.phone
                                            clientEmail = client.email
                                            clientVatNumber = client.vatNumber
                                            clientAddress = client.address
                                            showClientDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it; selectedClientId = null },
                        label = { Text("اسم العميل / المسؤول") },
                        modifier = Modifier.fillMaxWidth().testTag("document_client_name"),
                        shape = RoundedCornerShape(10.dp),
                        colors = getTextFieldColors()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = clientCompany,
                        onValueChange = { clientCompany = it; selectedClientId = null },
                        label = { Text("اسم المنشأة / الشركة أو المشروع") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = getTextFieldColors()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = clientPhone,
                            onValueChange = { clientPhone = it; selectedClientId = null },
                            label = { Text("رقم الهاتف") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = getTextFieldColors()
                        )
                        OutlinedTextField(
                            value = clientVatNumber,
                            onValueChange = { clientVatNumber = it },
                            label = { Text("الرقم الضريبي (اختياري، 15 رقم)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = getTextFieldColors()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = clientAddress,
                        onValueChange = { clientAddress = it },
                        label = { Text("العنوان وموقع المشروع بالرياض") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = getTextFieldColors()
                    )
                }
            }
        }

        // Invoice Meta Dates & Project Type
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "تواريخ الاستحقاق وتصنيف المشروع",
                        color = RoyalGold,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = issueDate,
                            onValueChange = { issueDate = it },
                            label = { Text("تاريخ الإصدار (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = getTextFieldColors()
                        )
                        OutlinedTextField(
                            value = dueDate,
                            onValueChange = { dueDate = it },
                            label = { Text(if (quoteMode) "صالح حتى" else "تاريخ الاستحقاق") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = getTextFieldColors()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = projectCategory,
                        onValueChange = { projectCategory = it },
                        label = { Text("تصنيف المشروع المعماري") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = getTextFieldColors()
                    )
                }
            }
        }

        // Line Items Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "بنود الفاتورة (${items.size})",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Add from Catalog Dropdown
                    Box {
                        Button(
                            onClick = { showCatalogDropdown = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CardSurfaceElevated),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Architecture,
                                contentDescription = null,
                                tint = RoyalGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "من الكتالوج", color = RoyalGold, style = MaterialTheme.typography.labelSmall)
                        }

                        DropdownMenu(
                            expanded = showCatalogDropdown,
                            onDismissRequest = { showCatalogDropdown = false },
                            modifier = Modifier.background(CardBackgroundGlass)
                        ) {
                            catalog.forEach { prod ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(text = prod.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(text = "${prod.defaultPrice} ر.س / ${prod.unit}", color = RoyalGold, fontSize = 11.sp)
                                        }
                                    },
                                    onClick = {
                                        addItemSafely(
                                            InvoiceItem(
                                                id = UUID.randomUUID().toString(),
                                                description = prod.name,
                                                quantity = 1.0,
                                                unitPrice = prod.defaultPrice,
                                                unit = prod.unit,
                                                discount = 0.0
                                            )
                                        )
                                        showCatalogDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Add Custom Item
                    Button(
                        onClick = {
                            addItemSafely(
                                InvoiceItem(
                                    id = UUID.randomUUID().toString(),
                                    description = "بند زجاجي جديد",
                                    quantity = 1.0,
                                    unitPrice = 100.0,
                                    unit = "متر مربع",
                                    discount = 0.0
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "بند مخصص", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // List of Line Items with Editable Fields
        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
            var desc by rememberSaveable(item.id) { mutableStateOf(item.description) }
            var qtyStr by rememberSaveable(item.id) { mutableStateOf(item.quantity.toString()) }
            var priceStr by rememberSaveable(item.id) { mutableStateOf(item.unitPrice.toString()) }
            var unitStr by rememberSaveable(item.id) { mutableStateOf(item.unit) }
            var discountStr by rememberSaveable(item.id) { mutableStateOf(item.discount.toString()) }

            fun updateItem() {
                val quantity = parseInvoiceDecimal(qtyStr)
                val price = parseInvoiceDecimal(priceStr)
                val discount = parseInvoiceDecimal(discountStr)
                if (quantity == null || price == null || discount == null) {
                    formErrors[item.id] = "أدخل الكمية والسعر والخصم كأرقام صحيحة"
                    return
                }
                try {
                    val updated = item.copy(description = desc, quantity = quantity, unitPrice = price, unit = unitStr, discount = discount)
                    calculateInvoiceTotals(items.toMutableList().apply { set(index, updated) }, taxRate)
                    items[index] = updated
                    if (desc.isBlank()) formErrors[item.id] = "وصف البند مطلوب" else formErrors.remove(item.id)
                } catch (error: IllegalArgumentException) {
                    formErrors[item.id] = error.message ?: "قيم البند غير صالحة"
                }
            }

            androidx.compose.runtime.LaunchedEffect(item.id, desc, qtyStr, priceStr, unitStr, discountStr) {
                updateItem()
            }

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
                            text = "البند #${index + 1}",
                            color = RoyalGold,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (items.size > 1) {
                            IconButton(
                                onClick = { formErrors.remove(item.id)
                                        items.removeAt(index) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = desc,
                        onValueChange = {
                            desc = it
                            updateItem()
                        },
                        label = { Text("بيان الصنف أو الخدمة") },
                        modifier = Modifier.fillMaxWidth().testTag("document_item_description_$index"),
                        shape = RoundedCornerShape(8.dp),
                        colors = getTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = qtyStr,
                            onValueChange = {
                                qtyStr = it
                                updateItem()
                            },
                            label = { Text("الكمية") },
                            modifier = Modifier.weight(1f).testTag("document_item_quantity_$index"),
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = getTextFieldColors()
                        )
                        OutlinedTextField(
                            value = unitStr,
                            onValueChange = {
                                unitStr = it
                                updateItem()
                            },
                            label = { Text("الوحدة") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = getTextFieldColors()
                        )
                        OutlinedTextField(
                            value = priceStr,
                            onValueChange = {
                                priceStr = it
                                updateItem()
                            },
                            label = { Text("السعر") },
                            modifier = Modifier.weight(1.2f).testTag("document_item_price_$index"),
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = getTextFieldColors()
                        )
                        OutlinedTextField(
                            value = discountStr,
                            onValueChange = {
                                discountStr = it
                                updateItem()
                            },
                            label = { Text("الخصم (ر.س)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = getTextFieldColors()
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "إجمالي البند: ${numberFormat.format(items[index].totalWithoutVat)} ${company.currencyAr}",
                            color = GlassBlue,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Live Financial Calculation Summary
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                hasGoldHighlight = true
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (quoteMode) "قيمة عرض السعر" else "الملخص المالي للفاتورة",
                        color = RoyalGold,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "المجموع قبل الضريبة", color = TextSecondary)
                        Text(text = "${numberFormat.format(subtotal)} ${company.currencyAr}", color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "ضريبة القيمة المضافة (${taxRate * 100}%)", color = GlassBlue)
                        Text(text = "${numberFormat.format(vatAmount)} ${company.currencyAr}", color = GlassBlue, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(GlassGoldBorder))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = if (quoteMode) "إجمالي العرض" else "المبلغ الإجمالي المستحق", color = RoyalGold, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = "${numberFormat.format(grandTotal)} ${company.currencyAr}",
                            color = RoyalGold,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Notes and Payment Terms
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = paymentTerms,
                        onValueChange = { paymentTerms = it },
                        label = { Text("شروط الدفع وجدول الدفعات") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = getTextFieldColors()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات الضمان والمواصفات الفنية") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = getTextFieldColors()
                    )
                }
            }
        }

        // Save & Issue Button
        item {
            Button(
                enabled = !logoBusy,
                onClick = {
                    if (items.any { it.description.isBlank() }) {
                        Toast.makeText(context, "أكمل وصف كل بند", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    if (clientName.isBlank()) {
                        Toast.makeText(context, "يرجى إدخال اسم العميل", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (formErrors.isNotEmpty()) {
                        Toast.makeText(context, formErrors.values.first(), Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    if (items.isEmpty()) {
                        Toast.makeText(context, "يرجى إضافة بند واحد على الأقل", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    try {
                    if (quoteMode) viewModel.createQuote(
                        clientName = clientName,
                        clientCompany = clientCompany.ifEmpty { clientName },
                        clientPhone = clientPhone.trim(), clientEmail = clientEmail,
                        clientVatNumber = clientVatNumber.trim(), clientAddress = clientAddress.trim(),
                        issueDate = issueDate, dueDate = dueDate, projectCategory = projectCategory,
                        items = items.toList(), notes = notes, paymentTerms = paymentTerms,
                        clientId = selectedClientId, logoFileName = selectedLogo
                    ) else viewModel.createInvoice(
                        clientName = clientName,
                        clientCompany = clientCompany.ifEmpty { clientName },
                        clientPhone = clientPhone.trim(), clientEmail = clientEmail,
                        clientVatNumber = clientVatNumber.trim(), clientAddress = clientAddress.trim(),
                        issueDate = issueDate, dueDate = dueDate, projectCategory = projectCategory,
                        items = items.toList(), notes = notes, paymentTerms = paymentTerms,
                        clientId = selectedClientId, logoFileName = selectedLogo,
                        status = InvoiceStatus.PENDING
                    )
                    Toast.makeText(context, if (quoteMode) "تم حفظ عرض السعر بنجاح" else "تم حفظ الفاتورة بنجاح", Toast.LENGTH_SHORT).show()
                    } catch (error: Exception) {
                        Toast.makeText(context, error.message ?: "تعذر حفظ الفاتورة", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("issue_invoice_submit_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GlassBlue)
            ) {
                Text(
                    text = if (quoteMode) "حفظ عرض السعر" else "إصدار وحفظ الفاتورة",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun getTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GlassBlue,
    unfocusedBorderColor = Color(0x35007AFF),
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color(0xF6FFFFFF),
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = GlassBlue,
    unfocusedLabelColor = TextSecondary,
    cursorColor = GlassBlue
)

private val documentItemsSaver = listSaver<SnapshotStateList<InvoiceItem>, Any>(
    save = { items -> items.flatMap { listOf<Any>(it.id, it.description, it.quantity, it.unitPrice, it.unit, it.discount) } },
    restore = { values ->
        mutableStateListOf<InvoiceItem>().apply {
            values.chunked(6).forEach { row ->
                add(InvoiceItem(row[0] as String, row[1] as String,
                    (row[2] as Number).toDouble(), (row[3] as Number).toDouble(),
                    unit = row[4] as String, discount = (row[5] as Number).toDouble()))
            }
        }
    }
)

private val documentErrorsSaver = mapSaver<SnapshotStateMap<String, String>>(
    save = { it.toMap() },
    restore = { values -> mutableStateMapOf<String, String>().apply {
        values.forEach { (key, value) -> this[key] = value as String }
    } }
)
