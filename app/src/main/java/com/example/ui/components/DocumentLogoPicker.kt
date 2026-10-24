package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.branding.CompanyLogoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DocumentLogoPicker(
    fileName: String?,
    companyLogo: String?,
    onLogoChanged: (String?) -> Unit,
    onBusyChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            onBusyChanged(true)
            try {
                val saved = withContext(Dispatchers.IO) { CompanyLogoStore.importFromUri(context, uri) }
                onLogoChanged(saved)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Toast.makeText(context, error.message ?: "تعذر اختيار الشعار", Toast.LENGTH_LONG).show()
            } finally {
                busy = false
                onBusyChanged(false)
            }
        }
    }
    Column(Modifier.fillMaxWidth().padding(16.dp).testTag("document_logo_picker")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CompanyLogo(fileName, Modifier.size(64.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("شعار الشركة في هذا المستند", style = MaterialTheme.typography.titleSmall)
                Text("يظهر في المعاينة وPDF عند الحفظ.", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(onClick = { picker.launch("image/*") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text(if (busy) "جارٍ تجهيز الشعار…" else "اختيار شعار من الصور")
        }
        if (fileName != companyLogo) {
            TextButton(onClick = { onLogoChanged(companyLogo) }, enabled = !busy) { Text("استخدام شعار المنشأة") }
        }
    }
}
