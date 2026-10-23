package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.branding.CompanyLogoStore

@Composable
fun CompanyLogo(fileName: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val logo = remember(context, fileName) {
        runCatching { CompanyLogoStore.load(context, fileName).asImageBitmap() }.getOrNull()
    }
    if (logo != null) {
        Image(bitmap = logo, contentDescription = "شعار الشركة", modifier = modifier, contentScale = ContentScale.Fit)
    } else {
        Text("الشعار غير متاح", modifier = modifier)
    }
}
