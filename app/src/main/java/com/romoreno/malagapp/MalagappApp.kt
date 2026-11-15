package com.romoreno.malagapp

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MalagappApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // La app siempre va en modo noche, independientemente del ajuste del sistema
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
}
