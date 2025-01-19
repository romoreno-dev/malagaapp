package com.romoreno.malagapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romoreno.malagapp.data.database.dto.CameraWithDistrict
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
class CameraListViewModel @Inject constructor(private val cameraRepository: CameraRepository,
    private val databaseRepository: DatabaseRepository) :
    ViewModel() {

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
            val cameraList = withContext(Dispatchers.IO) {
                var cameraList = databaseRepository.getAllCameraListWithDistrict()
                if (cameraList.isEmpty()) {
                    val camera = cameraRepository.getCameras()
                    databaseRepository.insertCamera(camera)
                }
                databaseRepository.getAllCameraListWithDistrict()
            }
            _stateLoading.value = LoadingState.Loaded
            _stateCamera.value = CameraListState.Success(cameraList)
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

}