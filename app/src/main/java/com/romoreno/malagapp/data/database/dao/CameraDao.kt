package com.romoreno.malagapp.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.romoreno.malagapp.data.database.dto.CameraWithDistrict
import com.romoreno.malagapp.data.database.entities.CameraEntity
import com.romoreno.malagapp.data.database.entities.DistrictEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CameraDao {

    @Query("SELECT * FROM camera WHERE district_id = :districtNumber ORDER BY favourite DESC, district_id, name")
    fun getCameraListByDistrictNumber(districtNumber: Int): Flow<List<CameraWithDistrict>>

    @Query("SELECT * FROM camera WHERE (name LIKE :keyword OR description LIKE :keyword OR address LIKE :keyword) ORDER BY favourite DESC, district_id, name")
    fun getCameraListByKeyword(keyword: String): Flow<List<CameraWithDistrict>>

    @Query("SELECT * FROM camera WHERE district_id = :districtNumber AND (name LIKE :keyword OR description LIKE :keyword OR address LIKE :keyword) ORDER BY favourite DESC, district_id, address")
    fun getCameraListByDistrictNumberAndKeyword(districtNumber: Int, keyword: String): Flow<List<CameraWithDistrict>>

    @Query("SELECT * FROM camera ORDER BY favourite DESC, district_id, name")
    fun getAllCameraList(): Flow<List<CameraWithDistrict>>

    @Query("SELECT * FROM camera ORDER BY favourite DESC, district_id, name")
    suspend fun getAllCameraListWithDistrict(): List<CameraWithDistrict>

    @Query("SELECT * FROM district")
    suspend fun getDistrictList(): List<DistrictEntity>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCamera(entity: CameraEntity): Long

    @Transaction
    @Update
    suspend fun updateCamera(entity: CameraEntity)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCamera(entity: List<CameraEntity>)

}