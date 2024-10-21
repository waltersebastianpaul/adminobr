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
    val estadoId: Int,

    // Nuevas propiedades para los campos de mantenimiento
    val combustible_tipo: String?,
    val combustible_cant: Int?,
    val aceite_motor_cant: Int?,
    val aceite_hidra_cant: Int?,
    val aceite_otro_cant: Int?,
    val engrase_general: Boolean?,
    val filtro_aire: Boolean?,
    val filtro_aceite: Boolean?,
    val filtro_comb: Boolean?,
    val filtro_otro: Boolean?
)