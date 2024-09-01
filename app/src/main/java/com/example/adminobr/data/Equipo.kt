package com.example.adminobr.data

data class Equipo(
    val id: Int,
    val interno: String,
    val descripcion: String) {
    override fun toString(): String {
        return "$interno - $descripcion"
    }
}