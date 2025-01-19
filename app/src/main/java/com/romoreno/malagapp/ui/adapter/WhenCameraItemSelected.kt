package com.romoreno.malagapp.ui.adapter

import com.romoreno.malagapp.data.database.entities.CameraEntity

data class WhenCameraItemSelected(val onCardViewSelected: (CameraEntity) -> Unit)