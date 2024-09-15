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
import org.json.JSONArray
import java.io.IOException

class ParteDiarioPagingSource(
    private val client: OkHttpClient,
    private val baseUrl: String,
    private val equipo: String,
    private val fechaInicio: String,
    private val fechaFin: String,
    private val empresaDbName: String // Incluye este nuevo parámetro
) : PagingSource<Int, ListarPartesDiarios>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ListarPartesDiarios> {
        return withContext(Dispatchers.IO) {
            try {
                val page = params.key ?: 1
                val pageSize = params.loadSize

                // Crea el cuerpo de la solicitud con todos los parámetros
                val requestBody = FormBody.Builder()
                    .add("equipo", equipo)
                    .add("fechaInicio", fechaInicio)
                    .add("fechaFin", fechaFin)
                    .add("empresaDbName", empresaDbName)
                    .add("page", page.toString())
                    .add("pageSize", pageSize.toString())
                    .build()

                val request = Request.Builder()
                    .url("$baseUrl${Constants.PartesDiarios.GET_LISTA}")
                    .post(requestBody)  // Usa POST en lugar de GET
                    .build()

                val response = client.newCall(request).execute()

                val jsonData = response.body?.string() ?: ""
                Log.d("ParteDiarioPagingSource", "Response Data: $jsonData")
                val jsonArray = JSONArray(jsonData)

                val partesDiarios = mutableListOf<ListarPartesDiarios>()

                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val parteDiario = ListarPartesDiarios(
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
                    partesDiarios.add(parteDiario)
                }

                val nextKey = if (partesDiarios.size < pageSize) {
                    null
                } else {
                    page + 1
                }

                LoadResult.Page(
                    data = partesDiarios,
                    prevKey = if (page == 1) null else page - 1,
                    nextKey = nextKey
                )
            } catch (e: IOException) {
                LoadResult.Error(e)
            } catch (e: Exception) {
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
