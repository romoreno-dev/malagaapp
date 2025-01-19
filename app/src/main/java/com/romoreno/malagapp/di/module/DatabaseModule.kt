package com.romoreno.malagapp.di.module

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.romoreno.malagapp.data.database.ApplicationDatabase
import com.romoreno.malagapp.data.database.dao.CameraDao
import com.romoreno.malagapp.data.database.provider.District
import com.romoreno.malagapp.data.database.repository.DatabaseRepository
import com.romoreno.malagapp.data.database.repository.DatabaseRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val MALAGA_APP_DATABASE = "malaga_app_db"

    @Singleton
    @Provides
    fun providesMalagappDatabase(@ApplicationContext context: Context) =
        Room.databaseBuilder(context, ApplicationDatabase::class.java, MALAGA_APP_DATABASE)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // En la primera ejecucion lanzamos una corrutina para llenar la BBDD
                    // con los supermercados disponibles en la aplicacion
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDatabase(db)
                    }
                }
            }).build()

    private fun populateDatabase(db: SupportSQLiteDatabase) {
        District.entries
            .forEach { db.execSQL(buildInsertDistrictSql(it)) }
    }

    private fun buildInsertDistrictSql(district: District) = "INSERT INTO district (id, name) VALUES ('${district.id}', '${district.districtName}')"

    @Singleton
    @Provides
    fun providesCameraDao(db: ApplicationDatabase) = db.getCameraDao()

    @Singleton
    @Provides
    fun provideDatabaseRepository(cameraDao: CameraDao): DatabaseRepository = DatabaseRepositoryImpl(cameraDao)

}