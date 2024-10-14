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

    var combustible_tipo: String? = null,
    var combustible_cant: Int? = null,
    var lubricante1_tipo: String? = null,
    var lubricante1_cant: Int? = null,
    var lubricante2_tipo: String? = null,
    var lubricante2_cant: Int? = null,
    var engrase: Boolean = false, // Campo booleano
    var filtro1_tipo: String? = null,
    var filtro1_cant: Int? = null,
    var filtro2_tipo: String? = null,
    var filtro2_cant: Int? = null,
    var filtro3_tipo: String? = null,
    var filtro3_cant: Int? = null
)