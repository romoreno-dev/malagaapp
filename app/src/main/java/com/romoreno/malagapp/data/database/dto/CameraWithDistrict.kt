package com.romoreno.malagapp.data.database.dto

import androidx.room.Embedded
import androidx.room.Relation
import com.romoreno.malagapp.data.database.entities.CameraEntity
import com.romoreno.malagapp.data.database.entities.DistrictEntity

data class CameraWithDistrict(
    @Embedded
    val cameraEntity: CameraEntity,
    @Relation(
        entity = DistrictEntity::class,
        parentColumn = "district_id",
        entityColumn = "id"
    )
    val district: DistrictEntity? = null
)
