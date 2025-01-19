package com.example.adminobr.api

import com.example.adminobr.data.Empresa
import com.example.adminobr.data.Usuario
import com.example.adminobr.data.LoginResponse
import com.example.adminobr.data.ParteDiario
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import com.example.adminobr.utils.Constants
import java.util.concurrent.TimeUnit

interface ApiService {

    // MODULO DE LOGIN
    @POST(Constants.Auth.VALIDATE_TOKEN)
    suspend fun validateToken(
        @Header("Authorization") token: String
    ): Response<Unit>

    @POST(Constants.Auth.LOGIN)
    suspend fun login(
        @Body loginRequest: Map<String, String>
    ): Response<LoginResponse>

    @POST(Constants.Auth.LOGOUT)
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<Unit>

    @POST(Constants.Empresas.VALIDATE)
    suspend fun validateCompany(
        @Body request: Map<String, String>
    ): Response<Empresa>


    // MODULO DE PARTES DIARIOS
    // Crear parte diario
    @FormUrlEncoded
    @POST(Constants.PartesDiarios.CREATE)
    suspend fun crearParteDiario(
        @Field("fecha") fecha: String,
        @Field("equipoId") equipoId: Int,
        @Field("horasInicio") horasInicio: Int,
        @Field("horasFin") horasFin: Int,
        @Field("horasTrabajadas") horasTrabajadas: Int,
        @Field("observaciones") observaciones: String?,
        @Field("obraId") obraId: Int,
        @Field("userCreated") userCreated: Int,
        @Field("estadoId") estadoId: Int,
        @Field("combustibleTipo") combustibleTipo: String?,
        @Field("combustibleCant") combustibleCant: Int?,
        @Field("aceiteMotorCant") aceiteMotorCant: Int?,
        @Field("aceiteHidraCant") aceiteHidraCant: Int?,
        @Field("aceiteOtroCant") aceiteOtroCant: Int?,
        @Field("engraseGeneral") engraseGeneral: Int?,
        @Field("filtroAire") filtroAire: Int?,
        @Field("filtroAceite") filtroAceite: Int?,
        @Field("filtroComb") filtroComb: Int?,
        @Field("filtroOtro") filtroOtro: Int?,
        @Field("empresaDbName") empresaDbName: String
    ): Response<ApiResponse<ParteDiario>>

    // Editar parte diario
    @PUT(Constants.PartesDiarios.UPDATE)
    suspend fun updateParteDiario(
        @Body requestBody: RequestBody
    ): Response<ApiResponse<ParteDiario>>

    // Eliminar parte diario
    @DELETE(Constants.PartesDiarios.DELETE)
    suspend fun deleteParteDiario(
        @Query("id") idParteDiario: Int,
        @Query("empresaDbName") empresaDbName: String
    ): Response<ApiResponse<Unit>>

    // Obtener parte diario por ID
    @POST(Constants.PartesDiarios.GET_BY_ID)
    suspend fun getParteDiarioById(
        @Body requestBody: RequestBody
    ): Response<ApiResponse<ParteDiario>>

    // Obtener todos los partes diarios
    @POST(Constants.PartesDiarios.GET_ALL)
    suspend fun getAllPartesDiarios(
        @Body requestBody: RequestBody
    ): Response<ApiResponse<List<ParteDiario>>>

    @POST(Constants.PartesDiarios.GET_BY_EQUIPO)
    suspend fun getUltimoPartePorEquipo(
        @Body requestBody: RequestBody
    ): Response<ApiResponse<ParteDiario>>

    @POST(Constants.PartesDiarios.GET_BY_USER)
    suspend fun getUltimosPartesPorUsuario(
        @Body requestBody: RequestBody
    ): Response<ApiResponse<List<ParteDiario>>>


    // MODULO DE USUARIOS
    // Crear usuario
    @FormUrlEncoded
    @POST(Constants.Usuarios.CREATE)
    suspend fun crearUsuario(
        @Field("legajo") legajo: String,
        @Field("email") email: String,
        @Field("dni") dni: String,
        @Field("password") password: String,
        @Field("nombre") nombre: String,
        @Field("apellido") apellido: String,
        @Field("telefono") telefono: String,
        @Field("userCreated") userCreated: Int,
        @Field("estadoId") estadoId: Int,
        @Field("roleId") roleId: Int?,
        @Field("empresaDbName") empresaDbName: String
    ): Response<ApiResponse<Usuario>>

    // Editar usuario
    @PUT(Constants.Usuarios.UPDATE)
    suspend fun editarUsuario(
        @Body requestBody: RequestBody
    ): Response<ApiResponse<Usuario>>

    // Eliminar usuario
    @DELETE(Constants.Usuarios.DELETE)
    suspend fun eliminarUsuario(
        @Query("id") idUsuario: Int,
        @Query("empresaDbName") empresaDbName: String
    ): Response<ApiResponse<Unit>>

    // Obtener usuario por ID
    @POST(Constants.Usuarios.GET_BY_ID)
    suspend fun obtenerUsuario(
        @Body requestBody: RequestBody
    ): Response<ApiResponse<Usuario>>

    // Obtener todos los usuarios
    @POST(Constants.Usuarios.GET_ALL)
    suspend fun obtenerUsuarios(
        @Body requestBody: RequestBody
    ): Response<ApiResponse<List<Usuario>>>

    // Asignar rol a usuario
    @PUT(Constants.Roles.SET_ROL)
    suspend fun asignarRolUsuario(
        @Body requestBody: RequestBody
    ): Response<ApiResponse<Unit>>


    // Reset Password
    @POST(Constants.ResetPassword.SEND_RECOVERY_CODE)
    suspend fun sendRecoveryCode(
        @Body body: Map<String, String>
    ): Response<ApiResponse<Unit>>

    @POST(Constants.ResetPassword.VERIFY_CODE)
    suspend fun verifyCode(
        @Body body: Map<String, String>
    ): Response<ApiResponse<Unit>>

    @POST(Constants.ResetPassword.RESET_PASSWORD)
    suspend fun changePassword(
        @Body body: Map<String, String>
    ): Response<ApiResponse<Unit>>


    // MODULO DE DEVICE
    @POST(Constants.Device.DEVICE_INFO)
    suspend fun sendDeviceInfo(
        @Body body: Map<String, String?>
    ): Response<ApiResponse<Unit>>


    companion object {
        fun create(): ApiService {
            val client = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
                .readTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(Constants.getBaseUrl())  // Configura tu base URL aquí
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}



