package com.example.adminobr.api

import com.example.adminobr.data.ListarPartesDiarios
import com.example.adminobr.data.LoginResponse
import com.example.adminobr.data.ParteDiario
import com.example.adminobr.utils.Constants
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST(Constants.Auth.LOGIN)
    suspend fun login(
        @Body requestBody: RequestBody,
        @Header("Cache-Control") cacheControl: String = "no-cache"
    ): Response<LoginResponse>

    @POST(Constants.Auth.LOGOUT)
    suspend fun logout(): Response<Unit>

    @DELETE("api/equipos/partes/guardar_parte_diario.php")
    suspend fun deleteParteDiario(
        @Query("id_parte_diario") idParteDiario: Int,
        @Query("empresaDbName") empresaDbName: String
    ): Response<Void>

    @PUT("api/equipos/partes/guardar_parte_diario.php")
    suspend fun updateParteDiario(
        @Body parteDiario: ListarPartesDiarios,
        @Query("empresaDbName") empresaDbName: String
    ): Response<Unit>

    @POST(Constants.PartesDiarios.GET_ULTIMOS_PARTES)
    suspend fun getUltimosPartesDiarios(
        @Body requestBody: RequestBody
    ): Response<List<ParteDiario>>
}