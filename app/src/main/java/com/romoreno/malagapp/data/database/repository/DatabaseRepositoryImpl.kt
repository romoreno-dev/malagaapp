package com.romoreno.malagapp.data.database.repository

import com.romoreno.malagapp.data.database.dao.CameraDao
import com.romoreno.malagapp.data.database.dto.CameraWithDistrict
import com.romoreno.malagapp.data.database.entities.CameraEntity
import com.romoreno.malagapp.data.database.entities.DistrictEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DatabaseRepositoryImpl @Inject constructor(private val cameraDao: CameraDao) : DatabaseRepository {

    override fun getCameraListByDistrictNumberAndKeyword(districtNumber: Int?, keyword: String?): Flow<List<CameraWithDistrict>> {
        if (districtNumber != null && !keyword.isNullOrBlank()) {
            return cameraDao.getCameraListByDistrictNumberAndKeyword(districtNumber, "%${keyword}%")
        } else if (!keyword.isNullOrBlank()) {
            return cameraDao.getCameraListByKeyword("%${keyword}%")
        } else if (districtNumber != null) {
            return cameraDao.getCameraListByDistrictNumber(districtNumber)
        } else {
            return cameraDao.getAllCameraList()
        }
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

    override suspend fun markCameraAsFavourite(camera: CameraEntity) {
        val favourite = camera.favourite
        val camera = camera.copy(favourite = !favourite)
        cameraDao.updateCamera(camera)
        }
}