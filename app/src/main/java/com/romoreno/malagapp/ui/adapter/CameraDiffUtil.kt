package com.romoreno.malagapp.ui.adapter

import androidx.recyclerview.widget.DiffUtil
import com.romoreno.malagapp.data.database.dto.CameraWithDistrict
import com.romoreno.malagapp.data.database.entities.CameraEntity

class CameraDiffUtil(
    private val oldList: List<CameraWithDistrict>,
    private val newList: List<CameraWithDistrict>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].cameraEntity.id == newList[newItemPosition].cameraEntity.id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}