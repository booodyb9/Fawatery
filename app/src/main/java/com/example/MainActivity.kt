package com.example

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.navigation.FawratyApp
import com.example.ui.theme.FawratyTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set application locale and layout direction to Arabic by default
        val arabicLocale = Locale("ar")
        Locale.setDefault(arabicLocale)
        val config = Configuration(resources.configuration).apply {
            setLocale(arabicLocale)
            setLayoutDirection(arabicLocale)
        }
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        enableEdgeToEdge()
        setContent {
            FawratyTheme {
                FawratyApp()
            }
        }
    }
}
