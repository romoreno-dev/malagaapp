package com.romoreno.malagapp.ui.adapter

import androidx.recyclerview.widget.DiffUtil
import com.romoreno.malagapp.data.database.entities.CameraEntity

class CameraDiffUtil(
    private val oldList: List<CameraEntity>,
    private val newList: List<CameraEntity>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}