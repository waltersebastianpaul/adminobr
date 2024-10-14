package com.example.adminobr.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.example.adminobr.utils.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.*
import com.example.adminobr.data.Usuario
import com.example.adminobr.utils.Constants
import com.example.adminobr.utils.SessionManager

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class NuevoUsuarioViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading


    private val _error = MutableLiveData<Event<String>>()
    val error: LiveData<Event<String>> = _error


    private val _mensaje = MutableLiveData<Event<String>>()
    val mensaje: LiveData<Event<String>> = _mensaje


    private inner class NuevoUsuarioApi {
        private val client = OkHttpClient.Builder().build()
        private val baseUrl = Constants.getBaseUrl() //"http://adminobr.site/"
        private val guardarUsuarioUrl = Constants.Usuarios.GUARDAR


        suspend fun guardarNuevoUsuario(nuevoUsuario: Usuario): Pair<Boolean, Int?> {
            return withContext(Dispatchers.IO) {
                try{
                    // Obtén empresaDbName de SessionManager
                    val empresaDbName = sessionManager.getEmpresaData()?.db_name ?: return@withContext Pair(false, null)

                    val requestBody = FormBody.Builder()
                        .add("legajo", nuevoUsuario.legajo)
                        .add("email", nuevoUsuario.email)
                        .add("dni", nuevoUsuario.dni)
                        .add("password", nuevoUsuario.password)
                        .add("nombre", nuevoUsuario.nombre)
                        .add("apellido", nuevoUsuario.apellido)
                        .add("telefono", nuevoUsuario.telefono)
                        .add("userCreated", nuevoUsuario.userCreated.toString())
                        .add("estado_id", nuevoUsuario.estadoId.toString())
                        .add("empresaDbName", empresaDbName)  // Agrega empresaDbName al cuerpo de la solicitud
                        .build()

                    val request = Request.Builder()
                        .url("$baseUrl$guardarUsuarioUrl")
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    return@withContext handleResponse(response)
                } catch (e: IOException) {
                    Pair(false, null)
                }
            }
        }

        suspend fun actualizarUsuario(usuario: Usuario, newPassword: String?): Boolean {
            return withContext(Dispatchers.IO) {
                try {
                    val empresaDbName = sessionManager.getEmpresaData()?.db_name ?: return@withContext false

                    val requestBody = FormBody.Builder()
                        .add("id", usuario.id.toString())
                        .add("legajo", usuario.legajo)
                        .add("email", usuario.email)
                        .add("dni", usuario.dni)
                        .add("nombre", usuario.nombre)
                        .add("apellido", usuario.apellido)
                        .add("telefono", usuario.telefono)
                        .add("empresaDbName", empresaDbName)

                    // Agregar la nueva contraseña si se proporciona
                    if (!newPassword.isNullOrEmpty()) {
                        requestBody.add("newPassword", newPassword)
                    }

                    val request = Request.Builder()
                        .url("$baseUrl${Constants.Usuarios.ACTUALIZAR}") // Usar la URL para actualizar
                        .post(requestBody.build())
                        .build()

                    val response = client.newCall(request).execute()

                    // Manejar la respuesta para actualizar (solo necesitamos éxito o fracaso)
                    return@withContext if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        val jsonResponse = responseBody?.let { JSONObject(it) }
                        jsonResponse?.getBoolean("success") ?: false
                    } else {
                        false
                    }
                } catch (e: IOException) {
                    false
                }
            }
        }

        private fun handleResponse(response: Response): Pair<Boolean, Int?> {
            return if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d("NuevoUsuarioViewModel", "Respuesta del servidor: ${response.code} - $responseBody")

                val jsonResponse = responseBody?.let { JSONObject(it) }
                val success = jsonResponse?.getBoolean("success") ?: false
                val newId = jsonResponse?.optInt("id")
                Pair(success, newId)
            } else {
                Log.e("NuevoUsuarioViewModel", "Error en la respuesta del servidor: ${response.code}")
                Pair(false, null)
            }
        }
    }


    private val api = NuevoUsuarioApi()


    fun guardarNuevoUsuario(nuevoUsuario: Usuario, callback: (Boolean, Int?) -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val (resultado, nuevoId) = api.guardarNuevoUsuario(nuevoUsuario)
                if (resultado) {
                    _mensaje.value = Event("Nuevo usuario guardado con éxito")
                    callback(true, nuevoId)
                } else {
                    _error.value = Event("Error al guardar el nuevo usuario")
                    callback(false, null)
                }
            } catch (e: Exception) {
                _error.value = Event("Error al guardar el nuevo usuario: ${e.message}")
                callback(false, null)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun actualizarUsuario(usuario: Usuario, newPassword: String?, callback: (Boolean) -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val resultado = api.actualizarUsuario(usuario, newPassword)
                if (resultado) {
                    _mensaje.value = Event("Usuario actualizado con éxito")
                    callback(true)
                } else {
                    _error.value = Event("Error al actualizar el usuario")
                    callback(false)
                }
            } catch (e: Exception) {
                _error.value = Event("Error al actualizar el usuario: ${e.message}")
                callback(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

}

// Factory para el ViewModel
class NuevoUsuarioViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NuevoUsuarioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NuevoUsuarioViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
