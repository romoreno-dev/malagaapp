package com.romoreno.malagapp.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.romoreno.malagapp.R
import com.romoreno.malagapp.data.database.dto.CameraWithDistrict
import com.romoreno.malagapp.data.database.entities.CameraEntity

class CameraListAdapter(private val whenCameraItemSelected: WhenCameraItemSelected,
                        private var list: List<CameraWithDistrict> = emptyList())  :
    RecyclerView.Adapter<CameraListViewHolder>() {

    /**
     * En la vista de favoritos se muestra o el alias o la calle (no ambos). Al cambiarlo hay que
     * volver a pintar todo: DiffUtil no lo detecta porque los datos de las camaras no cambian.
     */
    var favouritesMode: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    fun firstId(): Int? = list.firstOrNull()?.cameraEntity?.id

    fun updateList(newList: List<CameraWithDistrict>) {
        val productDiff = CameraDiffUtil(list, newList)
        val result = DiffUtil.calculateDiff(productDiff)
        list = newList
        result.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CameraListViewHolder {
        return CameraListViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.camera_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: CameraListViewHolder, position: Int) {
        holder.render(list[position], whenCameraItemSelected, favouritesMode)
    }

    override fun getItemCount() = list.size
}
