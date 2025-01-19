package com.romoreno.malagapp.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.romoreno.malagapp.data.database.entities.DistrictEntity

class ComboAdapter(context: Context, resource: Int, objects: List<DistrictEntity>) :
    ArrayAdapter<DistrictEntity>(context, resource, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val district = getItem(position)

        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = district?.name // Mostrar la clave en el combo

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        val district = getItem(position)

        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = district?.id.toString() // Clave en el dropdown

        return view
    }

    // Obtener el valor cuando se selecciona un ítem
    fun getSelectedValue(position: Int): Int {
        return getItem(position)?.id ?: 1
    }
}
