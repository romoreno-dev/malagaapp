package com.romoreno.malagapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romoreno.malagapp.data.database.dto.CameraWithDistrict
import com.romoreno.malagapp.data.database.entities.DistrictEntity
import com.romoreno.malagapp.data.database.repository.DatabaseRepository
import com.romoreno.malagapp.data.network.repository.CameraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CameraMapViewModel @Inject constructor(
    private val cameraRepository: CameraRepository,
    private val databaseRepository: DatabaseRepository
) : ViewModel() {

    private val _cameras = MutableStateFlow<List<CameraWithDistrict>>(emptyList())
    val cameras: StateFlow<List<CameraWithDistrict>> = _cameras

    private val _districts = MutableStateFlow<List<DistrictEntity>>(emptyList())
    val districts: StateFlow<List<DistrictEntity>> = _districts

    fun loadCameras() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    var list = databaseRepository.getAllCameraListWithDistrict()
                    if (list.isEmpty()) {
                        databaseRepository.insertCamera(cameraRepository.getCameras())
                        list = databaseRepository.getAllCameraListWithDistrict()
                    }
                    _districts.value = databaseRepository.getDistrictList().sortedBy { it.id }
                    _cameras.value = list
                } catch (e: Exception) {
                    // Sin red y sin datos: el mapa se queda sin marcadores
                }
            }
        }
    }
}
