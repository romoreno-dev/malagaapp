package com.romoreno.malagapp.ui

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.textview.MaterialTextView
import com.romoreno.malagapp.R
import com.romoreno.malagapp.data.database.dto.CameraWithDistrict
import com.romoreno.malagapp.data.database.entities.CameraEntity
import com.romoreno.malagapp.data.database.entities.DistrictEntity
import com.romoreno.malagapp.data.database.repository.DatabaseRepository
import com.romoreno.malagapp.databinding.FragmentCameraListBinding
import com.romoreno.malagapp.ui.adapter.CameraListAdapter
import com.romoreno.malagapp.ui.adapter.WhenCameraItemSelected
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        cameraListViewModel.searchDistricts()
       // cameraListViewModel.searchCameras()
    }

    private fun initUI() {
        initUIState()
        initAdapter()
        initList()
        initListeners()
    }

    private fun initList() {
        cameraAdapter = CameraListAdapter(WhenCameraItemSelected(onCardViewSelected = {
            whenCameraItemSelected(it)}, onStartSelected = {onStarSelected(it)}
        ))
        binding.rvCamera.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cameraAdapter
        }
    }

    private fun initListeners() {
        binding.svCameras.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                cameraListViewModel.searchCameras(idSeleccionado, query)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                cameraListViewModel.searchCameras(idSeleccionado, newText)
                return false
            }
        })
    }

    private fun sendImage(cameraEntity: CameraEntity) {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "${cameraEntity.description} \n ${cameraEntity.url} \n\n ${cameraEntity.address} \n "
            )
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(intent, getString(R.string.share))
        startActivity(shareIntent)
    }

    private fun onStarSelected(cameraEntity: CameraEntity) {
        cameraListViewModel.markCameraAsFavourite(cameraEntity)
    }

    private fun whenCameraItemSelected(cameraEntity: CameraWithDistrict) {
        var dialogVisible = true
        var recargarFoto = false
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_camera)
        val photoView = dialog.findViewById<ImageView>(R.id.photoView)
        val title = dialog.findViewById<MaterialTextView>(R.id.etTitle)

        title.text = cameraEntity.cameraEntity.description + " \n" + recargarFoto

        title.setOnClickListener() {
            recargarFoto = !recargarFoto
            title.text = cameraEntity.cameraEntity.description + " \n" + recargarFoto

            lifecycleScope.launch {
                while (dialogVisible && recargarFoto) {
                    loadPhoto(cameraEntity.cameraEntity.url, photoView)
                    delay(5000)
                }
            }
        }

        photoView.setOnLongClickListener {
            sendImage(cameraEntity.cameraEntity)
            return@setOnLongClickListener true
        }

        dialog.setOnDismissListener {
            dialogVisible = false
            recargarFoto = false
        }

        loadPhoto(cameraEntity.cameraEntity.url, photoView)
        dialog.show()
    }

    private fun loadPhoto(url: String, photoView: ImageView) {
        Glide.with(requireContext())
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(photoView)
    }

    private fun initUIState() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                cameraListViewModel.stateCamera.collect {
                    when (it) {
                        is CameraListState.Success -> successState(it.cameras)
                    }
                }
                cameraListViewModel.stateDistrict.collect {
                    when (it) {
                        is DistrictListState.Success -> successDistrictState(it.district)
                    }
                }
                cameraListViewModel.stateLoading.collect {
                    when (it) {
                        LoadingState.Loading -> binding.progressBar.isVisible = true
                        LoadingState.Loaded -> binding.progressBar.isVisible = false
                    }
                }
            }
        }
    }

    private fun initAdapter() {

        lifecycleScope.launch {
            val mapita = withContext(Dispatchers.IO) {
                databaseRepository.getDistrictList().associate { it.name to it.id }
            }

            val lista = listOf("Todos") + mapita
                .keys
                .toList()

            val adapter = ArrayAdapter<String>(
                requireContext(), // Contexto de la actividad
                android.R.layout.simple_spinner_item, // Layout para los items del Spinner
                lista // Lista de datos a mostrar
            )
            // Establecer el adaptador en el Spinner
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.comboBox.adapter = adapter

            // Agregar un listener al Spinner
            binding.comboBox.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    // El nombre seleccionado
                    val nombreSeleccionado = lista[position]
                    idSeleccionado = mapita[nombreSeleccionado]
                    cameraListViewModel.searchCameras(
                        idSeleccionado,
                        binding.svCameras.query.toString()
                    )
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }

        }


//        val items = arrayOf("1", "2", "3")
//        (binding.comboBox.editText as? MaterialAutoCompleteTextView)?.setSimpleItems(
//            items
//        )
    }

    private fun successState(cameraList: List<CameraWithDistrict>) {
//        val sb = StringBuilder()
//
//        cameraList.stream().forEach {
//            sb.append("${it.id} ${it.name} ${it.description} ${it.address} ${it.latitude} ${it.longitude} ${it.url}\n\n")
//            binding.tvCamera.text = sb
//            cameraUrl = it.url
//        }
        cameraAdapter.updateList(cameraList)
        binding.rvCamera.apply {
            scrollToPosition(0)
        }
    }

    private fun successDistrictState(district: List<DistrictEntity>) {
        // districtList = district
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCameraListBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}