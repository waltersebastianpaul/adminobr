package com.example.adminobr.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.example.adminobr.api.ApiService
import com.example.adminobr.data.CustomException
import com.example.adminobr.data.Usuario
import com.example.adminobr.repository.UsuarioRepository
import com.example.adminobr.utils.Event
import com.example.adminobr.utils.NetworkStatusHelper
import com.example.adminobr.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UsuarioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UsuarioRepository(ApiService.create(), SessionManager(application))

    private val _errorMessage = MutableLiveData<Event<String>>()
    val errorMessage: LiveData<Event<String>> get() = _errorMessage

    // LiveData para los errores en los campos del formulario (EditTextLayout)
    private val _fieldErrorMessage = MutableLiveData<Event<String>>()
    val fieldErrorMessage: LiveData<Event<String>> get() = _fieldErrorMessage

    private val _mensaje = MutableLiveData<Event<String>>()
    val mensaje: LiveData<Event<String>> get() = _mensaje

    private val _users = MutableLiveData<List<Usuario>?>()
    val users: LiveData<List<Usuario>?> get() = _users

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _passwordValidationResult = MutableLiveData<Result<Boolean>>()
    val passwordValidationResult: LiveData<Result<Boolean>> get() = _passwordValidationResult

    // Nuevo LiveData para el estado de la conexión a internet
    private val _isNetworkAvailable = MutableLiveData<Boolean>()
    val isNetworkAvailable: LiveData<Boolean> get() = _isNetworkAvailable

    init {
        // Inicializar el estado de la conexión a internet
        _isNetworkAvailable.value = NetworkStatusHelper.isConnected()
    }

    fun crearUsuario(
        usuario: Usuario,
        callback: (Boolean, Int?) -> Unit
    ) {
        if (!NetworkStatusHelper.isConnected()) {
            _errorMessage.value = Event("No hay conexión a internet, intenta más tarde")
            _isNetworkAvailable.value = false
            callback(false, null)
            return
        }

        _isNetworkAvailable.value = true
        _isLoading.value = true // Indica que la carga está en progreso
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repository.createUser(usuario) }
            if (result.isSuccess) {
                val response = result.getOrNull() ?: mapOf()
                _mensaje.value = Event("Usuario creado exitosamente")
                callback(true, response["id"] as? Int)
            } else {
                val error = result.exceptionOrNull()
                // Comprobar si la excepción es una CustomException
                if (error is CustomException) {
                    val errorCode = error.errorCode
                    val errorMessage = error.message ?: "Error desconocido"

                    // Manejar el error, como mostrar un mensaje adecuado al usuario
                    _errorMessage.value = Event(errorMessage)  // Aquí se maneja el mensaje completo

                    // Emitir el código de error en lugar del mensaje
                    _fieldErrorMessage.value = Event(errorCode)
                } else {
                    _errorMessage.value = Event(error?.message ?: "Error desconocido")
                }
                callback(false, null)
            }
            _isLoading.value = false // Indica que la carga ha terminado
        }
    }

    fun asignarRol(
        usuarioId: Int,
        rolId: Int
    ) {
        if (!NetworkStatusHelper.isConnected()) {
            _errorMessage.value = Event("No hay conexión a internet, intenta más tarde")
            _isNetworkAvailable.value = false
            return
        }

        _isNetworkAvailable.value = true
        _isLoading.value = true // Indica que la carga está en progreso
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repository.assignRole(usuarioId, rolId) }
            if (result.isSuccess) {
                _mensaje.value = Event("Rol asignado exitosamente")
            } else {
                _errorMessage.value = Event("Error al asignar rol: ${result.exceptionOrNull()?.message}")
            }
            _isLoading.value = false // Indica que la carga ha terminado
        }
    }

    fun actualizarUsuario(
        usuario: Usuario,
        currentpassword: String?,
        newPassword: String?,
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
            val result = withContext(Dispatchers.IO) { repository.updateUser(usuario, currentpassword, newPassword) }
            if (result.isSuccess) {
                val response = result.getOrNull() ?: mapOf()
                _mensaje.value = Event("Usuario actualizado exitosamente")
                callback(true)
            } else {
                val error = result.exceptionOrNull()
                // Comprobar si la excepción es una CustomException
                if (error is CustomException) {
                    val errorCode = error.errorCode
                    val errorMessage = error.message ?: "Error desconocido al actualizar usuario."

                    // Manejar el error, como mostrar un mensaje adecuado al usuario
                    _errorMessage.value = Event(errorMessage)  // Aquí se maneja el mensaje completo

                    // Emitir el código de error en lugar del mensaje
                    _fieldErrorMessage.value = Event(errorCode)
                } else {
                    _errorMessage.value = Event(error?.message ?: "Error desconocido al actualizar usuario.")
                }
                callback(false)
            }
            _isLoading.value = false // Indica que la carga ha terminado
        }
    }

    fun actualizarPerfilUsuario(
        usuario: Usuario,
        currentPassword: String?,
        newPassword: String?,
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
            val result = withContext(Dispatchers.IO) { repository.updateProfile(usuario, currentPassword, newPassword) }
            if (result.isSuccess) {
                _mensaje.value = Event("Perfil actualizado exitosamente")
                callback(true)
            } else {
                val error = result.exceptionOrNull()
                // Comprobar si la excepción es una CustomException
                if (error is CustomException) {
                    val errorCode = error.errorCode
                    val errorMessage = error.message ?: "Error desconocido al actualizar perfil."

                    // Manejar el error, como mostrar un mensaje adecuado al usuario
                    _errorMessage.value = Event(errorMessage)  // Aquí se maneja el mensaje completo

                    // Emitir el código de error en lugar del mensaje
                    _fieldErrorMessage.value = Event(errorCode)
                } else {
                    _errorMessage.value = Event(error?.message ?: "Error desconocido al actualizar perfil.")
                }
                callback(false)
            }
            _isLoading.value = false // Indica que la carga ha terminado
        }
    }

    fun actualizarContrasenaUsuario(
        usuario: Usuario,
        currentPassword: String,
        newPassword: String,
        callback: (Boolean) -> Unit
    ) {
        if (!NetworkStatusHelper.isConnected()) {
            _errorMessage.value = Event("No hay conexión a internet, intenta más tarde")
            _isNetworkAvailable.value = false
            callback(false)
            return
        }

        _isNetworkAvailable.value = true
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.updatePassword(usuario, currentPassword, newPassword)
            if (result.isSuccess) {
                _mensaje.value = Event("Contraseña actualizada exitosamente")
                callback(true)
            } else {
                val error = result.exceptionOrNull()
                if (error is CustomException) {
                    val errorCode = error.errorCode
                    val errorMessage = error.message ?: "Error desconocido al actualizar contraseña."

                    // Manejar el error, como mostrar un mensaje adecuado al usuario
                    _errorMessage.value = Event(errorMessage)  // Aquí se maneja el mensaje completo

                    // Emitir el código de error en lugar del mensaje
                    _fieldErrorMessage.value = Event(errorCode)
                } else {
                    _errorMessage.value = Event("Error desconocido al actualizar contraseña.")
                }
                callback(false)
            }
            _isLoading.value = false
        }
    }

    fun eliminarUsuario(idUsuario: Int) {
        if (!NetworkStatusHelper.isConnected()) {
            _errorMessage.value = Event("No hay conexión a internet, intenta más tarde")
            return
        }

        _isLoading.value = true // Indica que la carga está en progreso
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
            _isLoading.value = false // Indica que la carga ha terminado
        }
    }

    fun obtenerUsuarioPorId(idUsuario: Int) {
        if (!NetworkStatusHelper.isConnected()) {
            _errorMessage.value = Event("No hay conexión a internet, intenta más tarde")
            return
        }

        _isLoading.value = true // Indica que la carga está en progreso
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repository.fetchUserById(idUsuario) }
            if (result.isSuccess) {
                result.getOrNull()?.let { _users.value = listOf(it) }
            } else {
                _errorMessage.value = Event("Error al obtener usuario: ${result.exceptionOrNull()?.message}")
            }
            _isLoading.value = false // Indica que la carga ha terminado
        }
    }

    fun cargarUsuarios(usuarioFiltro: String = "") {
        if (!NetworkStatusHelper.isConnected()) {
            _errorMessage.value = Event("No hay conexión a internet, intenta más tarde")
            return
        }

        _isLoading.value = true // Indica que la carga está en progreso
        viewModelScope.launch {
            Log.d("UsuarioViewModel", "Iniciando carga de usuarios con filtro '$usuarioFiltro'...")
            val result = withContext(Dispatchers.IO) { repository.fetchUsers(usuarioFiltro) }

            if (result.isSuccess) {
                _users.value = result.getOrNull()
                Log.d("UsuarioViewModel", "Usuarios cargados correctamente.")
//            } else {
//                val error = result.exceptionOrNull()?.message ?: "Error desconocido"
//                _errorMessage.value = Event("Error al cargar usuarios: $error")
//                Log.e("UsuarioViewModel", "Error al cargar usuarios: $error")
            } else {
                val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                _errorMessage.postValue(Event("Error al obtener usuarios: $error"))
                Log.e("UsuarioViewModel", "Error al obtener usuarios: $error")
            }
            _isLoading.value = false // Indica que la carga ha terminado
        }
    }
}
