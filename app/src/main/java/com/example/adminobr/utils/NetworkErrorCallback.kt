package com.example.adminobr.utils

/**
 * Interfaz para recibir notificaciones sobre cambios en el estado de la red.
 */
interface NetworkErrorCallback {
    /**
     * Se llama cuando el estado de la red cambia.
     */
    fun manageNetworkErrorLayout()
}