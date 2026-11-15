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

enum class GuessMode { DISTRICT, STREET }

data class GuessOption(val key: String, val label: String)

data class GuessRound(
    val camera: CameraWithDistrict,
    val options: List<GuessOption>,
    val correctKey: String,
    val selectedKey: String? = null
) {
    val answered: Boolean get() = selectedKey != null
    val correctLabel: String get() = options.firstOrNull { it.key == correctKey }?.label ?: ""
}

data class GuessState(
    val mode: GuessMode = GuessMode.DISTRICT,
    val round: GuessRound? = null,
    val hits: Int = 0,
    val total: Int = 0,
    val empty: Boolean = false
)

@HiltViewModel
class CameraGuessViewModel @Inject constructor(
    private val cameraRepository: CameraRepository,
    private val databaseRepository: DatabaseRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GuessState())
    val state: StateFlow<GuessState> = _state

    private var cameras: List<CameraWithDistrict> = emptyList()
    private var districts: List<DistrictEntity> = emptyList()

    fun start() {
        if (_state.value.round != null) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    var list = databaseRepository.getAllCameraListWithDistrict()
                    if (list.isEmpty()) {
                        databaseRepository.insertCamera(cameraRepository.getCameras())
                    }
                    databaseRepository.ensureExtraCameras()
                    list = databaseRepository.getAllCameraListWithDistrict()
                    cameras = list
                    districts = databaseRepository.getDistrictList()
                } catch (e: Exception) {
                    cameras = emptyList()
                }
            }
            if (cameras.isEmpty()) _state.value = _state.value.copy(empty = true) else nextRound()
        }
    }

    /** Cambia entre jugar por distrito o por calle. Reinicia el marcador. */
    fun setMode(mode: GuessMode) {
        if (_state.value.mode == mode) return
        _state.value = GuessState(mode = mode, empty = cameras.isEmpty())
        nextRound()
    }

    fun nextRound() {
        if (cameras.isEmpty()) return
        val camera = cameras.random()
        val mode = _state.value.mode
        val (options, correctKey) = when (mode) {
            GuessMode.DISTRICT -> districtOptions(camera)
            GuessMode.STREET -> streetOptions(camera)
        }
        _state.value = _state.value.copy(
            round = GuessRound(camera, options, correctKey),
            empty = false
        )
    }

    private fun districtOptions(camera: CameraWithDistrict): Pair<List<GuessOption>, String> {
        val correctId = camera.cameraEntity.productId
        val correct = districts.firstOrNull { it.id == correctId } ?: camera.district
        val distractors = districts.filter { it.id != correctId }.shuffled().take(3)
        val options = (distractors + listOfNotNull(correct)).shuffled()
            .map { GuessOption(it.id.toString(), it.name) }
        return options to correctId.toString()
    }

    private fun streetOptions(camera: CameraWithDistrict): Pair<List<GuessOption>, String> {
        val correctName = camera.cameraEntity.description.trim()
        val distractors = cameras
            .map { it.cameraEntity.description.trim() }
            .filter { it.isNotEmpty() && it != correctName }
            .distinct()
            .shuffled()
            .take(3)
        val options = (distractors + correctName).shuffled().map { GuessOption(it, it) }
        return options to correctName
    }

    fun answer(key: String) {
        val current = _state.value
        val round = current.round ?: return
        if (round.answered) return
        _state.value = current.copy(
            round = round.copy(selectedKey = key),
            hits = current.hits + if (key == round.correctKey) 1 else 0,
            total = current.total + 1
        )
    }
}
