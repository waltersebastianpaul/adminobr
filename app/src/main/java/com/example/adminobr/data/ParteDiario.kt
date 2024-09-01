package com.example.adminobr.data

data class ParteDiario(
    val id_parte_diario: Int = 0,
    val fecha: String,
    val equipoId: Int,
    val equipoInterno: String,
    val horasInicio: Int,
    val horasFin: Int,
    val horasTrabajadas: Int,
    val observaciones: String?,
    val obraId: Int,
    val userCreated: Int,
    val estadoId: Int
)