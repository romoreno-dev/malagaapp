package com.romoreno.malagapp.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.romoreno.malagapp.data.database.dao.CameraDao
import com.romoreno.malagapp.data.database.entities.CameraEntity
import com.romoreno.malagapp.data.database.entities.DistrictEntity

@Database(entities = [DistrictEntity::class, CameraEntity::class], version = 1, exportSchema = false)
abstract class ApplicationDatabase : RoomDatabase() {

    abstract fun getCameraDao(): CameraDao

}