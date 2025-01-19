package com.romoreno.malagapp.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.romoreno.malagapp.data.database.dto.CameraWithDistrict
import com.romoreno.malagapp.data.database.entities.CameraEntity
import com.romoreno.malagapp.data.database.entities.DistrictEntity

@Dao
interface CameraDao {

    @Query("SELECT * FROM camera WHERE district_id = :districtNumber")
    suspend fun getCameraListByDistrictNumber(districtNumber: Int): List<CameraEntity>

    @Query("SELECT * FROM camera ORDER BY district_id")
    suspend fun getAllCameraList(): List<CameraEntity>

    @Query("SELECT * FROM camera ORDER BY district_id")
    suspend fun getAllCameraListWithDistrict(): List<CameraWithDistrict>

    @Query("SELECT * FROM district")
    suspend fun getDistrictList(): List<DistrictEntity>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCamera(entity: CameraEntity): Long

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCamera(entity: List<CameraEntity>)

}