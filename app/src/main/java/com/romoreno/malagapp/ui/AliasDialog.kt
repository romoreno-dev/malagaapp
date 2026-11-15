package com.romoreno.malagapp.ui

import android.content.Context
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputEditText
import com.romoreno.malagapp.R
import com.romoreno.malagapp.data.database.entities.CameraEntity

/** Cuadro pequeño con un campo de texto para ponerle un alias a una camara. */
object AliasDialog {

    /**
     * @param skipBlank si es true y se deja el campo vacio, no se llama a [onSave] (no se toca
     * nada). Si es false, vacio significa "quitar el alias".
     */
    fun show(
        context: Context,
        camera: CameraEntity,
        skipBlank: Boolean = false,
        onSave: (String?) -> Unit
    ) {
        val input = TextInputEditText(context).apply {
            setText(camera.alias ?: "")
            setHint(R.string.alias_hint)
            setSingleLine()
            setSelection(text?.length ?: 0)
        }
        val container = FrameLayout(context).apply {
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
            addView(input)
        }
        val dialog = AlertDialog.Builder(context)
            .setTitle(camera.description)
            .setView(container)
            .setPositiveButton(R.string.alias_save) { _, _ ->
                val text = input.text?.toString()?.trim().orEmpty()
                if (!(skipBlank && text.isEmpty())) onSave(text.ifEmpty { null })
            }
            .setNegativeButton(R.string.alias_cancel, null)
            .create()
        // Que salga el teclado directamente con el cursor en el campo
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()
        input.requestFocus()
    }
}
