package com.romoreno.malagapp.data.database.repository

import com.romoreno.malagapp.data.database.dto.CameraWithDistrict
import com.romoreno.malagapp.data.database.entities.CameraEntity
import com.romoreno.malagapp.data.database.entities.DistrictEntity
import kotlinx.coroutines.flow.Flow

interface DatabaseRepository {

    fun getCameraListByDistrictNumber(districtNumber: Int): Flow<List<CameraWithDistrict>>
    suspend fun getAllCameraList(): List<CameraEntity>
    suspend fun getAllCameraListWithDistrict(): List<CameraWithDistrict>
    suspend fun getDistrictList(): List<DistrictEntity>
    suspend fun insertCamera(entity: CameraEntity)
    suspend fun insertCamera(entity: List<CameraEntity>?)

}