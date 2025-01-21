package com.romoreno.malagapp.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "camera",
    foreignKeys = [ForeignKey(
        entity = DistrictEntity::class,
        parentColumns = ["id"],
        childColumns = ["district_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["id"])]
)
data class CameraEntity (
    @PrimaryKey
    @ColumnInfo(name = "id") val id : Int,
    @ColumnInfo(name = "district_id") val productId: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "latitude") val latitude: String,
    @ColumnInfo(name = "longitude") val longitude: String,
    @ColumnInfo(name = "favourite") val favourite: Boolean
)