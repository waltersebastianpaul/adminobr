package com.example.adminobr.data

data class ParteSimple(
    val fecha: String,
    val equipo: String,
    val horas: Int,
    val timestamp: Long = System.currentTimeMillis() // Agregar timestamp
)

//data class ParteSimple(
//    val fecha: String,
//    val equipo: String,
//    val horas: Int
//)
