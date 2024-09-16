package com.example.adminobr.ui.partediario

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.adminobr.data.ListarPartesDiarios
import com.example.adminobr.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class ParteDiarioPagingSource(
    private val client: OkHttpClient,
    private val baseUrl: String,
    private val equipo: String,
    private val fechaInicio: String,
    private val fechaFin: String,
    private val empresaDbName: String
) : PagingSource<Int, ListarPartesDiarios>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ListarPartesDiarios> {
        return withContext(Dispatchers.IO) {
            try {
                val page = params.key ?: 1 // Ignoramos la página, ya que la API no soporta paginación en este caso
                val pageSize = params.loadSize // Ignoramos el tamaño de la página

                val requestBody = FormBody.Builder()
                    .add("equipo", equipo)
                    .add("fechaInicio", fechaInicio)
                    .add("fechaFin", fechaFin)
                    .add("empresaDbName", empresaDbName)
                    .add("page", page.toString()) // Enviamos la página aunque la API no la use
                    .add("pageSize", pageSize.toString()) // Enviamos el tamaño de página aunque la API no lo use
                    .build()

                val request = Request.Builder()
                    .url("$baseUrl${Constants.PartesDiarios.GET_LISTA}")
                    .post(requestBody)
                    .build()

                Log.d("ParteDiarioPagingSource", "Enviando petición: ${request.url}")
                Log.d("ParteDiarioPagingSource", "Cuerpo de la petición: ${requestBody}")


                val response = client.newCall(request).execute()


                Log.d("ParteDiarioPagingSource", "Código de respuesta: ${response.code}")
                Log.d("ParteDiarioPagingSource", "Respuesta: ${response.body?.string()}")

                val jsonData = response.body?.string() ?: ""
                val jsonObject = JSONObject(jsonData)


                val parteDiario = if (jsonObject.length() > 0) {
                    ListarPartesDiarios(
                        id_parte_diario = jsonObject.getInt("id_parte_diario"),
                        fecha = jsonObject.getString("fecha"),
                        equipo_id = jsonObject.getInt("equipo_id"),
                        interno = jsonObject.getString("interno"),
                        horas_inicio = jsonObject.getInt("horas_inicio"),
                        horas_fin = jsonObject.getInt("horas_fin"),
                        horas_trabajadas = jsonObject.getInt("horas_trabajadas"),
                        observaciones = jsonObject.optString("observaciones"),
                        obra_id = jsonObject.getInt("obra_id"),
                        user_created = jsonObject.getInt("user_created"),
                        estado_id = jsonObject.getInt("estado_id")
                    )
                } else {
                    null
                }

                val partesDiarios = if (parteDiario != null) listOf(parteDiario) else emptyList()

                LoadResult.Page(
                    data = partesDiarios,
                    prevKey = null,
                    nextKey = null
                )
            } catch (e: IOException) {
                Log.e("ParteDiarioPagingSource", "Error de IO: ${e.message}")
                LoadResult.Error(e)
            } catch (e: Exception) {
                Log.e("ParteDiarioPagingSource", "Error: ${e.message}")
                LoadResult.Error(e)
            }
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ListarPartesDiarios>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}

//package com.example.adminobr.ui.partediario
//
//import androidx.paging.PagingSource
//import androidx.paging.PagingState
//import com.example.adminobr.data.ListarPartesDiarios
//import com.example.adminobr.utils.Constants
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import okhttp3.FormBody
//import okhttp3.OkHttpClient
//import okhttp3.Request
//import org.json.JSONArray
//import java.io.IOException
//
//class ParteDiarioPagingSource(
//    private val client: OkHttpClient,
//    private val baseUrl: String,
//    private val equipo: String,
//    private val fechaInicio: String,
//    private val fechaFin: String,
//    private val empresaDbName: String // Incluye este nuevo parámetro
//) : PagingSource<Int, ListarPartesDiarios>() {
//
//    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ListarPartesDiarios> {
//        return withContext(Dispatchers.IO) {
//            try {
//                val page = params.key ?: 1
//                val pageSize = params.loadSize
//
//                // Crea el cuerpo de la solicitud con todos los parámetros
//                val requestBody = FormBody.Builder()
//                    .add("equipo", equipo)
//                    .add("fechaInicio", fechaInicio)
//                    .add("fechaFin", fechaFin)
//                    .add("empresaDbName", empresaDbName)
//                    .add("page", page.toString())
//                    .add("pageSize", pageSize.toString())
//                    .build()
//
//                val request = Request.Builder()
//                    .url("$baseUrl${Constants.PartesDiarios.GET_LISTA}")
//                    .post(requestBody)  // Usa POST en lugar de GET
//                    .build()
//
//                val response = client.newCall(request).execute()
//
//                val jsonData = response.body?.string() ?: ""
//                val jsonArray = JSONArray(jsonData)
//
//                val partesDiarios = mutableListOf<ListarPartesDiarios>()
//
//                for (i in 0 until jsonArray.length()) {
//                    val jsonObject = jsonArray.getJSONObject(i)
//                    val parteDiario = ListarPartesDiarios(
//                        id_parte_diario = jsonObject.getInt("id_parte_diario"),
//                        fecha = jsonObject.getString("fecha"),
//                        equipo_id = jsonObject.getInt("equipo_id"),
//                        interno = jsonObject.getString("interno"),
//                        horas_inicio = jsonObject.getInt("horas_inicio"),
//                        horas_fin = jsonObject.getInt("horas_fin"),
//                        horas_trabajadas = jsonObject.getInt("horas_trabajadas"),
//                        observaciones = jsonObject.optString("observaciones"),
//                        obra_id = jsonObject.getInt("obra_id"),
//                        user_created = jsonObject.getInt("user_created"),
//                        estado_id = jsonObject.getInt("estado_id")
//                    )
//                    partesDiarios.add(parteDiario)
//                }
//
//                val nextKey = if (partesDiarios.size < pageSize) {
//                    null
//                } else {
//                    page + 1
//                }
//
//                LoadResult.Page(
//                    data = partesDiarios,
//                    prevKey = if (page == 1) null else page - 1,
//                    nextKey = nextKey
//                )
//            } catch (e: IOException) {
//                LoadResult.Error(e)
//            } catch (e: Exception) {
//                LoadResult.Error(e)
//            }
//        }
//    }
//
//    override fun getRefreshKey(state: PagingState<Int, ListarPartesDiarios>): Int? {
//        return state.anchorPosition?.let { anchorPosition ->
//            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
//                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
//        }
//    }
//}
