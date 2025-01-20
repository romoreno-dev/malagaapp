package com.romoreno.malagapp.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textview.MaterialTextView
import com.romoreno.malagapp.R
import com.romoreno.malagapp.data.database.dao.CameraDao
import com.romoreno.malagapp.data.database.dao.CameraDao_Impl
import com.romoreno.malagapp.data.database.dto.CameraWithDistrict
import com.romoreno.malagapp.data.database.entities.CameraEntity
import com.romoreno.malagapp.data.database.entities.DistrictEntity
import com.romoreno.malagapp.data.database.repository.DatabaseRepository
import com.romoreno.malagapp.data.database.repository.DatabaseRepositoryImpl
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

    private var cameraUrl: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        cameraListViewModel.searchDistricts()
        cameraListViewModel.searchCameras()
    }

    private fun initUI() {
        initUIState()
        initAdapter()
        initList()
    }

    private fun initList() {
        cameraAdapter = CameraListAdapter(WhenCameraItemSelected(onCardViewSelected = {
            whenCameraItemSelected(it)
        }))
        binding.rvCamera.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cameraAdapter
        }
    }

    private fun whenCameraItemSelected(cameraEntity: CameraWithDistrict) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_camera)
        val photoView = dialog.findViewById<ImageView>(R.id.photoView)
        val title = dialog.findViewById<MaterialTextView>(R.id.etTitle)
        var recargarFoto = true

        title.text =  cameraEntity.cameraEntity.description + " " + recargarFoto

        var isDialogVisible = true // Bandera para controlar la recarga

        title.setOnClickListener() {
            recargarFoto = !recargarFoto
            title.text =  cameraEntity.cameraEntity.description + " " + recargarFoto
        }

//                // Función para recargar la foto
        fun reloadPhoto() {
            Glide.with(requireContext())
                .load(cameraEntity.cameraEntity.url)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)// Obtiene una URL aleatoria
                .into(photoView)
        }
//
                // Iniciar recarga periódica
                lifecycleScope.launch {
                    while (isDialogVisible && recargarFoto) {
         reloadPhoto()
                        delay(5000) // Espera 5 segundos antes de la próxima recarga
                    }
                }

                // Mostrar el diálogo
                dialog.setOnDismissListener {
                    isDialogVisible = false // Detener la recarga cuando el diálogo se cierra
                }

        reloadPhoto() // Cargar la imagen inicial
        dialog.show()
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
            val mapita = withContext(Dispatchers.IO) { databaseRepository.getDistrictList().associate { it.name  to it.id } }

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
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    // El nombre seleccionado
                    val nombreSeleccionado = lista[position]
                    val idSeleccionado = mapita.get(nombreSeleccionado)
                    if (idSeleccionado != null) {
                        cameraListViewModel.searchCameras(idSeleccionado)
                    } else {
                        cameraListViewModel.searchCameras()
                    }
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