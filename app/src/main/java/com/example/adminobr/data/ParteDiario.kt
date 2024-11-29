package com.example.adminobr.data

import com.google.gson.annotations.SerializedName

data class ParteDiario(
    @SerializedName("idParteDiario") val idParteDiario: Int?,
    @SerializedName("fecha") val fecha: String?,
    @SerializedName("equipoId") val equipoId: Int?,
    @SerializedName("equipoInterno") val equipoInterno: String?, // Este es el que viene en el JSON
    @SerializedName("horasInicio") val horasInicio: Int?,
    @SerializedName("horasFin") val horasFin: Int?,
    @SerializedName("horasTrabajadas") val horasTrabajadas: Int?,
    @SerializedName("observaciones") val observaciones: String?,
    @SerializedName("obraId") val obraId: Int?,
    @SerializedName("obraNombre") val obraNombre: String? = null,  // Nuevo campo para el nombre de la obra
    @SerializedName("obraCentroCosto") val obraCentroCosto: String? = null,  // Nuevo campo para el centro de costo de la obra
    @SerializedName("userCreated") val userCreated: Int?,
    @SerializedName("userUpdated") val userUpdated: Int?,
    @SerializedName("estadoId") val estadoId: Int?,
    @SerializedName("combustibleTipo") val combustibleTipo: String? = "",
    @SerializedName("combustibleCant") val combustibleCant: Int? = 0,
    @SerializedName("aceiteMotorCant") val aceiteMotorCant: Int? = 0,
    @SerializedName("aceiteHidraCant") val aceiteHidraCant: Int? = 0,
    @SerializedName("aceiteOtroCant") val aceiteOtroCant: Int? = 0,
    @SerializedName("engraseGeneral") val engraseGeneral: Int? = 0,
    @SerializedName("filtroAire") val filtroAire: Int? = 0,
    @SerializedName("filtroAceite") val filtroAceite: Int? = 0,
    @SerializedName("filtroComb") val filtroComb: Int? = 0,
    @SerializedName("filtroOtro") val filtroOtro: Int? = 0
)
