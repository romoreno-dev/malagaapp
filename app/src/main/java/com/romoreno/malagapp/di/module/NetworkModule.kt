package com.romoreno.malagapp.di.module

import com.romoreno.malagapp.data.network.config.ServiceNames
import com.romoreno.malagapp.data.network.repository.CameraRepository
import com.romoreno.malagapp.data.network.repository.CameraRepositoryImpl
import com.romoreno.malagapp.data.network.service.CameraApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideRetrofitBuilder(): Retrofit.Builder {
        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        // Interceptor para imprimir en el log la request y la response
        // (Lo comento porque solo lo necesito cuando hago debug)
        val interceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)

        return OkHttpClient
            .Builder()
            .addInterceptor(interceptor)
            .build()
    }

    @Provides
    fun provideCameraApiService(
        builder: Retrofit.Builder,
        okHttpClient: OkHttpClient
    ): CameraApiService {
        return builder
            .baseUrl(ServiceNames.CAMERA_BASE_URL)
            .client(okHttpClient)
            .build()
            .create(CameraApiService::class.java)
    }

    @Provides
    fun provideCameraRepository(cameraApiService: CameraApiService): CameraRepository =
        CameraRepositoryImpl(cameraApiService)


}