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
        holder.render(list[position], whenCameraItemSelected)
    }

    override fun getItemCount() = list.size
}
