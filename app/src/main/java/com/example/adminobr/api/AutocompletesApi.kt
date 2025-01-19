package com.example.adminobr.api

import android.app.Application
import android.util.Log
import com.example.adminobr.utils.Constants
import com.example.adminobr.data.Empresa
import com.example.adminobr.data.Equipo
import com.example.adminobr.data.Estado
import com.example.adminobr.data.Obra
import com.example.adminobr.data.Rol
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AutocompletesApi {

    @POST(Constants.Equipos.GET_LISTA)
    suspend fun getEquipos(@Body requestBody: RequestBody
    ): List<Equipo>

    @POST(Constants.Obras.GET_LISTA)
    suspend fun getObras(@Body requestBody: RequestBody
    ): List<Obra>

    companion object {
        private val BASE_URL = Constants.getBaseUrl()


        fun create(
            application: Application
        ): AutocompletesApi {
            val logging = HttpLoggingInterceptor().apply {
                setLevel(HttpLoggingInterceptor.Level.BODY)
            }
            val httpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()

            val gson = GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create()
            Log.d("AutocompletesApi", "Base URL: $BASE_URL")
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient)
                .build()

            return retrofit.create(AutocompletesApi::class.java)
        }
    }

    @POST(Constants.Roles.GET_LISTA)
    suspend fun getRoles(@Body requestBody: RequestBody): List<Rol>

    @POST(Constants.Estados.GET_LISTA)
    suspend fun getEstados(@Body requestBody: RequestBody): List<Estado>
}
