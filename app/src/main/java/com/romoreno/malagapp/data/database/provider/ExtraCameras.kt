package com.romoreno.malagapp.data.database.provider

import com.romoreno.malagapp.data.database.entities.CameraEntity

/**
 * Camaras que no vienen de los datos abiertos del Ayuntamiento, anadidas a mano. Se insertan si
 * faltan (sin pisar favorito ni alias de las que ya existen). Ids por encima de 900000 para no
 * chocar con los del Ayuntamiento.
 */
object ExtraCameras {

    /**
     * Estacion meteorologica EA7CL en Nueva Malaga (http://nuevamalaga.duckdns.org/), con camara
     * publica en Bailen-Miraflores (distrito 4).
     *
     * Ojo: igual que las del Ayuntamiento, "latitude" guarda la primera coordenada del par (x) y
     * "longitude" la segunda (y). Aqui van en grados, asi que x = longitud e y = latitud; el mapa
     * lo interpreta por el rango (ver UtmConverter.toLatLng). Las coordenadas salen del marcador
     * del mapa embebido en la pagina de la estacion.
     */
    /** Camaras que ademas tienen historico de imagenes: id -> carpeta base en el servidor. */
    val HISTORY_BASE_URL = mapOf(
        900001 to "http://nuevamalaga.duckdns.org/captura/copias"
    )

    val ALL = listOf(
        CameraEntity(
            id = 900001,
            productId = 4,
            name = "EA7CL - NUEVA MÁLAGA",
            description = "Nueva Málaga (estación EA7CL)",
            address = "Nueva Málaga, Bailén-Miraflores",
            url = "http://nuevamalaga.duckdns.org/captura/last.jpg",
            latitude = "-4.444",
            longitude = "36.726",
            favourite = false
        )
    )
}
