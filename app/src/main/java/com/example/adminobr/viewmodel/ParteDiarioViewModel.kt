package com.example.adminobr.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.*

import android.util.Log
import androidx.lifecycle.*
import com.example.adminobr.api.ApiService
import com.example.adminobr.data.ParteDiario
import com.example.adminobr.repository.ParteDiarioRepository
import com.example.adminobr.ui.partediario.ParteDiarioPagingSource
import com.example.adminobr.utils.Event
import com.example.adminobr.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ParteDiarioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ParteDiarioRepository(ApiService.create(), SessionManager(application))

    private val _errorMessage = MutableLiveData<Event<String>>()
    val errorMessage: LiveData<Event<String>> get() = _errorMessage

    private val _mensaje = MutableLiveData<Event<String>>()
    val mensaje: LiveData<Event<String>> get() = _mensaje

    private val _recargarListaPartesPorUsuario = MutableLiveData<Boolean>()
    val recargarListaPartesPorUsuario: LiveData<Boolean> get() = _recargarListaPartesPorUsuario

    private val _recargarListaPartes = MutableLiveData<Boolean>()
    val recargarListaPartes: LiveData<Boolean> get() = _recargarListaPartes

    private val _partes = MutableLiveData<List<ParteDiario>?>()
    val partes: LiveData<List<ParteDiario>?> get() = _partes

    private val _ultimosPartes = MutableLiveData<List<ParteDiario>?>()
    val ultimosPartes: LiveData<List<ParteDiario>?> get() = _ultimosPartes

    private val _ultimoPartePorEquipo = MutableLiveData<ParteDiario?>()
    val ultimoPartePorEquipo: LiveData<ParteDiario?> get() = _ultimoPartePorEquipo

    private val apiService = ApiService.create()
    private val sessionManager = SessionManager(application)
    private val empresaDbName = sessionManager.getEmpresaData()?.db_name ?: ""

    private val filterState = MutableStateFlow(ParteDiarioFilter())

    val partesDiarios: Flow<PagingData<ParteDiario>> = filterState.flatMapLatest { filter ->
        Log.d("ParteDiarioViewModel", "Filtrando con Equipo: ${filter.equipoId}, FechaInicio: ${filter.fechaInicio}, FechaFin: ${filter.fechaFin}")
        Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = {
                ParteDiarioPagingSource(
                    apiService,
                    empresaDbName,
                    filter.equipoId,
                    filter.fechaInicio,
                    filter.fechaFin
                )
            }
        ).flow.cachedIn(viewModelScope)
    }

    fun updateFilters(equipoId: Int, fechaInicio: String, fechaFin: String) {
        filterState.value = ParteDiarioFilter(equipoId, fechaInicio, fechaFin)
    }

    data class ParteDiarioFilter(
        val equipoId: Int = 0,
        val fechaInicio: String = "",
        val fechaFin: String = ""
    )

    fun obtenerUltimoPartePorEquipo(equipoId: Int) {
        viewModelScope.launch {
            Log.d("ParteDiarioViewModel", "Iniciando obtenerUltimoPartePorEquipo con equipoId: $equipoId")
            val result = repository.getUltimoPartePorEquipo(equipoId)
            Log.d("ParteDiarioViewModel", "Resultado de obtenerUltimoPartePorEquipo: $result")
            if (result.isSuccess) {
                _ultimoPartePorEquipo.value = result.getOrNull()
            } else {
                _ultimoPartePorEquipo.value = null
            }
        }
    }

    fun crearParteDiario(parte: ParteDiario, callback: (Boolean, Int?) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repository.createParteDiario(parte) }
            if (result.isSuccess) {
                val (success, nuevoId) = result.getOrNull() ?: Pair(false, null)
                Log.d("UsuarioViewModel", "Callback de creación: success=$success, nuevoId=$nuevoId")
                if (success) {
                    _mensaje.value = Event("Parte diario creado exitosamente")
                    callback(true, nuevoId) // Llamada a la función de callback
                    // Cargar partes después de crear uno nuevo
                    cargarPartes()
                } else {
                    _errorMessage.value = Event("Error al crear parte")
                    callback(false, null)
                }
            } else {
                _errorMessage.value = Event("Error al crear parte: ${result.exceptionOrNull()?.message}")
                callback(false, null)
            }
        }
    }

    fun actualizarParteDiario(parte: ParteDiario, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { repository.updateParteDiario(parte) } // Realiza la operación de actualización en el repositorio
                if (result.isSuccess) {
                    _mensaje.postValue(Event("Parte actualizado exitosamente"))
                    callback(true) // Notifica éxito
                } else {
                    _errorMessage.postValue(Event("Error al actualizar parte: ${result.exceptionOrNull()?.message}"))
                    callback(false) // Notifica error
                }
            } catch (e: Exception) {
                _errorMessage.postValue(Event("Error al actualizar parte: ${e.message}"))
                callback(false) // Notifica error
            }
        }
    }

//
//    fun actualizarParteDiario(parte: ParteDiario) {
//        viewModelScope.launch {
//            val result = withContext(Dispatchers.IO) { repository.updateParteDiario(parte) }
//            if (result.isSuccess) {
//                _mensaje.value = Event("Parte actualizado exitosamente")
//            } else {
//                _errorMessage.value = Event("Error al actualizar parte: ${result.exceptionOrNull()?.message}")
//            }
//        }
//    }

    fun eliminarParteDiario(idParteDiario: Int, origen: String) {
        viewModelScope.launch {
            val result = repository.deleteParteDiario(idParteDiario)

            if (result.isSuccess) {
                _mensaje.value = Event("Parte eliminado correctamente")

                // Emitir el evento específico según el fragmento de origen
                if (origen == "form") {
                    _recargarListaPartesPorUsuario.value = true
                } else if (origen == "listar") {
                    _recargarListaPartes.value = true
                }
            } else {
                val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                _errorMessage.value = Event("Error al eliminar parte: $error")
            }
        }
    }


    fun obtenerParteDiarioPorId(idParteDiario: Int) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repository.fetchParteById(idParteDiario) }
            if (result.isSuccess) {
                result.getOrNull()?.let { _partes.value = listOf(it) }
            } else {
                _errorMessage.value = Event("Error al obtener parte: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun cargarPartes(parteFiltro: String = "") {
        viewModelScope.launch {
            Log.d("ParteDiarioViewModel", "Iniciando carga de parte con filtro '$parteFiltro'...")
            val result = withContext(Dispatchers.IO) { repository.fetchPartes(parteFiltro) }

            if (result.isSuccess) {
                _partes.value = result.getOrNull()
                Log.d("ParteDiarioViewModel", "Partes cargados correctamente.")
            } else {
                val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                _errorMessage.value = Event("Error al cargar partes: $error")
                Log.e("ParteDiarioViewModel", "Error al cargar partes: $error")
            }
        }
    }

    fun cargarUltimosPartesPorUsuario(userId: Int) {
        viewModelScope.launch {
            Log.d("ParteDiarioViewModel", "Cargando últimos partes del usuario con ID: $userId")
            val result = repository.getUltimosPartesPorUsuario(userId)

            if (result.isSuccess) {
                _ultimosPartes.value = result.getOrNull()
                Log.d("ParteDiarioViewModel", "Datos cargados: ${_ultimosPartes.value?.size} partes.")
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Error desconocido"
                _errorMessage.value = Event("Error al cargar partes: $errorMessage")
                Log.e("ParteDiarioViewModel", "Error al cargar partes: $errorMessage")
            }
        }
    }


    // Método loadPartes para cargar partes con filtro de búsqueda
    fun loadPartes(parteFiltro: String = "") {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val empresaDbName = repository.sessionManager.getEmpresaData()?.db_name ?: return@withContext

                    val jsonObject = JSONObject().apply {
                        put("empresaDbName", empresaDbName)
                        if (parteFiltro.isNotEmpty()) {
                            put("parteFiltro", parteFiltro)
                        }
                    }

                    val requestBody = jsonObject.toString()
                        .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

                    val response = repository.apiService.getAllPartesDiarios(requestBody)

                    if (response.isSuccessful) {
                        val partes = response.body()?.data ?: emptyList()
                        _partes.postValue(partes)
                        Log.d("ParteDiarioViewModel", "Partes cargados con filtro correctamente.")
                    } else {
                        _errorMessage.postValue(Event("Error al obtener partes: ${response.code()}"))
                    }
                } catch (e: Exception) {
                    _errorMessage.postValue(Event("Error: ${e.message}"))
                }
            }
        }
    }

    fun resetRecargarListaPartesPorUsuario() {
        _recargarListaPartesPorUsuario.value = false
    }

    fun resetRecargarListaPartes() {
        _recargarListaPartes.value = false
    }

}
