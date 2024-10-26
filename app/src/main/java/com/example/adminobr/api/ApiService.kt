package com.example.adminobr.api

import com.example.adminobr.data.ListarPartesDiarios
import com.example.adminobr.data.Usuario
import com.example.adminobr.data.LoginResponse
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.http.*
import com.example.adminobr.utils.Constants
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(
        @Body requestBody: RequestBody,
        @Header("Cache-Control") cacheControl: String = "no-cache"
    ): Response<LoginResponse>

    @POST("api/auth/logout")
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
    ): Response<List<ListarPartesDiarios>>





    // Crear usuario
    @FormUrlEncoded
    @POST("api/usuarios/crear_usuario.php")
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
        @Field("empresaDbName") empresaDbName: String
    ): Response<ApiResponse<Usuario>> // Cambiado a ApiResponse<Usuario>

    // Actualizar usuario
    @PUT("api/usuarios/editar_usuario.php")
    suspend fun actualizarUsuario(
        @Body requestBody: RequestBody
    ): Response<ApiResponse<Usuario>> // Cambiado a ApiResponse<Usuario>

    // Eliminar usuario
    @DELETE("api/usuarios/eliminar_usuario.php")
    suspend fun eliminarUsuario(
        @Query("id") idUsuario: Int,
        @Query("empresaDbName") empresaDbName: String
    ): Response<ApiResponse<Unit>> // Cambiado a ApiResponse<Unit>

    // Obtener usuario por ID
    @POST("api/usuarios/get_usuario_by_id.php")
    suspend fun getUsuarioById(
        @Body requestBody: RequestBody
    ): Response<ApiResponse<Usuario>> // Cambiado a ApiResponse<Usuario>

    // Obtener todos los usuarios
    @POST("api/usuarios/get_usuarios.php")
    suspend fun getAllUsers(
        @Body requestBody: RequestBody
    ): Response<ApiResponse<List<Usuario>>> // Cambiado a ApiResponse<List<Usuario>>

    companion object {
        fun create(): ApiService {
            // Configuración del interceptor de logging para debug
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // Configuración del cliente OkHttp
            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .readTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS)
                .build()

            // Creación de Retrofit
            return Retrofit.Builder()
                .baseUrl(Constants.getBaseUrl()) // Reemplaza con la URL base de tu API
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}