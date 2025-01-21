package com.romoreno.malagapp.data.network.mapper

import com.romoreno.malagapp.data.database.entities.CameraEntity
import com.romoreno.malagapp.data.network.pojo.Feature

object CameraMapper {

    fun Feature.toCameraEntity(): CameraEntity {
        return CameraEntity(
            properties?.id!!,
            getDistrictNumber(properties?.url),
            properties?.nombre ?: "",
            properties?.descripcion ?: "",
            properties?.direccion ?: "",
            properties?.urlImagen ?: "",
            geometry?.coordinates?.get(0).toString(),
            geometry?.coordinates?.get(1).toString(),
            false
        )
    }

    private fun getDistrictNumber(url: String?): Int {
        if (url == null) {
            return 1
        }
        val regex = Regex("distrito-(\\d+)")
        val matchResult = regex.find(url)

        return matchResult?.groups?.get(1)?.value?.toIntOrNull() ?: 1
    }
}