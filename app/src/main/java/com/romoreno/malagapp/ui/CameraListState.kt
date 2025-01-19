package com.romoreno.malagapp.ui

import com.romoreno.malagapp.data.database.entities.CameraEntity

sealed class CameraListState {

    data class Success(val cameras: List<CameraEntity>) : CameraListState()

}