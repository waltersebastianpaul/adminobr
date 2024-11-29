package com.example.adminobr.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.example.adminobr.api.ApiService
import com.example.adminobr.data.Usuario
import com.example.adminobr.repository.UsuarioRepository
import com.example.adminobr.utils.Event
import com.example.adminobr.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class UsuarioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UsuarioRepository(ApiService.create(), SessionManager(application))

    private val _errorMessage = MutableLiveData<Event<String>>()
    val errorMessage: LiveData<Event<String>> get() = _errorMessage

    private val _mensaje = MutableLiveData<Event<String>>()
    val mensaje: LiveData<Event<String>> get() = _mensaje

    private val _users = MutableLiveData<List<Usuario>?>()
    val users: LiveData<List<Usuario>?> get() = _users

    fun crearUsuario(usuario: Usuario, callback: (Boolean, Int?) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repository.createUser(usuario) }
            if (result.isSuccess) {
                val (success, nuevoId) = result.getOrNull() ?: Pair(false, null)
                Log.d("UsuarioViewModel", "Callback de creación: success=$success, nuevoId=$nuevoId")
                if (success) {
                    _mensaje.value = Event("Usuario creado exitosamente")
                    callback(true, nuevoId) // Llamada a la función de callback
                    cargarUsuarios() // Cargar lista actualizada de usuarios
                } else {
                    _errorMessage.value = Event("Error al crear usuario")
                    callback(false, null)
                }
            } else {
                _errorMessage.value = Event("Error al crear usuario: ${result.exceptionOrNull()?.message}")
                callback(false, null)
            }
        }
    }

    fun asignarRol(usuarioId: Int, rolId: Int) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repository.assignRole(usuarioId, rolId) }
            if (result.isSuccess) {
                _mensaje.value = Event("Rol asignado exitosamente")
            } else {
                _errorMessage.value = Event("Error al asignar rol: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun actualizarUsuario(usuario: Usuario, newPassword: String?, callback: (success: Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { repository.updateUser(usuario, newPassword) }
                if (result.isSuccess) {
                    _mensaje.value = Event("Usuario actualizado exitosamente")
                    callback(true) // Notifica éxito al callback
                } else {
                    _errorMessage.value = Event("Error al actualizar usuario: ${result.exceptionOrNull()?.message}")
                    callback(false) // Notifica error al callback
                }
            } catch (e: Exception) {
                _errorMessage.value = Event("Error al actualizar usuario: ${e.message}")
                callback(false) // Notifica error al callback
            }
        }
    }

    fun actualizarPerfilUsuario(usuario: Usuario, nuevaContrasena: String?, callback: (success: Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { repository.updateProfile(usuario, nuevaContrasena) }
                if (result.isSuccess) {
                    _mensaje.value = Event("Perfil actualizado exitosamente")
                    callback(true) // Notifica éxito al callback
                } else {
                    _errorMessage.value = Event("Error al actualizar perfil: ${result.exceptionOrNull()?.message}")
                    callback(false) // Notifica error al callback
                }
            } catch (e: Exception) {
                _errorMessage.value = Event("Error al actualizar perfil: ${e.message}")
                callback(false) // Notifica error al callback
            }
        }
    }

    fun actualizarContrasenaUsuario(usuario: Usuario, nuevaContrasena: String, callback: (success: Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { repository.updatePassword(usuario, nuevaContrasena) }
                if (result.isSuccess) {
                    _mensaje.value = Event("Contraseña actualizada exitosamente")
                    callback(true) // Notifica éxito al callback
                } else {
                    _errorMessage.value = Event("Error al actualizar contraseña: ${result.exceptionOrNull()?.message}")
                    callback(false) // Notifica error al callback
                }
            } catch (e: Exception) {
                _errorMessage.value = Event("Error al actualizar contraseña: ${e.message}")
                callback(false) // Notifica error al callback
            }
        }
    }

    fun eliminarUsuario(idUsuario: Int) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repository.deleteUser(idUsuario) }

            if (result.isSuccess) {
                Log.d("UsuarioViewModel", "Eliminación exitosa: ${result.getOrNull()}")
                _mensaje.value = Event("Usuario eliminado correctamente")
                cargarUsuarios() // Recargar la lista después de la eliminación
            } else {
                val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                Log.e("UsuarioViewModel", "Error al eliminar usuario: $error")
                _errorMessage.value = Event("Error al eliminar usuario: $error")
            }
        }
    }

    fun obtenerUsuarioPorId(idUsuario: Int) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repository.fetchUserById(idUsuario) }
            if (result.isSuccess) {
                result.getOrNull()?.let { _users.value = listOf(it) }
            } else {
                _errorMessage.value = Event("Error al obtener usuario: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun cargarUsuarios(usuarioFiltro: String = "") {
        viewModelScope.launch {
            Log.d("UsuarioViewModel", "Iniciando carga de usuarios con filtro '$usuarioFiltro'...")
            val result = withContext(Dispatchers.IO) { repository.fetchUsers(usuarioFiltro) }

            if (result.isSuccess) {
                _users.value = result.getOrNull()
                Log.d("UsuarioViewModel", "Usuarios cargados correctamente.")
            } else {
                val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                _errorMessage.value = Event("Error al cargar usuarios: $error")
                Log.e("UsuarioViewModel", "Error al cargar usuarios: $error")
            }
        }
    }

    // Método loadUsers para cargar usuarios con filtro de búsqueda
    fun loadUsers(usuarioFiltro: String = "") {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val empresaDbName = repository.sessionManager.getEmpresaData()?.db_name ?: return@withContext

                    val jsonObject = JSONObject().apply {
                        put("empresaDbName", empresaDbName)
                        if (usuarioFiltro.isNotEmpty()) {
                            put("usuarioFiltro", usuarioFiltro)
                        }
                    }

                    val requestBody = jsonObject.toString()
                        .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

                    val response = repository.apiService.obtenerUsuarios(requestBody)

                    if (response.isSuccessful) {
                        val users = response.body()?.data ?: emptyList()
                        _users.postValue(users)
                        Log.d("UsuarioViewModel", "Usuarios cargados con filtro correctamente.")
                    } else {
                        _errorMessage.postValue(Event("Error al obtener usuarios: ${response.code()}"))
                    }
                } catch (e: Exception) {
                    _errorMessage.postValue(Event("Error: ${e.message}"))
                }
            }
        }
    }
}
