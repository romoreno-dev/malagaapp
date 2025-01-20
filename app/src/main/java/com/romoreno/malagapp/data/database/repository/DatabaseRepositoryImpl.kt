package com.romoreno.malagapp.data.database.repository

import com.romoreno.malagapp.data.database.dao.CameraDao
import com.romoreno.malagapp.data.database.dto.CameraWithDistrict
import com.romoreno.malagapp.data.database.entities.CameraEntity
import com.romoreno.malagapp.data.database.entities.DistrictEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DatabaseRepositoryImpl @Inject constructor(private val cameraDao: CameraDao) : DatabaseRepository {

    override fun getCameraListByDistrictNumber(districtNumber: Int): Flow<List<CameraWithDistrict>> {
        return cameraDao.getCameraListByDistrictNumber(districtNumber)
    }

    override suspend fun getAllCameraList(): List<CameraEntity> {
        return cameraDao.getAllCameraList()
    }

    override suspend fun getAllCameraListWithDistrict(): List<CameraWithDistrict> {
        return cameraDao.getAllCameraListWithDistrict()
    }

    override suspend fun getDistrictList(): List<DistrictEntity> {
        return cameraDao.getDistrictList()
    }

    override suspend fun insertCamera(entity: CameraEntity) {
        cameraDao.insertCamera(entity)
    }

    override suspend fun insertCamera(entity: List<CameraEntity>?) {
        if (entity != null) {
            cameraDao.insertCamera(entity)
        }
    }
}