package com.romoreno.malagapp.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.romoreno.malagapp.R
import com.romoreno.malagapp.data.database.dto.CameraWithDistrict
import com.romoreno.malagapp.data.database.entities.DistrictEntity
import com.romoreno.malagapp.data.network.mapper.UtmConverter
import com.romoreno.malagapp.databinding.FragmentCameraMapBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker

/**
 * Mapa con OpenStreetMap (osmdroid, sin API key). No carga ninguna camara hasta que se elige
 * un distrito (o "Todos") en el selector superior.
 */
@AndroidEntryPoint
class CameraMapFragment : Fragment() {

    private sealed interface Selection {
        data object None : Selection
        data object All : Selection
        data class Single(val districtId: Int) : Selection
    }

    private val viewModel: CameraMapViewModel by viewModels()

    private var _binding: FragmentCameraMapBinding? = null
    private val binding get() = _binding!!

    private var selection: Selection = Selection.None
    private var selectedPosition = 0
    private var spinnerDistricts: List<DistrictEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // osmdroid exige un user agent propio y una carpeta de cache donde podamos escribir
        MapConfig.init(requireContext())
        _binding = FragmentCameraMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            controller.setZoom(12.0)
            controller.setCenter(MALAGA)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.districts.collect { setupSpinner(it) } }
                launch { viewModel.cameras.collect { drawMarkers(zoomToResults = false) } }
            }
        }
    }

    private fun setupSpinner(districts: List<DistrictEntity>) {
        if (districts.isEmpty() || districts == spinnerDistricts) return
        spinnerDistricts = districts
        val names = listOf(getString(R.string.map_select_district)) +
                districts.map { it.name } +
                getString(R.string.map_all_districts)
        binding.spDistrict.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, names
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.spDistrict.setSelection(selectedPosition, false)
        binding.spDistrict.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, position: Int, id: Long) {
                if (position == selectedPosition && selection != Selection.None) return
                selectedPosition = position
                selection = when (position) {
                    0 -> Selection.None
                    names.lastIndex -> Selection.All
                    else -> Selection.Single(spinnerDistricts[position - 1].id)
                }
                drawMarkers(zoomToResults = true)
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun visibleCameras(): List<CameraWithDistrict> {
        val all = viewModel.cameras.value
        return when (val s = selection) {
            Selection.None -> emptyList()
            Selection.All -> all
            is Selection.Single -> all.filter { it.cameraEntity.productId == s.districtId }
        }
    }

    private fun drawMarkers(zoomToResults: Boolean) {
        val map = _binding?.mapView ?: return
        map.overlays.clear()
        val points = mutableListOf<GeoPoint>()
        visibleCameras().forEach { item ->
            val camera = item.cameraEntity
            val x = camera.latitude.toDoubleOrNull()
            val y = camera.longitude.toDoubleOrNull()
            if (x == null || y == null) return@forEach
            // En BBDD se guardan tal cual vienen del geojson: (x, y) en UTM
            val (lat, lon) = UtmConverter.toLatLng(x, y)
            val point = GeoPoint(lat, lon)
            points.add(point)
            val marker = Marker(map).apply {
                position = point
                title = camera.description
                snippet = camera.address.trim()
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                // Pin azul (favoritas en ambar); copia propia del icono para no teñir el compartido
                icon = icon?.constantState?.newDrawable()?.mutate()?.apply {
                    setTint(if (camera.favourite) FAVOURITE_COLOR else PIN_COLOR)
                }
                setOnMarkerClickListener { _, _ ->
                    CameraDialog.show(requireContext(), viewLifecycleOwner, camera)
                    true
                }
            }
            map.overlays.add(marker)
        }
        map.invalidate()

        if (zoomToResults && points.isNotEmpty()) {
            if (points.size == 1) {
                map.controller.setZoom(17.0)
                map.controller.setCenter(points[0])
            } else {
                val box = BoundingBox.fromGeoPoints(points)
                map.post { _binding?.mapView?.zoomToBoundingBox(box, true, 150) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        // Recarga por si se marcaron favoritos en la lista mientras el mapa estaba en segundo plano
        viewModel.loadCameras()
    }

    override fun onPause() {
        binding.mapView.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        binding.mapView.onDetach()
        spinnerDistricts = emptyList()
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        val MALAGA = GeoPoint(36.7213, -4.4214)
        val PIN_COLOR = Color.parseColor("#1976D2")
        val FAVOURITE_COLOR = Color.parseColor("#FFB300")
    }
}
