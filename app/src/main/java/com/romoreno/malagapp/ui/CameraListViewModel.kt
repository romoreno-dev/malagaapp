package com.romoreno.malagapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romoreno.malagapp.data.database.entities.CameraEntity
import com.romoreno.malagapp.data.database.repository.DatabaseRepository
import com.romoreno.malagapp.data.network.repository.CameraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CameraListViewModel @Inject constructor(
    private val cameraRepository: CameraRepository,
    private val databaseRepository: DatabaseRepository
) :
    ViewModel() {

    private var searchJob: Job? = null

    private var _stateCamera = MutableStateFlow<CameraListState>(
        CameraListState
            .Success(emptyList())
    )
    val stateCamera: StateFlow<CameraListState> = _stateCamera

    private var _stateDistrict = MutableStateFlow<DistrictListState>(
        DistrictListState
            .Success(emptyList())
    )
    val stateDistrict: StateFlow<DistrictListState> = _stateDistrict


    private var _stateLoading = MutableStateFlow<LoadingState>(
        LoadingState.Loaded
    )
    val stateLoading: StateFlow<LoadingState> = _stateLoading

    fun searchCameras() {
        viewModelScope.launch {
            _stateLoading.value = LoadingState.Loading
            try {
                withContext(Dispatchers.IO) {
                    if (databaseRepository.getAllCameraListWithDistrict().isEmpty()) {
                        databaseRepository.insertCamera(cameraRepository.getCameras())
                    }
                }
            } catch (e: Exception) {
                // Sin red: se queda la lista vacia hasta el siguiente intento
            } finally {
                _stateLoading.value = LoadingState.Loaded
            }
        }
    }

    fun searchCameras(districtNumber: Int?, keyword: String?, onlyFavourites: Boolean = false) {
        // Cancelamos la busqueda anterior: si no, cada cambio de filtro dejaba un colector vivo
        // que pisaba la lista (y rompia el refresco al marcar favoritos)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            databaseRepository.getCameraListByDistrictNumberAndKeyword(districtNumber, keyword)
                .map { list -> if (onlyFavourites) list.filter { it.cameraEntity.favourite } else list }
                .collect { p ->
                    _stateLoading.value = LoadingState.Loaded
                    _stateCamera.value = CameraListState.Success(p)
                }
        }
    }

    fun searchDistricts() {
        viewModelScope.launch {
            _stateLoading.value = LoadingState.Loading
            val districtList = withContext(Dispatchers.IO) {
                databaseRepository.getDistrictList()
            }
            _stateLoading.value = LoadingState.Loaded
            _stateDistrict.value = DistrictListState.Success(districtList)
        }
    }

    fun updateAlias(cameraId: Int, alias: String?) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { databaseRepository.updateAlias(cameraId, alias) }
        }
    }

    fun markCameraAsFavourite(cameraEntity: CameraEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                databaseRepository.markCameraAsFavourite(cameraEntity)
            }
        }
    }

}