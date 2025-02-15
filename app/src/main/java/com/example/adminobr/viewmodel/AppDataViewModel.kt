package com.example.adminobr.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.adminobr.api.AutocompletesApi
import com.example.adminobr.data.Equipo
import com.example.adminobr.data.Obra
import com.example.adminobr.data.Empresa
import com.example.adminobr.data.Estado
import com.example.adminobr.data.Rol
import com.example.adminobr.utils.Event
import com.example.adminobr.utils.SessionManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AppDataViewModel(application: Application) : AndroidViewModel(application) {

    private val _equipos = MutableLiveData<List<Equipo>>()
    val equipos: LiveData<List<Equipo>> = _equipos

    private val _obras = MutableLiveData<List<Obra>>()
    val obras: LiveData<List<Obra>> = _obras

    private val _empresas = MutableLiveData<List<Empresa>>()
    val empresas: LiveData<List<Empresa>> = _empresas

    private val _roles = MutableLiveData<List<Rol>>()
    val roles: LiveData<List<Rol>> = _roles

    private val _estados = MutableLiveData<List<Estado>>()
    val estados: LiveData<List<Estado>> = _estados

    private val _tipoCombustible = MutableLiveData<List<String>>()
    val tipoCombustible: LiveData<List<String>> get() = _tipoCombustible

    private val _errorObras = MutableLiveData<Event<String>>()
    val errorObras: LiveData<Event<String>> = _errorObras

    private val api: AutocompletesApi = AutocompletesApi.create(application)
    private val sessionManager = SessionManager(application)

    private fun createRequestBody(): String {
        val json = JSONObject()
        val empresaDbName = sessionManager.getEmpresaData()["empresaDbName"]

        json.put("empresaDbName", empresaDbName)
        return json.toString()
    }

    fun cargarEquipos() {
        viewModelScope.launch {
            try {
                val requestBody = createRequestBody().toRequestBody("application/json".toMediaTypeOrNull())
                val response = api.getEquipos(requestBody)
                _equipos.value = response
                Log.d("AppDataViewModel", "Equipos cargados: ${_equipos.value}")
            } catch (e: Exception) {
                Log.e("AppDataViewModel", "Error al cargar equipos: ${e.message}")
            }
        }
    }

    fun cargarObras(empresaDbName: String? = null, filterEstado: Boolean = false) {
        viewModelScope.launch {
            try {
                val dbName = empresaDbName ?: sessionManager.getEmpresaData()["empresaDbName"]

                if (!dbName.isNullOrEmpty()) {
                    val requestBody = JSONObject().apply {
                        put("empresaDbName", dbName)
                        if (filterEstado) {
                            put("filterEstado", true)
                        }
                    }.toString().toRequestBody("application/json".toMediaTypeOrNull())

                    val response = api.getObras(requestBody) // Llamada a la API para obtener obras
                    _obras.value = response
                    _errorObras.value = Event(null) // Limpia el error tras un éxito
                    Log.d("AppDataViewModel", "Obras cargadas: ${_obras.value}")
                } else {
                    _errorObras.value = Event("No se pudo cargar obras: empresaDbName no disponible")
                    Log.e("AppDataViewModel", "No se pudo cargar obras: empresaDbName no disponible")
                }
            } catch (e: Exception) {
                _errorObras.value = Event("Error al cargar obras")
                Log.e("AppDataViewModel", "Error al cargar obras: ${e.message}")
            }
        }
    }

    fun cargarRoles() {
        viewModelScope.launch {
            try {
                val requestBody =
                    createRequestBody().toRequestBody("application/json".toMediaTypeOrNull())
                val response = api.getRoles(requestBody)
                _roles.value = response
                Log.d("AppDataViewModel", "Roles cargados: ${_roles.value}")
            } catch (e: Exception) {
                Log.e("AppDataViewModel", "Error al cargar roles: ${e.message}")
            }
        }
    }

    fun cargarEstados() {
        viewModelScope.launch {
            try {
                val requestBody =
                    createRequestBody().toRequestBody("application/json".toMediaTypeOrNull())
                val response = api.getEstados(requestBody)
                _estados.value = response
                Log.d("AppDataViewModel", "Estados cargados: ${_estados.value}")
            } catch (e: Exception) {
                Log.e("AppDataViewModel", "Error al cargar estados: ${e.message}")
            }
        }
    }

    fun cargarTipoCombustible() {
        // Cargar datos fijos de tipo de combustible
        _tipoCombustible.value = listOf("Diesel", "Nafta")
    }
}

