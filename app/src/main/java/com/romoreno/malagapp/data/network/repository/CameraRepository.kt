package com.romoreno.malagapp.data.network.repository

import com.romoreno.malagapp.data.database.entities.CameraEntity

fun interface CameraRepository {

    suspend fun getCameras(): List<CameraEntity>

}