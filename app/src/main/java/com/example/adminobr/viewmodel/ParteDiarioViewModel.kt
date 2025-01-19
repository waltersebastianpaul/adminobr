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
import com.example.adminobr.utils.Event
import com.example.adminobr.utils.NetworkStatusHelper
import com.example.adminobr.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val empresaDbName = sessionManager.getEmpresaData()["empresaDbName"] ?: ""

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val filterState = MutableStateFlow(ParteDiarioFilter())

    // Nuevo LiveData para el estado de la conexión a internet
    private var _isNetworkAvailable = MutableLiveData<Boolean>()
    val isNetworkAvailable: LiveData<Boolean> get() = _isNetworkAvailable

    init {
        // Inicializar el estado de la conexión a internet
        _isNetworkAvailable.value = NetworkStatusHelper.isConnected()
    }

    // Flujo que emite los datos paginados
    val partesDiarios: Flow<PagingData<ParteDiario>> = filterState.flatMapLatest { filter ->
        if (!NetworkStatusHelper.isConnected()) {
            // Emitir un flujo vacío y registrar un error si no hay conexión
            Log.e("ParteDiarioViewModel", "No hay conexión a internet. No se puede cargar datos paginados.")
            _errorMessage.postValue(Event("No hay conexión a internet, intenta más tarde"))
            flowOf(PagingData.empty())
        } else {
            Log.d("ParteDiarioViewModel", "Aplicando filtro: $filter")
            Pager(
                config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                pagingSourceFactory = {
                    repository.getPagingSource(
                        filter.equipoId,
                        filter.obraId,
                        filter.fechaInicio,
                        filter.fechaFin
                    )
                }
            ).flow.cachedIn(viewModelScope)
        }
    }

    // Actualizar filtros desde el fragmento
    fun updateFilters(
        equipoId: Int,
        obraId: Int,
        fechaInicio: String,
        fechaFin: String
    ) {
        if (!NetworkStatusHelper.isConnected()) {
            // Emite un error específico en el errorMessage si no hay conexión
            _errorMessage.value = Event("No hay conexión a internet, intenta nuevamente mas tarde")
            return
        }
        val newFilter = ParteDiarioFilter(
            equipoId = equipoId,
            obraId = obraId,
            fechaInicio = fechaInicio,
            fechaFin = fechaFin
        )
        Log.d("ParteDiarioViewModel", "Actualizando filtros: $newFilter")
        filterState.value = newFilter
    }

    // Clase contenedora para los filtros
    data class ParteDiarioFilter(
        val equipoId: Int = 0,
        val obraId: Int = 0,
        val fechaInicio: String = "",
        val fechaFin: String = ""
    )

    fun obtenerUltimoPartePorEquipo(
        equipoId: Int
    ) {
        if (!NetworkStatusHelper.isConnected()) {
            _errorMessage.value = Event("No hay conexión a internet, intenta más tarde")
            return
        }

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

    fun crearParteDiario(
        parte: ParteDiario,
        callback: (Boolean, Int?) -> Unit
    ) {
        Log.d("ParteDiarioViewModel", "Iniciando creación de parte: $parte")
        if (!NetworkStatusHelper.isConnected()) {
            _errorMessage.value = Event("No hay conexión a internet, intenta más tarde")
            callback(false, null)
            return
        }

        _isLoading.value = true // Indica que la carga está en progreso
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repository.createParteDiario(parte) }
            Log.d("ParteDiarioViewModel", "Resultado de creación: $result")
            if (result.isSuccess) {
                val (success, nuevoId) = result.getOrNull() ?: Pair(false, null)
                Log.d("ParteDiarioViewModel", "Parte creado con éxito, nuevoId=$nuevoId")
                Log.d("ParteDiarioViewModel", "Callback de creación: success=$success, nuevoId=$nuevoId")
                if (success) {
                    _mensaje.value = Event("Parte diario creado exitosamente")
                    callback(true, nuevoId) // Llamada a la función de callback
                    // Cargar partes después de crear uno nuevo
                    //cargarPartes()
                } else {
                    _errorMessage.value = Event("Error al crear parte")
                    callback(false, null)
                }
            } else {
                _errorMessage.value = Event("Error al crear parte: ${result.exceptionOrNull()?.message}")
                Log.e("ParteDiarioViewModel", "Error al crear el parte: ${result.exceptionOrNull()?.message}")
                callback(false, null)
            }
            _isLoading.value = false // Indica que la carga ha terminado
        }
    }

    fun actualizarParteDiario(
        parte: ParteDiario,
        callback: (Boolean) -> Unit
    ) {
        if (!NetworkStatusHelper.isConnected()) {
            _errorMessage.value = Event("No hay conexión a internet, intenta más tarde")
            _isNetworkAvailable.value = false
            callback(false)
            return
        }

        _isNetworkAvailable.value = true
        _isLoading.value = true // Indica que la carga está en progreso
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
                _isLoading.value = false // Indica que la carga ha terminado
            } catch (e: Exception) {
                _errorMessage.postValue(Event("Error al actualizar parte: ${e.message}"))
                callback(false) // Notifica error
            }
        }
    }

    fun eliminarParteDiario(
        idParteDiario: Int,
        origen: String
    ) {
        if (!NetworkStatusHelper.isConnected()) {
            _errorMessage.value = Event("No hay conexión a internet, intenta más tarde")
            return
        }

        _isLoading.value = true // Indica que la carga está en progreso
        viewModelScope.launch {
            val result = repository.deleteParteDiario(idParteDiario)
            if (result.isSuccess) {
                _mensaje.value = Event("Parte eliminado correctamente")
                Log.d("ParteDiarioViewModel", "Parte eliminada correctamente - Origen: $origen")
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
            _isLoading.value = false // Indica que la carga ha terminado
        }
    }

    fun obtenerParteDiarioPorId(idParteDiario: Int) {
        if (!NetworkStatusHelper.isConnected()) {
            _errorMessage.value = Event("No hay conexión a internet, intenta más tarde")
            _isLoading.value = false
            return
        }

        _isLoading.value = true // Indica que la carga está en progreso
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repository.fetchParteById(idParteDiario) }
            if (result.isSuccess) {
                result.getOrNull()?.let { _partes.value = listOf(it) }
            } else {
                _errorMessage.value = Event("Error al obtener parte: ${result.exceptionOrNull()?.message}")
            }
            _isLoading.value = false // Indica que la carga ha terminado
        }
    }

    fun cargarUltimosPartesPorUsuario(userId: Int) {
        if (!NetworkStatusHelper.isConnected()) {
            _errorMessage.value = Event("No hay conexión a internet, intenta más tarde")
//            _isLoading.value = false
            return
        }

//        _isLoading.value = true // Indica que la carga está en progreso
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
//            _isLoading.value = false // Indica que la carga ha terminado
        }
    }

    fun resetRecargarListaPartesPorUsuario() {
        _recargarListaPartesPorUsuario.value = false
    }

    fun resetRecargarListaPartes() {
        _recargarListaPartes.value = false
    }

}
