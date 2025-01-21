package com.romoreno.malagapp.ui.adapter

import com.romoreno.malagapp.data.database.dto.CameraWithDistrict
import com.romoreno.malagapp.data.database.entities.CameraEntity

data class WhenCameraItemSelected(val onCardViewSelected: (CameraWithDistrict) -> Unit,
    val onStartSelected: (CameraEntity) -> Unit)