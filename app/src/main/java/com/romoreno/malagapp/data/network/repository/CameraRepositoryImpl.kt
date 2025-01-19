package com.romoreno.malagapp.data.network.repository

import com.romoreno.malagapp.data.database.entities.CameraEntity
import com.romoreno.malagapp.data.network.mapper.CameraMapper.toCameraEntity
import com.romoreno.malagapp.data.network.service.CameraApiService
import java.util.stream.Collectors
import javax.inject.Inject

class CameraRepositoryImpl @Inject constructor(private val cameraApiService: CameraApiService) : CameraRepository {

    override suspend fun getCameras(): List<CameraEntity> {
        return cameraApiService.getCameras().features
            ?.stream()
            ?.map { it.toCameraEntity() }
            ?.collect(Collectors.toList()) ?: emptyList()
    }

}