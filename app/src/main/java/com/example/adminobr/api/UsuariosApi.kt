package com.example.adminobr.api

//import com.example.adminobr.api.ApiResponse
//import com.example.adminobr.data.Estado
//
//import com.example.adminobr.data.Rol
//import com.example.adminobr.data.Usuario
//import com.example.adminobr.utils.Constants
//import com.google.gson.GsonBuilder
//import okhttp3.OkHttpClient
//import okhttp3.logging.HttpLoggingInterceptor
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//import retrofit2.http.Body
//import retrofit2.http.GET
//import retrofit2.http.POST
//
//
//interface UsuariosApi {@GET(Constants.Usuarios.GET_LISTA)
//suspend fun getUsuarios(): List<Usuario>
//
//
//    @GET(Constants.Roles.GET_LISTA)
//    suspend fun getRoles(): List<Rol>
//
//
//    @GET(Constants.Estados.GET_LISTA)
//    suspend fun getEstados(): List<Estado>
//
//
//    @POST(Constants.Usuarios.GUARDAR)
//    suspend fun guardarUsuario(@Body usuario: Usuario): ApiResponse
//
//    companion object {
//        private val BASE_URL = Constants.getBaseUrl()
//
//        fun create(): UsuariosApi {
//            val logging = HttpLoggingInterceptor()
//            logging.setLevel(HttpLoggingInterceptor.Level.BODY)
//            val httpClient = OkHttpClient.Builder()
//                .addInterceptor(logging)
//                .build()
//
//
//            val gson = GsonBuilder()
//                .setDateFormat("yyyy-MM-dd HH:mm:ss")
//                .create()
//
//
//            val retrofit = Retrofit.Builder()
//                .baseUrl(BASE_URL)
//                .addConverterFactory(GsonConverterFactory.create(gson))
//                .client(httpClient)
//                .build()
//
//            return retrofit.create(UsuariosApi::class.java)
//        }
//    }
//}