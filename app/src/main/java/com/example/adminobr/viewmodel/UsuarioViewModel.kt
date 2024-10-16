package com.example.adminobr.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.example.adminobr.utils.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.adminobr.data.Usuario
import com.example.adminobr.utils.Constants
import com.example.adminobr.utils.SessionManager
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class UsuarioViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _mensaje = MutableLiveData<Event<String>>() // Event wrapper for messages
    val mensaje: LiveData<Event<String>> get() = _mensaje

    private val _error = MutableLiveData<Event<String>>()  // Event wrapper for errors
    val error: LiveData<Event<String>> get() = _error

    private val _users = MutableLiveData<List<Usuario>>()
    val users: LiveData<List<Usuario>> = _users

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            try {
                val empresaDbName = sessionManager.getEmpresaData()?.db_name ?: return@launch
                val response = api.obtenerUsuarios(empresaDbName) // Ensure api is initialized correctly
                if (response.isSuccessful) {
                    val users = parseUsers(response) // Implement parseUsers as necessary
                    _users.postValue(users)
                } else {
                    _error.postValue(Event("Error al obtener usuarios: ${response.code}"))
                }
            } catch (e: Exception) {
                _error.postValue(Event("Error: ${e.message}"))
            }
        }
    }

    private val api = UsuarioApi()

    private inner class UsuarioApi {
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

        suspend fun obtenerUsuarios(empresaDbName: String): Response {
            return withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url("${baseUrl}${Constants.Usuarios.GET_LISTA}?empresaDbName=$empresaDbName")
                    .build()
                client.newCall(request).execute()
            }
        }
    }

    private fun handleResponse(response: Response): Pair<Boolean, Int?> {
                    return try {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d("NuevoUsuarioViewModel", "Respuesta del servidor: ${response.code} - $responseBody")

                    val jsonResponse = responseBody?.let { JSONObject(it) }
                    val success = jsonResponse?.getBoolean("success") ?: false

                    // Verificar si success es false y hay un mensaje de error
                    if (!success && jsonResponse?.has("message") == true) {
                        val errorMessage = jsonResponse.getString("message")
                        _error.postValue(Event(errorMessage)) // Mostrar el mensaje de error en el Toast
                        return Pair(false, null)
                    }

                    // Obtener newId si success es true
                    val newId = if (success) {
                        jsonResponse?.optInt("id")
                    } else {
                        null
                    }

                    Pair(success, newId)
                } else {
                    Log.e("NuevoUsuarioViewModel", "Error en la respuesta del servidor: ${response.code}")
                    Pair(false, null)
                }
            } catch (e: JSONException) {
                Log.e("NuevoUsuarioViewModel", "Error al parsear la respuesta JSON: ${e.message}")
                Pair(false, null)
            } catch (e: IOException) {
                Log.e("NuevoUsuarioViewModel", "Error de conexión: ${e.message}")
                Pair(false, null)
            }
        }

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

    private fun parseUsers(response: Response): List<Usuario> {
        val userList = mutableListOf<Usuario>()
        try {
            val responseBody = response.body?.string()
            Log.d("UsuarioViewModel", "ResponseBody: $responseBody") // Log del ResponseBody
            val jsonArray = JSONObject(responseBody).getJSONArray("usuarios")
            Log.d("UsuarioViewModel", "JsonArray length: ${jsonArray.length()}") // Log del tamaño del JsonArray
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                Log.d("UsuarioViewModel", "JsonObject: $jsonObject") // Log del JsonObject actual
                val usuario = Usuario(
                    id = jsonObject.getInt("id"),
                    legajo = jsonObject.getString("legajo"),
                    email = jsonObject.getString("email"),
                    dni = jsonObject.getString("dni"),
                    nombre = jsonObject.getString("nombre"),
                    apellido = jsonObject.getString("apellido"),
                    telefono = jsonObject.getString("telefono"),
                    estadoId = jsonObject.getInt("estado_id"),
                    userCreated = jsonObject.getInt("userCreated"),
                    password = ""
                )
                userList.add(usuario)
            }
        } catch (e: JSONException) {
            Log.e("UsuarioViewModel", "Error al parsear JSON: ${e.message}", e) // Log del error con la excepción
        } catch (e: IOException) {
            Log.e("UsuarioViewModel", "Error de E/S: ${e.message}", e) // Log del error con la excepción
        }
        Log.d("UsuarioViewModel", "UserList size: ${userList.size}") // Log del tamaño de la lista de usuarios
        return userList
    }
}

// Factory for the ViewModel
class UsuarioViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UsuarioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UsuarioViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}



//package com.example.adminobr.viewmodel
//
//import android.app.Application
//import android.util.Log
//import androidx.activity.result.launch
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//
//import com.example.adminobr.utils.Event
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import androidx.lifecycle.*
//import com.example.adminobr.data.Usuario
//import com.example.adminobr.utils.Constants
//import com.example.adminobr.utils.SessionManager
//
//import okhttp3.FormBody
//import okhttp3.OkHttpClient
//import okhttp3.Request
//import okhttp3.Response
//import org.json.JSONException
//import org.json.JSONObject
//import java.io.IOException
//
//class UsuarioViewModel(application: Application) : AndroidViewModel(application) {
//
//    private val sessionManager = SessionManager(application)
//
//    private val _isLoading = MutableLiveData<Boolean>()
//    val isLoading: LiveData<Boolean> get() = _isLoading
//
//    private val _mensaje = MutableLiveData<String>()
//    val mensaje: LiveData<String> get() = _mensaje
//
//    private val _error = MutableLiveData<String>()
//    val error: LiveData<String> get() = _error
//
//    private val _users = MutableLiveData<List<Usuario>>()
//    val users: LiveData<List<Usuario>> = _users
//
//    init {
//        loadUsers()
//    }
//
//    private fun loadUsers() {
//        viewModelScope.launch {
//            try {
//                val empresaDbName = sessionManager.getEmpresaData()?.db_name ?: return@launch
//                val response = api.obtenerUsuarios(empresaDbName)
//                if (response.isSuccessful) {
//                    val users = parseUsers(response)
//                    _users.postValue(users)
//                } else {
//                    _error.postValue(Event("Error al obtener usuarios: ${response.code}").toString())
//                }
//            } catch (e: Exception) {
//                _error.postValue(Event("Error al obtener usuarios: ${e.message}").toString())
//            }
//        }
//    }
//
//    private suspend fun parseUsers(response: Response): List<Usuario> {
//        val users = mutableListOf<Usuario>()
//        val responseBody = response.body?.string()
//        val jsonArray = JSONObject(responseBody).getJSONArray("usuarios")
//        for (i in 0 until jsonArray.length()) {
//            val jsonObject = jsonArray.getJSONObject(i)
//            val user = Usuario(
//                id = jsonObject.getInt("id"),
//                legajo = jsonObject.getString("legajo"),
//                email = jsonObject.getString("email"),
//                dni = jsonObject.getString("dni"),
//                nombre = jsonObject.getString("nombre"),
//                apellido = jsonObject.getString("apellido"),
//                telefono = jsonObject.getString("telefono"),
//                estadoId = jsonObject.getInt("estado_id"),
//                userCreated = jsonObject.getInt("userCreated"),
//                password = ""
//            )
//            users.add(user)
//        }
//        return users
//    }
//
//    private val api = UsuarioApi()
//
//    private inner class UsuarioApi {
//        private val client = OkHttpClient.Builder().build()
//        private val baseUrl = Constants.getBaseUrl() //"http://adminobr.site/"
//        private val guardarUsuarioUrl = Constants.Usuarios.GUARDAR
//
//
//        suspend fun guardarNuevoUsuario(nuevoUsuario: Usuario): Pair<Boolean, Int?> {
//            return withContext(Dispatchers.IO) {
//                try{
//                    // Obtén empresaDbName de SessionManager
//                    val empresaDbName = sessionManager.getEmpresaData()?.db_name ?: return@withContext Pair(false, null)
//
//                    val requestBody = FormBody.Builder()
//                        .add("legajo", nuevoUsuario.legajo)
//                        .add("email", nuevoUsuario.email)
//                        .add("dni", nuevoUsuario.dni)
//                        .add("password", nuevoUsuario.password)
//                        .add("nombre", nuevoUsuario.nombre)
//                        .add("apellido", nuevoUsuario.apellido)
//                        .add("telefono", nuevoUsuario.telefono)
//                        .add("userCreated", nuevoUsuario.userCreated.toString())
//                        .add("estado_id", nuevoUsuario.estadoId.toString())
//                        .add("empresaDbName", empresaDbName)  // Agrega empresaDbName al cuerpo de la solicitud
//                        .build()
//
//                    val request = Request.Builder()
//                        .url("$baseUrl$guardarUsuarioUrl")
//                        .post(requestBody)
//                        .build()
//
//                    val response = client.newCall(request).execute()
//                    return@withContext handleResponse(response)
//                } catch (e: IOException) {
//                    Pair(false, null)
//                }
//            }
//        }
//
//        suspend fun actualizarUsuario(usuario: Usuario, newPassword: String?): Boolean {
//            return withContext(Dispatchers.IO) {
//                try {
//                    val empresaDbName = sessionManager.getEmpresaData()?.db_name ?: return@withContext false
//
//                    val requestBody = FormBody.Builder()
//                        .add("id", usuario.id.toString())
//                        .add("legajo", usuario.legajo)
//                        .add("email", usuario.email)
//                        .add("dni", usuario.dni)
//                        .add("nombre", usuario.nombre)
//                        .add("apellido", usuario.apellido)
//                        .add("telefono", usuario.telefono)
//                        .add("empresaDbName", empresaDbName)
//
//                    // Agregar la nueva contraseña si se proporciona
//                    if (!newPassword.isNullOrEmpty()) {
//                        requestBody.add("newPassword", newPassword)
//                    }
//
//                    val request = Request.Builder()
//                        .url("$baseUrl${Constants.Usuarios.ACTUALIZAR}") // Usar la URL para actualizar
//                        .post(requestBody.build())
//                        .build()
//
//                    val response = client.newCall(request).execute()
//
//                    // Manejar la respuesta para actualizar (solo necesitamos éxito o fracaso)
//                    return@withContext if (response.isSuccessful) {
//                        val responseBody = response.body?.string()
//                        val jsonResponse = responseBody?.let { JSONObject(it) }
//                        jsonResponse?.getBoolean("success") ?: false
//                    } else {
//                        false
//                    }
//                } catch (e: IOException) {
//                    false
//                }
//            }
//        }
//
//        suspend fun obtenerUsuarios(empresaDbName: String): Response {
//            return withContext(Dispatchers.IO) {
//                val request = Request.Builder()
//                    .url("${baseUrl}${Constants.Usuarios.GET_LISTA}?empresaDbName=$empresaDbName")
//                    .build()
//                client.newCall(request).execute()
//            }
//        }
//    }
////        private fun handleResponse(response: Response): Pair<Boolean, Int?> {
////            return if (response.isSuccessful) {
////                val responseBody = response.body?.string()
////                Log.d("NuevoUsuarioViewModel", "Respuesta del servidor: ${response.code} - $responseBody")
////
////                val jsonResponse = responseBody?.let { JSONObject(it) }
////                val success = jsonResponse?.getBoolean("success") ?: false
////                val newId = jsonResponse?.optInt("id")
////                Pair(success, newId)
////            } else {
////                Log.e("NuevoUsuarioViewModel", "Error en la respuesta del servidor: ${response.code}")
////                Pair(false, null)
////            }
////        }
//
//        private fun handleResponse(response: Response): Pair<Boolean, Int?> {
//            return try {
//                if (response.isSuccessful) {
//                    val responseBody = response.body?.string()
//                    Log.d("NuevoUsuarioViewModel", "Respuesta del servidor: ${response.code} - $responseBody")
//
//                    val jsonResponse = responseBody?.let { JSONObject(it) }
//                    val success = jsonResponse?.getBoolean("success") ?: false
//
//                    // Verificar si success es false y hay un mensaje de error
//                    if (!success && jsonResponse?.has("message") == true) {
//                        val errorMessage = jsonResponse.getString("message")
//                        _error.postValue(Event(errorMessage)) // Mostrar el mensaje de error en el Toast
//                        return Pair(false, null)
//                    }
//
//                    // Obtener newId si success es true
//                    val newId = if (success) {
//                        jsonResponse?.optInt("id")
//                    } else {
//                        null
//                    }
//
//                    Pair(success, newId)
//                } else {
//                    Log.e("NuevoUsuarioViewModel", "Error en la respuesta del servidor: ${response.code}")
//                    Pair(false, null)
//                }
//            } catch (e: JSONException) {
//                Log.e("NuevoUsuarioViewModel", "Error al parsear la respuesta JSON: ${e.message}")
//                Pair(false, null)
//            } catch (e: IOException) {
//                Log.e("NuevoUsuarioViewModel", "Error de conexión: ${e.message}")
//                Pair(false, null)
//            }
//        }
//    }
//
//
//fun guardarNuevoUsuario(nuevoUsuario: Usuario, callback: (Boolean, Int?) -> Unit) {
//    _isLoading.value = true
//    viewModelScope.launch {
//        try {
//            val (resultado, nuevoId) = api.guardarNuevoUsuario(nuevoUsuario)
//            if (resultado) {
//                _mensaje.value = Event("Nuevo usuario guardado con éxito")
//                callback(true, nuevoId)
//            } else {
//                _error.value = Event("Error al guardar el nuevo usuario")
//                callback(false, null)
//            }
//        } catch (e: Exception) {
//            _error.value = Event("Error al guardar el nuevo usuario: ${e.message}")
//            callback(false, null)
//        } finally {
//            _isLoading.value = false
//        }
//    }
//}
//
//fun actualizarUsuario(usuario: Usuario, newPassword: String?, callback: (Boolean) -> Unit) {
//    _isLoading.value = true
//    viewModelScope.launch {
//        try {
//            val resultado = api.actualizarUsuario(usuario, newPassword)
//            if (resultado) {
//                _mensaje.value = Event("Usuario actualizado con éxito")
//                callback(true)
//            } else {
//                _error.value = Event("Error al actualizar el usuario")
//                callback(false)
//            }
//        } catch (e: Exception) {
//            _error.value = Event("Error al actualizar el usuario: ${e.message}")
//            callback(false)
//        } finally {
//            _isLoading.value = false
//        }
//    }
//}
//
//
//
//}
//
//// Factory para el ViewModel
//class UsuarioViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(UsuarioViewModel::class.java)) {
//            @Suppress("UNCHECKED_CAST")
//            return UsuarioViewModel(application) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel class")
//    }
//}
