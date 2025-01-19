package com.romoreno.malagapp.ui.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.romoreno.malagapp.data.database.dto.CameraWithDistrict
import com.romoreno.malagapp.data.database.entities.CameraEntity
import com.romoreno.malagapp.databinding.CameraItemBinding

class CameraListViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val binding = CameraItemBinding.bind(view)

    fun render(
        cameraEntity: CameraWithDistrict,
        whenCameraItemSelected: WhenCameraItemSelected
    ) {

        binding.tvDistrict.text = cameraEntity.district?.name

        binding.tvCameraTitle.text =
            "${cameraEntity.cameraEntity.description} \n ${cameraEntity.cameraEntity.address}"

        binding.cameraCardView.setOnClickListener {
            whenCameraItemSelected.onCardViewSelected(cameraEntity)
        }

    }
}