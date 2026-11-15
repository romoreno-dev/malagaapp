package com.romoreno.malagapp.ui.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.romoreno.malagapp.R
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

        val camera = cameraEntity.cameraEntity
        val details = "${camera.description} \n ${camera.address}"
        binding.tvCameraTitle.text = if (camera.alias.isNullOrBlank()) details
        else "${camera.alias}\n$details"

        binding.ivEdit.setOnClickListener { whenCameraItemSelected.onEditAliasSelected(camera) }
        binding.cameraCardView.setOnLongClickListener {
            whenCameraItemSelected.onEditAliasSelected(camera)
            true
        }

        binding.cameraCardView.setOnClickListener {
            whenCameraItemSelected.onCardViewSelected(cameraEntity)
        }

        binding.ivStar.apply {
            setImageResource(if (cameraEntity.cameraEntity.favourite) {R.drawable.ic_filled_star}
            else {R.drawable.ic_star})

            setOnClickListener { whenCameraItemSelected.onStartSelected(cameraEntity.cameraEntity) }
        }

    }
}