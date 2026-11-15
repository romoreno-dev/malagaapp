package com.romoreno.malagapp.ui

import android.content.Context
import org.osmdroid.config.Configuration
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay

/** Configuracion comun de osmdroid: user agent propio y cache donde la app puede escribir. */
object MapConfig {
    fun init(context: Context) {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = context.cacheDir
            osmdroidTileCache = context.cacheDir.resolve("osmdroid_tiles")
        }
    }

    /** Mapa oscuro para casar con el modo noche: invierte los colores de los tiles de OSM. */
    fun applyDarkTiles(map: MapView) {
        map.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
    }
}
