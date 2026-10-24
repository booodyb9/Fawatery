package com.example

import android.app.Application
import android.content.Context

class FawratyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }
    companion object {
        lateinit var appContext: Context
            private set
    }
}
