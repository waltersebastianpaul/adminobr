package com.example.adminobr.utils

import android.view.View
import com.google.android.material.snackbar.Snackbar
import androidx.core.content.ContextCompat
import com.example.adminobr.R

var currentSnackbar: Snackbar? = null

fun showNetworkErrorSnackbar(
    view: View,
    message: String = "Sin conexión a internet.",
    onConnectionSuccess: (() -> Unit)? = null
) {
    currentSnackbar = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE)
        .setAction("Reintentar") {
            attemptConnection(view, onConnectionSuccess)
        }
//        .setBackgroundTint(ContextCompat.getColor(view.context, R.color.colorDanger))
        .setTextColor(ContextCompat.getColor(view.context, R.color.danger_400))
    currentSnackbar?.show()
}

private fun attemptConnection(
    view: View,
    onConnectionSuccess: (() -> Unit)? = null
) {
    if (NetworkStatusHelper.isNetworkAvailable()) {
        dismissNetworkErrorSnackbar()
        onConnectionSuccess?.invoke() // Llamar al callback de éxito
    } else {
        // Cerrar el Snackbar anterior antes de mostrar el nuevo
        dismissNetworkErrorSnackbar()
        showNetworkErrorSnackbar(view, "Sin conexión, intenta más tarde", onConnectionSuccess)
    }
}

fun dismissNetworkErrorSnackbar() {
    currentSnackbar?.dismiss()
    currentSnackbar = null
}