package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import com.example.branding.CompanyLogoStore
import com.example.ui.components.CompanyLogo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.components.ZatcaVerificationTag
import com.example.ui.theme.CardBackgroundGlass
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.GlassBlue
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassGoldBorder
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.FawratyViewModel

@Composable
fun SettingsScreen(
    viewModel: FawratyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val company by viewModel.companyProfile.collectAsState()

    var companyName by remember { mutableStateOf(company.companyName) }
    var companyNameEn by remember { mutableStateOf(company.companyNameEn) }
    var crNumber by remember { mutableStateOf(company.commercialRegistration) }
    var vatNumber by remember { mutableStateOf(company.vatNumber) }
    var address by remember { mutableStateOf(company.address) }
    var phone by remember { mutableStateOf(company.phone) }
    var email by remember { mutableStateOf(company.email) }
    var bankName by remember { mutableStateOf(company.bankName) }
    var iban by remember { mutableStateOf(company.iban) }
    var logoFileName by remember { mutableStateOf(company.logoFileName) }
    var importingLogo by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            importingLogo = true
            try {
                logoFileName = withContext(Dispatchers.IO) { CompanyLogoStore.importFromUri(context, uri) }
                Toast.makeText(context, "تم اختيار الشعار؛ اضغط حفظ التعديلات لتطبيقه", Toast.LENGTH_LONG).show()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Toast.makeText(context, error.message ?: "تعذر قراءة صورة الشعار", Toast.LENGTH_LONG).show()
            } finally { importingLogo = false }
        }
    }

    val reminderOptions by viewModel.reminderOptions.collectAsState()
    val autoRemind3Days = reminderOptions.threeDaysBefore
    val autoRemindDueDate = reminderOptions.onDueDate
    val autoRemindOverdue = reminderOptions.overdue

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
                        text = "إعدادات المنشأة وتخصيص الفاتورة",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "تخصيص الشعار، الألوان، البيانات الرسمية، ونظام التذكيرات",
                        color = RoyalGold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                ZatcaVerificationTag()
            }
        }

        // Logo & Visual Branding Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                hasGoldHighlight = true
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompanyLogo(
                        fileName = logoFileName,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, GlassGoldBorder, RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Palette,
                                contentDescription = null,
                                tint = RoyalGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "الهوية البصرية وشعار الفاتورة",
                                color = RoyalGold,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "اختر صورة للشعار ثم احفظ التعديلات. يظهر في الفواتير الجديدة وPDF، وتحتفظ الفواتير الصادرة بشعارها.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            enabled = !importingLogo,
                            onClick = { logoPicker.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = GlassBlue)
                        ) {
                            Text(if (importingLogo) "جارٍ تجهيز الشعار…" else "اختيار شعار من الصور")
                        }
                        if (logoFileName != null) {
                            androidx.compose.material3.TextButton(
                                enabled = !importingLogo,
                                onClick = { logoFileName = null }
                            ) { Text("استعادة الشعار الافتراضي") }
                        }
                    }
                }
            }
        }

        // Automated Reminders Configuration Section
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = CardBackgroundGlass
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = RoyalGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إظهار اقتراحات تذكير السداد",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Text(
                        text = "تُعرض داخل التطبيق؛ فتح واتساب أو SMS وإرسال الرسالة يحتاج إجراءً منك.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3 Days Before
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تذكير قبل 3 أيام من تاريخ الاستحقاق",
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "إظهار اقتراح تذكير خلال الأيام الثلاثة السابقة للاستحقاق",
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Switch(
                            checked = autoRemind3Days,
                            onCheckedChange = { viewModel.updateReminderOptions(reminderOptions.copy(threeDaysBefore = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = GlassBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // On Due Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تذكير في يوم الاستحقاق المالي",
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "إشعار العميل بأن الفاتورة أصبحت مستحقة السداد اليوم",
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Switch(
                            checked = autoRemindDueDate,
                            onCheckedChange = { viewModel.updateReminderOptions(reminderOptions.copy(onDueDate = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = GlassBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Overdue Followup
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "متابعة الفواتير المتأخرة",
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "تنبيهات فورية للمستحقات المتأخرة لتحصيل المستحقات",
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Switch(
                            checked = autoRemindOverdue,
                            onCheckedChange = { viewModel.updateReminderOptions(reminderOptions.copy(overdue = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = StatusOverdue)
                        )
                    }
                }
            }
        }

        // Official Business Profile Details
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "البيانات التجارية المطبوعة على الفاتورة",
                        color = RoyalGold,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("اسم الشركة (بالعربية)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = getSettingsFieldColors()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = companyNameEn,
                        onValueChange = { companyNameEn = it },
                        label = { Text("Company Name (English)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = getSettingsFieldColors()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = crNumber,
                            onValueChange = { crNumber = it },
                            label = { Text("السجل التجاري (CR)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = getSettingsFieldColors()
                        )
                        OutlinedTextField(
                            value = vatNumber,
                            onValueChange = { vatNumber = it },
                            label = { Text("الرقم الضريبي (اختياري)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = getSettingsFieldColors()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("العنوان الرسمي والمقر بالرياض") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = getSettingsFieldColors()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("رقم الهاتف") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = getSettingsFieldColors()
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("البريد الإلكتروني") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = getSettingsFieldColors()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("اسم البنك المعتمد") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = getSettingsFieldColors()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = iban,
                        onValueChange = { iban = it },
                        label = { Text("رقم الآيبان البنكي (IBAN)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = getSettingsFieldColors()
                    )
                }
            }
        }

        // Save Button
        item {
            Button(
                enabled = !importingLogo,
                onClick = {
                    val updated = company.copy(
                        companyName = companyName,
                        companyNameEn = companyNameEn,
                        commercialRegistration = crNumber,
                        vatNumber = vatNumber.trim(),
                        address = address,
                        phone = phone,
                        email = email,
                        bankName = bankName,
                        iban = iban,
                        logoFileName = logoFileName
                    )
                    if (viewModel.updateCompanyProfile(updated)) {
                        Toast.makeText(context, "تم حفظ بيانات المنشأة محليًا", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GlassBlue),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "حفظ كافة التعديلات", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun getSettingsFieldColors() = OutlinedTextFieldDefaults.colors(
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
