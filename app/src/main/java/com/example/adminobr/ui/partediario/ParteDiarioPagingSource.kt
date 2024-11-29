package com.example.adminobr.ui.partediario

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.adminobr.api.ApiService
import com.example.adminobr.data.ParteDiario
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ParteDiarioPagingSource(
    private val apiService: ApiService,
    private val empresaDbName: String,
    private val equipoId: Int,
    private val fechaInicio: String,
    private val fechaFin: String
) : PagingSource<Int, ParteDiario>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ParteDiario> {
        return try {
            val page = params.key ?: 1
            val jsonBody = JSONObject().apply {
                put("empresaDbName", empresaDbName)
                put("equipoId", equipoId)  // Cambiado a equipoId para consistencia
                put("fechaInicio", fechaInicio)
                put("fechaFin", fechaFin)
                put("page", page)
                put("pageSize", params.loadSize)
            }
            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

            Log.d("ParteDiarioPagingSource", "Enviando datos a la API: $jsonBody")
            val response = apiService.getAllPartesDiarios(requestBody)

            if (response.isSuccessful) {
                val partes = response.body()?.data ?: emptyList()
                Log.d("ParteDiarioPagingSource", "Respuesta de la API: ${response.body()}")
                LoadResult.Page(
                    data = partes,
                    prevKey = if (page == 1) null else page - 1,
                    nextKey = if (partes.isEmpty()) null else page + 1
                )
            } else {
                Log.e("ParteDiarioPagingSource", "Error en la respuesta de la API: ${response.errorBody()?.string()}")
                LoadResult.Error(Throwable(response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            Log.e("ParteDiarioPagingSource", "Error de red o procesamiento: ${e.message}")
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ParteDiario>): Int? =
        state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
}
