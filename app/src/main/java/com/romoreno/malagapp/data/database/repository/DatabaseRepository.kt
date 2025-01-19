package com.romoreno.malagapp.data.database.repository

import com.romoreno.malagapp.data.database.entities.CameraEntity
import com.romoreno.malagapp.data.database.entities.DistrictEntity

interface DatabaseRepository {

    suspend fun getCameraListByDistrictNumber(districtNumber: Int): List<CameraEntity>
    suspend fun getAllCameraList(): List<CameraEntity>
    suspend fun getDistrictList(): List<DistrictEntity>
    suspend fun insertCamera(entity: CameraEntity)
    suspend fun insertCamera(entity: List<CameraEntity>?)

}