package com.example.adminobr.ui.partediario

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.adminobr.api.ApiService
import com.example.adminobr.data.ParteDiario
import com.example.adminobr.utils.SessionManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ParteDiarioPagingSource(
    private val apiService: ApiService,
    private val sessionManager: SessionManager,
    private val equipoId: Int,
    private val obraId: Int,
    private val fechaInicio: String,
    private val fechaFin: String
) : PagingSource<Int, ParteDiario>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ParteDiario> {
        return try {
            val page = params.key ?: 1
            val pageSize = params.loadSize

            // Construir el cuerpo de la solicitud
            val requestBody = createFilterRequestBody(page, pageSize).toRequestBody("application/json".toMediaTypeOrNull())

            Log.d("ParteDiarioPagingSource", "Enviando solicitud a la API: $requestBody")
            val response = apiService.getAllPartesDiarios(requestBody)

            if (response.isSuccessful) {
                val partesDiarios = response.body()?.data ?: emptyList()
                Log.d("ParteDiarioPagingSource", "Respuesta de la API: ${response.body()}")

                LoadResult.Page(
                    data = partesDiarios,
                    prevKey = if (page == 1) null else page - 1,
                    nextKey = if (partesDiarios.isEmpty()) null else page + 1
                )
            } else {
                Log.e("ParteDiarioPagingSource", "Error en la respuesta de la API: ${response.errorBody()?.string()}")
                LoadResult.Error(Throwable("Error HTTP: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("ParteDiarioPagingSource", "Error durante la carga: ${e.message}")
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ParteDiario>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    // Método para construir el cuerpo JSON de los filtros
    private fun createFilterRequestBody(page: Int, pageSize: Int): String {
        val empresaDbName = sessionManager.getEmpresaData()["empresaDbName"] ?: ""

        val jsonObject = JSONObject().apply {
            put("empresaDbName", empresaDbName)
            put("equipoId", equipoId.takeIf { it != 0 }) // Solo incluye el ID si no es 0
            put("obraId", obraId.takeIf { it != 0 }) // Solo incluye el ID si no es 0
            put("fechaInicio", fechaInicio.takeIf { it.isNotBlank() }) // Solo incluye si no está vacío
            put("fechaFin", fechaFin.takeIf { it.isNotBlank() }) // Solo incluye si no está vacío
            put("page", page)
            put("pageSize", pageSize)
        }

        Log.d("ParteDiarioPagingSource", "Cuerpo de la solicitud JSON: $jsonObject")
        return jsonObject.toString()
    }
}
