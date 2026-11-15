package com.romoreno.malagapp.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity

/**
 * Con Hilt el contexto de un fragment viene envuelto en ContextWrapper(s): un simple
 * "as FragmentActivity" falla, hay que desenvolverlo hasta dar con la Activity.
 */
fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return null
}
