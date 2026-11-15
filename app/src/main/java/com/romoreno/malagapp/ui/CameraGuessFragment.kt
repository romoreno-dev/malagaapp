package com.romoreno.malagapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.annotation.SuppressLint
import android.graphics.Color
import android.view.MotionEvent
import com.google.android.material.button.MaterialButton
import com.romoreno.malagapp.data.network.mapper.UtmConverter
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import com.romoreno.malagapp.R
import com.romoreno.malagapp.databinding.FragmentCameraGuessBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CameraGuessFragment : Fragment() {

    private val viewModel: CameraGuessViewModel by viewModels()

    private var _binding: FragmentCameraGuessBinding? = null
    private val binding get() = _binding!!

    private var shownCameraId: Int? = null
    private var mapCameraId: Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnNext.setOnClickListener { viewModel.nextRound() }
        setupMap()
        binding.modeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                viewModel.setMode(
                    if (checkedId == R.id.btnModeStreet) GuessMode.STREET else GuessMode.DISTRICT
                )
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { render(it) } }
                // La foto se va actualizando sola mientras se juega
                launch {
                    while (isActive) {
                        delay(REFRESH_MILLIS)
                        viewModel.state.value.round?.let {
                            CameraDialog.loadPhoto(
                                requireContext(), it.camera.cameraEntity.url, binding.ivGuessPhoto
                            )
                        }
                    }
                }
            }
        }
        viewModel.start()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupMap() {
        binding.guessMap.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            // El mapa esta dentro de un ScrollView: que el gesto sobre el mapa no lo desplace
            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                        v.parent.requestDisallowInterceptTouchEvent(true)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        v.parent.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
        }
    }

    /** Al resolver: nombre de la camara y su ubicacion en un mapa pequeño. */
    private fun showAnswerInfo(camera: com.romoreno.malagapp.data.database.entities.CameraEntity) {
        binding.answerInfo.isVisible = true
        binding.tvCameraInfo.text = buildString {
            append(getString(R.string.guess_camera_is)).append('\n')
            if (!camera.alias.isNullOrBlank()) append(camera.alias).append(" · ")
            append(camera.description).append('\n').append(camera.address.trim())
        }
        if (mapCameraId == camera.id) return
        mapCameraId = camera.id
        val x = camera.latitude.toDoubleOrNull()
        val y = camera.longitude.toDoubleOrNull()
        if (x == null || y == null) return
        val (lat, lon) = UtmConverter.toLatLng(x, y)
        val point = GeoPoint(lat, lon)
        val map = binding.guessMap
        map.overlays.clear()
        map.overlays.add(Marker(map).apply {
            position = point
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = icon?.constantState?.newDrawable()?.mutate()?.apply {
                setTint(Color.parseColor("#1976D2"))
            }
            setOnMarkerClickListener { _, _ -> true }
        })
        map.controller.setZoom(16.0)
        map.controller.setCenter(point)
        map.invalidate()
    }

    private fun render(state: GuessState) {
        binding.tvEmpty.isVisible = state.empty
        val round = state.round
        binding.guessContent.isVisible = round != null && !state.empty
        if (round == null) return

        binding.tvScore.text = getString(R.string.guess_score, state.hits, state.total)
        binding.tvQuestion.setText(
            if (state.mode == GuessMode.DISTRICT) R.string.guess_question
            else R.string.guess_question_street
        )
        val modeButton =
            if (state.mode == GuessMode.DISTRICT) R.id.btnModeDistrict else R.id.btnModeStreet
        if (binding.modeGroup.checkedButtonId != modeButton) binding.modeGroup.check(modeButton)

        // Solo cambiamos de foto al cambiar de camara, no al responder
        val camera = round.camera.cameraEntity
        if (shownCameraId != camera.id) {
            shownCameraId = camera.id
            binding.ivGuessPhoto.setImageDrawable(null)
            CameraDialog.loadPhoto(requireContext(), camera.url, binding.ivGuessPhoto)
        }

        binding.optionsContainer.removeAllViews()
        round.options.forEach { option ->
            val button = MaterialButton(requireContext()).apply {
                text = option.label
                isAllCaps = false
                isEnabled = !round.answered
                if (round.answered) {
                    when (option.key) {
                        round.correctKey -> setBackgroundColor(Color.parseColor("#2E7D32"))
                        round.selectedKey -> setBackgroundColor(Color.parseColor("#C62828"))
                    }
                    setTextColor(Color.WHITE)
                }
                setOnClickListener { viewModel.answer(option.key) }
            }
            binding.optionsContainer.addView(
                button,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        binding.btnNext.isEnabled = round.answered
        if (round.answered) showAnswerInfo(camera) else {
            binding.answerInfo.isVisible = false
            mapCameraId = null
        }
        binding.tvResult.text = when {
            !round.answered -> ""
            round.selectedKey == round.correctKey -> getString(R.string.guess_correct)
            else -> getString(R.string.guess_wrong, round.correctLabel)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        MapConfig.init(requireContext())
        _binding = FragmentCameraGuessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        binding.guessMap.onResume()
    }

    override fun onPause() {
        binding.guessMap.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        binding.guessMap.onDetach()
        super.onDestroyView()
        _binding = null
        shownCameraId = null
        mapCameraId = null
    }

    private companion object {
        const val REFRESH_MILLIS = 5000L
    }
}
