package com.romoreno.malagapp.data.network.service

import com.romoreno.malagapp.data.network.config.ServiceNames
import com.romoreno.malagapp.data.network.pojo.Root
import retrofit2.http.GET

interface CameraApiService {
    @GET(ServiceNames.CAMERA_PATH)
    suspend fun getCameras() : Root
}