package com.romoreno.malagapp.ui

import android.content.Context
import org.osmdroid.config.Configuration

/** Configuracion comun de osmdroid: user agent propio y cache donde la app puede escribir. */
object MapConfig {
    fun init(context: Context) {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = context.cacheDir
            osmdroidTileCache = context.cacheDir.resolve("osmdroid_tiles")
        }
    }
}
