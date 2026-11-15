package com.romoreno.malagapp.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.romoreno.malagapp.data.database.dao.CameraDao
import com.romoreno.malagapp.data.database.entities.CameraEntity
import com.romoreno.malagapp.data.database.entities.DistrictEntity

@Database(entities = [DistrictEntity::class, CameraEntity::class], version = 2, exportSchema = false)
abstract class ApplicationDatabase : RoomDatabase() {

    abstract fun getCameraDao(): CameraDao

    companion object {
        /** v1 -> v2: alias opcional por camara. Solo anade la columna, no toca los datos. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE camera ADD COLUMN alias TEXT")
            }
        }
    }

}