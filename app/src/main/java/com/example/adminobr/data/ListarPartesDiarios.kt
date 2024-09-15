package com.example.adminobr.data

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
data class ListarPartesDiarios(
    val id_parte_diario: Int,
    val fecha: String,
    val equipo_id: Int,
    val interno: String,
    val horas_inicio: Int,
    val horas_fin: Int,
    val horas_trabajadas: Int,
    val observaciones: String?,
    val obra_id: Int,
    val user_created: Int,
    val estado_id: Int
) : Parcelable
