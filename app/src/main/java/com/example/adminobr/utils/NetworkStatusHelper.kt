package com.example.adminobr.utils

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.adminobr.ui.login.LoginActivity
import com.google.android.material.snackbar.Snackbar

class NetworkStatusHelper(private val context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var snackbar: Snackbar? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            (context as? Activity)?.runOnUiThread {
                // Solo muestra el Toast cuando la red se restablezca
//                if (isWifiConnected()) {
//                    Toast.makeText(context, "Conectado por Wi-Fi", Toast.LENGTH_SHORT).show()
//                } else if (isMobileDataConnected()) {
//                    Toast.makeText(context, "Conectado por Datos Móviles", Toast.LENGTH_SHORT).show()
//                }

                // Recargar componentes que dependen de la red
                (context as? LoginActivity)?.reloadParticularComponents()
            }
        }

        override fun onLost(network: Network) {
            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, "Se ha perdido la conexión a Internet", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun registerNetworkCallback() {
        connectivityManager.registerNetworkCallback(
            NetworkRequest.Builder().build(),
            networkCallback
        )
    }

    fun unregisterNetworkCallback() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
    }

    fun isWifiConnected(): Boolean {
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
    }

    fun isMobileDataConnected(): Boolean {
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false
    }

    private fun showNetworkLostWarning() {
        val activity = context as? Activity ?: return
        val view = activity.findViewById<View>(android.R.id.content)

        // Show Snackbar with retry action
        snackbar = Snackbar.make(view, "No internet connection", Snackbar.LENGTH_INDEFINITE)
            .setAction("Retry") {
                if (isNetworkAvailable()) {
                    reload()
                } else {
                    showNetworkLostWarning()
                }
            }
        snackbar?.show()
    }

    private fun reload() {
        when (context) {
            is Activity -> context.recreate()
            is Fragment -> {
                context.activity?.supportFragmentManager?.beginTransaction()
                    ?.detach(context)?.attach(context)?.commitAllowingStateLoss()
            }
        }
    }
}
