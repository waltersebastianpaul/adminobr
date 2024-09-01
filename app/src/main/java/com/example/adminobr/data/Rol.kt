package com.example.adminobr.data

data class Rol(
    val id: Int,
    val nombre: String,
    val descripcion: String) {
    override fun toString(): String {
        return nombre
    }
}