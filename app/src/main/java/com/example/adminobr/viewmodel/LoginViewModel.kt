package com.example.adminobr.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.adminobr.data.Empresa
import com.example.adminobr.data.LoginResponse
import com.example.adminobr.data.ResultData
import com.example.adminobr.data.Usuario
import com.example.adminobr.repository.LoginRepository
import com.example.adminobr.utils.Constants
import com.example.adminobr.utils.Event
import com.example.adminobr.utils.NetworkStatusHelper
import com.example.adminobr.utils.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val _errorMessage = MutableLiveData<Event<String>>()
    val errorMessage: LiveData<Event<String>> get() = _errorMessage

    private val _successMessage = MutableLiveData<Event<String>>()
    val successMessage: LiveData<Event<String>> get() = _successMessage

    // LiveData para los errores en los campos del formulario (EditTextLayout)
    private val _fieldErrorMessage = MutableLiveData<Event<String>>()
    val fieldErrorMessage: LiveData<Event<String>> get() = _fieldErrorMessage

    private val _mensaje = MutableLiveData<Event<String>>()
    val mensaje: LiveData<Event<String>> get() = _mensaje

    private val _users = MutableLiveData<List<Usuario>?>()
    val users: LiveData<List<Usuario>?> get() = _users

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val loginRepository = LoginRepository()

    // Otros campos
    val loginResult = SingleLiveEvent<ResultData<LoginResponse>>()
//    val networkError = MutableLiveData<Boolean>()
    val empresaValidationResult = MutableLiveData<ResultData<Empresa>>()

    private var isNetworkCheckEnabled = Constants.getNetworkStatusHelper()

    // Método de validación de empresa
    fun validateEmpresa(empresaCode: String, callback: (Boolean) -> Unit = {}) {
        if (isNetworkCheckEnabled && !NetworkStatusHelper.isConnected()) {
            // Emite un error específico en el loginResult si no hay conexión
            loginResult.value = ResultData.Error("No hay conexión a internet, intenta nuevamente mas tarde")
            callback(false)
            return
        }

        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = loginRepository.validateCompany(empresaCode)
                Log.d("LoginViewModel", "Resultado recibido del Repository: $result")

                withContext(Dispatchers.Main) {
                    empresaValidationResult.value = result
                    Log.d("LoginViewModel", "LiveData empresaValidationResult actualizado con: $result")

                    when (result) {
                        is ResultData.Success -> {
                            //val response = result.data
                            //_successMessage.value = Event(response.message ?: "Inicio de sesión exitoso.")
                            callback(true)
                        }
                        is ResultData.Error -> {
                            val errorMessage = result.message ?: "Error desconocido al validar la empresa."
                            val errorCode = result.errorCode // Aquí debería ya tener el error_code de la API

                            _errorMessage.value = Event(errorMessage)
                            if (!errorCode.isNullOrEmpty()) {
                                _fieldErrorMessage.value = Event(errorCode)
                            }
                            callback(false)
                        }
                    }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Excepción al validar la empresa: ${e.localizedMessage}", e)
            }
        }
    }

    fun login(usuario: String, password: String, empresaDbName: String, callback: (Boolean) -> Unit = {}) {
        if (isNetworkCheckEnabled && !NetworkStatusHelper.isConnected()) {
            // Emite un error específico en el loginResult si no hay conexión
            loginResult.value = ResultData.Error("No hay conexión a internet, intenta nuevamente mas tarde")
            callback(false)
            return
        }

        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val result = loginRepository.login(usuario, password, empresaDbName)
            withContext(Dispatchers.Main) {
                loginResult.value = result
                when (result) {
                    is ResultData.Success -> {
                        //val response = result.data
                        //_successMessage.value = Event(response.message ?: "Inicio de sesión exitoso.")
                        callback(true)
                    }
                    is ResultData.Error -> {
                        val errorMessage = result.message ?: "Error desconocido al iniciar sesión."
                        val errorCode = result.errorCode // Aquí debería ya tener el error_code de la API

                        _errorMessage.value = Event(errorMessage)
                        if (!errorCode.isNullOrEmpty()) {
                            _fieldErrorMessage.value = Event(errorCode)
                        }
                        callback(false)
                    }
                }
                _isLoading.value = false
            }
        }
    }

    fun validateToken(token: String): ResultData<Unit> {
        return runBlocking {
            loginRepository.validateToken(token)
        }
    }

    fun logout(token: String): ResultData<Unit> {
        if (isNetworkCheckEnabled && !NetworkStatusHelper.isConnected()) {
            // Retorna un error específico si no hay conexión
            return ResultData.Error("No hay conexión a internet, intenta más tarde")
        }

        // Usa runBlocking para ejecutar una llamada síncrona al repositorio
        return runBlocking {
            loginRepository.logout(token) // Llama al método correspondiente en el repositorio
        }
    }
}
