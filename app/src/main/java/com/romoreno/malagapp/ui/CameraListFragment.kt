package com.romoreno.malagapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.romoreno.malagapp.R
import com.romoreno.malagapp.data.database.dto.CameraWithDistrict
import com.romoreno.malagapp.data.database.entities.CameraEntity
import com.romoreno.malagapp.data.database.repository.DatabaseRepository
import com.romoreno.malagapp.databinding.FragmentCameraListBinding
import com.romoreno.malagapp.ui.adapter.CameraListAdapter
import com.romoreno.malagapp.ui.adapter.WhenCameraItemSelected
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@AndroidEntryPoint
class CameraListFragment : Fragment() {

    private val cameraListViewModel: CameraListViewModel by viewModels()

    @Inject
    lateinit var databaseRepository: DatabaseRepository

    private lateinit var cameraAdapter: CameraListAdapter

    private var _binding: FragmentCameraListBinding? = null
    private val binding get() = _binding!!

    private var idSeleccionado: Int? = null
    private var onlyFavourites = true // el selector empieza en "Favoritos"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        cameraListViewModel.searchCameras()
    }

    private fun initUI() {
        initUIState()
        initAdapter()
        initList()
        initListeners()
    }

    private fun initList() {
        cameraAdapter = CameraListAdapter(WhenCameraItemSelected(
            onCardViewSelected = { whenCameraItemSelected(it) },
            onStartSelected = { onStarSelected(it) },
            onEditAliasSelected = { showAliasDialog(it) }
        ))
        cameraAdapter.favouritesMode = onlyFavourites
        binding.rvCamera.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cameraAdapter
        }
    }

    private fun initListeners() {
        binding.svCameras.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                cameraListViewModel.searchCameras(idSeleccionado, query, onlyFavourites)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                cameraListViewModel.searchCameras(idSeleccionado, newText, onlyFavourites)
                return false
            }
        })
    }

    private fun onStarSelected(cameraEntity: CameraEntity) {
        cameraListViewModel.markCameraAsFavourite(cameraEntity)
        // Al marcar (no al desmarcar) se ofrece ponerle alias; en blanco no se toca nada
        if (!cameraEntity.favourite) {
            AliasDialog.show(requireContext(), cameraEntity, skipBlank = true) {
                cameraListViewModel.updateAlias(cameraEntity.id, it)
            }
        }
    }

    private fun showAliasDialog(camera: CameraEntity) {
        AliasDialog.show(requireContext(), camera) {
            cameraListViewModel.updateAlias(camera.id, it)
        }
    }

    private fun whenCameraItemSelected(camera: CameraWithDistrict) {
        CameraDialog.show(requireContext(), viewLifecycleOwner, camera.cameraEntity)
    }

    private fun initUIState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // collect no termina nunca: cada flujo necesita su propia corrutina
                launch {
                    cameraListViewModel.stateCamera.collect {
                        when (it) {
                            is CameraListState.Success -> successState(it.cameras)
                        }
                    }
                }
                launch {
                    cameraListViewModel.stateLoading.collect {
                        when (it) {
                            LoadingState.Loading -> binding.progressBar.isVisible = true
                            LoadingState.Loaded -> binding.progressBar.isVisible = false
                        }
                    }
                }
            }
        }
    }

    private fun initAdapter() {
        viewLifecycleOwner.lifecycleScope.launch {
            val districts = withContext(Dispatchers.IO) {
                databaseRepository.getDistrictList().sortedBy { it.id }.associate { it.name to it.id }
            }

            // Posicion 0 = Favoritos (por defecto), 1 = Todos, el resto = distritos
            val names = listOf(
                getString(R.string.filter_favourites),
                getString(R.string.filter_all)
            ) + districts.keys.toList()

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                names
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.comboBox.adapter = adapter

            binding.comboBox.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    onlyFavourites = position == 0
                    cameraAdapter.favouritesMode = onlyFavourites
                    idSeleccionado = if (position >= 2) districts[names[position]] else null
                    cameraListViewModel.searchCameras(
                        idSeleccionado,
                        binding.svCameras.query.toString(),
                        onlyFavourites
                    )
                    // Solo al cambiar de filtro; al marcar favorito la lista no debe saltar arriba
                    binding.rvCamera.scrollToPosition(0)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }
    }

    private fun successState(cameraList: List<CameraWithDistrict>) {
        val oldFirst = cameraAdapter.firstId()
        cameraAdapter.updateList(cameraList)
        // Los favoritos van primero (ORDER BY favourite DESC): si cambia la cabecera de la lista
        // (se marca/desmarca una), subimos para que se vea donde ha quedado
        if (oldFirst != cameraAdapter.firstId()) binding.rvCamera.scrollToPosition(0)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
