package com.romoreno.malagapp.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import io.getstream.photoview.PhotoView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foto a pantalla completa con zoom por pellizco (y doble toque para acercar). No cambia la
 * orientacion de la app. Atras o la X para cerrar. Mientras no se haya hecho zoom, la foto se
 * sigue actualizando cada 5 s.
 */
object FullScreenPhoto {

    private const val REFRESH_MILLIS = 5000L

    fun show(context: Context, owner: LifecycleOwner, url: String, initial: Drawable?) {
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val root = FrameLayout(context).apply { setBackgroundColor(Color.BLACK) }
        val photo = PhotoView(context).apply {
            setImageDrawable(initial)
            maximumScale = 8f
        }
        root.addView(
            photo,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        val close = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundColor(0x66000000)
            setOnClickListener { dialog.dismiss() }
        }
        val margin = (12 * context.resources.displayMetrics.density).toInt()
        root.addView(
            close,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.END
            ).apply { setMargins(margin, margin, margin, margin) }
        )

        fun load() {
            // Tamano original: asi el zoom se ve nitido y no una version reducida
            Glide.with(context)
                .load(url)
                .placeholder(photo.drawable)
                .override(Target.SIZE_ORIGINAL)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(photo)
        }

        val job = owner.lifecycleScope.launch {
            load()
            while (isActive) {
                delay(REFRESH_MILLIS)
                // Recargar reinicia el zoom: solo si el usuario no ha ampliado
                if (photo.scale <= photo.minimumScale + 0.01f) load()
            }
        }

        dialog.setOnDismissListener { job.cancel() }
        dialog.setContentView(root)
        dialog.show()
    }
}
