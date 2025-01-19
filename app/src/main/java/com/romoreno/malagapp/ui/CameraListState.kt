package com.romoreno.malagapp.ui

import com.romoreno.malagapp.data.database.dto.CameraWithDistrict

sealed class CameraListState {

    data class Success(val cameras: List<CameraWithDistrict>) : CameraListState()

}