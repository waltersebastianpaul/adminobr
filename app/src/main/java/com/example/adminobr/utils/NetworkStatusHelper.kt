package com.example.adminobr.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object NetworkStatusHelper {
    private val _networkAvailable = MutableStateFlow(false)
    val networkAvailable: StateFlow<Boolean> = _networkAvailable

    private lateinit var connectivityManager: ConnectivityManager

    fun initialize(context: Context) {
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        startNetworkCallback()
    }

    private fun startNetworkCallback() {
        val networkRequest = NetworkRequest.Builder().build()
        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _networkAvailable.value = true
            }

            override fun onLost(network: Network) {
                _networkAvailable.value = false
            }
        })
    }

    fun refreshNetworkStatus() {
        _networkAvailable.value = isNetworkAvailable()
    }

    /**
     * Verifica si hay una conexión a internet disponible.
     * @return true si hay conexión a internet, false en caso contrario.
     */
    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
    }

    /**
     * Verifica si la conexión actual es a través de Wi-Fi.
     * @return true si está conectado a Wi-Fi, false en caso contrario.
     */
    fun isWifiConnected(): Boolean {
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
    }

    /**
     * Verifica si la conexión actual es a través de datos móviles.
     * @return true si está conectado a datos móviles, false en caso contrario.
     */
    fun isMobileDataConnected(): Boolean {
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false
    }
}
