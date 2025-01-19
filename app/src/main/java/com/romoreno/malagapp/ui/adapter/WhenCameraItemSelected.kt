package com.romoreno.malagapp.ui.adapter

import com.romoreno.malagapp.data.database.dto.CameraWithDistrict

data class WhenCameraItemSelected(val onCardViewSelected: (CameraWithDistrict) -> Unit)