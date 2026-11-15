package com.romoreno.malagapp

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp
import java.io.File

@HiltAndroidApp
class MalagappApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // La app siempre va en modo noche, independientemente del ajuste del sistema
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        clearTemporaryFiles()
    }

    /**
     * Los videos y fotos temporales (cache/shared) se borran al usarlos, pero si la app se cierra
     * o muere a mitad de una grabacion o de una exportacion podria quedar algun resto: se limpia
     * al arrancar para no dejar nada huerfano.
     */
    private fun clearTemporaryFiles() {
        try {
            File(cacheDir, "shared").listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {
        }
    }
}
