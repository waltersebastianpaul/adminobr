package com.example.adminobr.data

import android.util.Log

data class Obra(
    val id: Int,
    val centroCosto: String,
    val nombre: String,
    val localidad: String?,
    val estado: Boolean
) {
    override fun toString(): String {
        Log.d("Obra", "Obra: $centroCosto")
        return "$centroCosto - $nombre"
    }
}