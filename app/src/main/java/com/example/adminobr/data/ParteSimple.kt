package com.example.adminobr.data

data class ParteSimple(
    val fecha: String,
    val equipo: String,
    val horas: Int,
    val observaciones: String = "" // Inicializar como cadena vacía
) {
    val timestamp: Long = System.currentTimeMillis()
}