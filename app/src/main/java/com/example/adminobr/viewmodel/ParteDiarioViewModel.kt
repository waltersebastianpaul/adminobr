package com.example.adminobr.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import androidx.paging.*
import com.example.adminobr.data.ListarPartesDiarios
import com.example.adminobr.data.ParteDiario
import com.example.adminobr.utils.Event
import com.example.adminobr.ui.partediario.ParteDiarioPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

import com.example.adminobr.utils.Constants
import com.example.adminobr.utils.SessionManager

class ParteDiarioViewModel(application: Application) : AndroidViewModel(application) {

    private val client = OkHttpClient.Builder().build()
    private val baseUrl = Constants.getBaseUrl() //"http://adminobr.site/"
//    private val baseUrl = Constants.buildUrl("") // Construye la URL base con CURRENT_DIR
    private val guardarParteDiarioUrl = Constants.PartesDiarios.GUARDAR
    private val sessionManager = SessionManager(application)

    private val _partesList = MutableLiveData<List<ParteDiario>>()
    val partesList: LiveData<List<ParteDiario>> = _partesList

    private val _mensaje = MutableLiveData<Event<String?>>()
    val mensaje: LiveData<Event<String?>> = _mensaje

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<Event<String?>>()
    val error: LiveData<Event<String?>> = _error

    private val _filterEquipo = MutableLiveData<String>()
    private val _filterFechaInicio = MutableLiveData<String>()
    private val _filterFechaFin = MutableLiveData<String>()

    val partesDiarios: Flow<PagingData<ListarPartesDiarios>> = combine(
        _filterEquipo.asFlow(),
        _filterFechaInicio.asFlow(),
        _filterFechaFin.asFlow()
    ) { equipo, fechaInicio, fechaFin ->
        Triple(equipo, fechaInicio, fechaFin)
    }.flatMapLatest { (equipo, fechaInicio, fechaFin) ->
        Log.d("ParteDiarioViewModel", "Fetching data with filter - Equipo: $equipo, FechaInicio: $fechaInicio, FechaFin: $fechaFin")
        val empresaDbName = sessionManager.getEmpresaData()?.db_name ?: ""
        Pager(PagingConfig(pageSize = 20)) {
            ParteDiarioPagingSource(client,
                baseUrl.toString(), equipo ?: "", fechaInicio ?: "", fechaFin ?: "", empresaDbName)
        }.flow.cachedIn(viewModelScope)
    }

    private inner class NuevoParteDiarioApi {

        suspend fun guardarParteDiario(parteDiario: ParteDiario): Pair<Boolean, Int?> {
            return withContext(Dispatchers.IO) {
                try{
                    // Obtén empresaDbName de SessionManager
                    val empresaDbName = sessionManager.getEmpresaData()?.db_name ?: return@withContext Pair(false, null)
//                    val empresaDbName = sessionManager.getEmpresaDbName()
                    Log.d("ParteDiarioViewModel", "empresaDbName: $empresaDbName")

                    val requestBody = FormBody.Builder()
                        .add("fecha", parteDiario.fecha)
                        .add("equipoId", parteDiario.equipoId.toString())
                        .add("horasInicio", parteDiario.horasInicio.toString())
                        .add("horasFin", parteDiario.horasFin.toString())
                        .add("horasTrabajadas", parteDiario.horasTrabajadas.toString())
                        .add("observaciones", parteDiario.observaciones ?: "")
                        .add("obraId", parteDiario.obraId.toString())
                        .add("userCreated", parteDiario.userCreated.toString())
                        .add("estadoId", parteDiario.estadoId.toString())
                        .add("empresaDbName", empresaDbName)  // Agrega empresaDbName al cuerpo de la solicitud
                        .build()

                    val request = Request.Builder()
                        .url("$baseUrl$guardarParteDiarioUrl")
                        .post(requestBody)
                        .build()

                    Log.d("ParteDiarioPagingSource", "Request URL: \"$baseUrl${guardarParteDiarioUrl}\"")

                    Log.d("ParteDiarioViewModel", "Enviando datos al servidor: $requestBody")

                    val response = client.newCall(request).execute()
                    return@withContext handleResponse(response)
                } catch (e: IOException) {
                    Pair(false, null)
                }
            }
        }

        private fun handleResponse(response: Response): Pair<Boolean, Int?> {
            val responseBody = response.body?.string()
            Log.d("ParteDiarioViewModel", "Respuesta del servidor: ${response.code} - $responseBody")

            return if (response.isSuccessful) {
                val jsonResponse = responseBody?.let { JSONObject(it) }
                val success = jsonResponse?.getBoolean("success") ?: false
                val newId = jsonResponse?.optInt("id")
                Pair(success, newId)
            } else {
                Log.e("ParteDiarioViewModel", "Error en la respuesta del servidor: ${response.code} - $responseBody")
                Pair(false, null)
            }
        }
    }

    private val api = NuevoParteDiarioApi()

    fun setFilter(equipo: String, fechaInicio: String, fechaFin: String) {
        Log.d("ParteDiarioViewModel", "Setting filter - Equipo: $equipo, FechaInicio: $fechaInicio, FechaFin: $fechaFin")
        _filterEquipo.value = equipo
        _filterFechaInicio.value = fechaInicio
        _filterFechaFin.value = fechaFin
    }

    fun guardarParteDiario(parteDiario: ParteDiario, callback: (Boolean, Int?) -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val fechaConvertida = convertirFecha(parteDiario.fecha)
                val parteDiarioConvertido = parteDiario.copy(fecha = fechaConvertida)
//                val (resultado, nuevoId) = withContext(Dispatchers.IO) {
//                    guardarParteDiarioEnBaseDeDatos(parteDiarioConvertido)
//                }

                val (resultado, nuevoId) = api.guardarParteDiario(parteDiarioConvertido)
                if (resultado) {
                    _mensaje.value = Event("Parte diario guardado con éxito")
                    callback(true, nuevoId)
                } else {
                    _error.value = Event("Error al guardar el parte diario")
                    callback(false, null)
                }
            } catch (e: Exception) {
                _error.value = Event("Error al guardar el parte diario: ${e.message}")
                callback(false, null)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun convertirFecha(fechaOriginal: String): String {
        val parts = fechaOriginal.split("/")
        return if (parts.size == 3) {
            "${parts[2]}/${parts[1]}/${parts[0]}"
        } else {
            fechaOriginal
        }
    }

    fun clearPartesList() {
        _partesList.value = emptyList()
    }

    fun removeParte(position: Int) {
        val currentList = _partesList.value.orEmpty().toMutableList()
        currentList.removeAt(position)
        _partesList.value = currentList
    }
}