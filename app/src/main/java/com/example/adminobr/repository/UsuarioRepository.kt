package com.example.adminobr.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.adminobr.api.ApiResponse
import com.example.adminobr.api.ApiService
import com.example.adminobr.data.Usuario
import com.example.adminobr.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Response
import okhttp3.RequestBody.Companion.toRequestBody

class UsuarioRepository(
    val apiService: ApiService,
    val sessionManager: SessionManager
) {
    val isLoading = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String>()

    // Crear usuario
    suspend fun createUser(usuario: Usuario): Result<Usuario> = withContext(Dispatchers.IO) {
        try {
            val empresaDbName = sessionManager.getEmpresaData()?.db_name
                ?: return@withContext Result.failure<Usuario>(Exception("Empresa DB no especificado."))

            val response = apiService.crearUsuario(
                usuario.legajo, usuario.email, usuario.dni, usuario.password,
                usuario.nombre, usuario.apellido, usuario.telefono,
                usuario.userCreated ?: 0, usuario.estadoId, empresaDbName
            )
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Actualizar usuario
    suspend fun updateUser(usuario: Usuario, newPassword: String?): Result<Usuario> = withContext(Dispatchers.IO) {
        try {
            val empresaDbName = sessionManager.getEmpresaData()?.db_name
                ?: return@withContext Result.failure<Usuario>(Exception("Empresa DB no especificado."))

            val jsonObject = JSONObject().apply {
                put("empresaDbName", empresaDbName)
                put("id_usuario", usuario.id)
                put("legajo", usuario.legajo)
                put("email", usuario.email)
                put("dni", usuario.dni)
                put("nombre", usuario.nombre)
                put("apellido", usuario.apellido)
                put("telefono", usuario.telefono)
                put("estado_id", usuario.estadoId)
                if (!newPassword.isNullOrEmpty()) put("password", newPassword)
            }

            val requestBody = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val response = apiService.actualizarUsuario(requestBody)
            handleResponse(response)
        } catch (e: Exception) {
            Log.e("UsuarioRepository", "Error al actualizar usuario: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    // Actualizar datos parciales del perfil
    suspend fun updateProfile(usuario: Usuario, newPassword: String?): Result<Usuario> = withContext(Dispatchers.IO) {
        try {
            val empresaDbName = sessionManager.getEmpresaData()?.db_name
                ?: return@withContext Result.failure<Usuario>(Exception("Empresa DB no especificado."))

            val jsonObject = JSONObject().apply {
                put("empresaDbName", empresaDbName)
                put("id_usuario", usuario.id)
                put("email", usuario.email)
                put("dni", usuario.dni)
                put("nombre", usuario.nombre)
                put("apellido", usuario.apellido)
                put("telefono", usuario.telefono)
                if (!newPassword.isNullOrEmpty()) put("password", newPassword)
            }

            val requestBody = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val response = apiService.actualizarUsuario(requestBody)
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Actualizar solo la contraseña
    suspend fun updatePassword(usuario: Usuario, nuevaContrasena: String): Result<Usuario> = withContext(Dispatchers.IO) {
        try {
            val empresaDbName = sessionManager.getEmpresaData()?.db_name
                ?: return@withContext Result.failure<Usuario>(Exception("Empresa DB no especificado."))

            val jsonObject = JSONObject().apply {
                put("empresaDbName", empresaDbName)
                put("id_usuario", usuario.id)
                put("password", nuevaContrasena)
            }

            val requestBody = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val response = apiService.actualizarUsuario(requestBody)
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // Eliminar usuario
    suspend fun deleteUser(idUsuario: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val empresaDbName = sessionManager.getEmpresaData()?.db_name
                ?: return@withContext Result.failure<Unit>(Exception("Empresa DB no especificado."))

            val response = apiService.eliminarUsuario(idUsuario, empresaDbName)
            handleResponse(response)
        } catch (e: Exception) {
            Log.e("UsuarioRepository", "Excepción al eliminar usuario: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    // Obtener todos los usuarios
    suspend fun fetchUsers(usuarioFiltro: String = ""): Result<List<Usuario>> = withContext(Dispatchers.IO) {
        try {
            val empresaDbName = sessionManager.getEmpresaData()?.db_name
                ?: return@withContext Result.failure<List<Usuario>>(Exception("Empresa DB no especificado."))

            val jsonObject = JSONObject().apply {
                put("empresaDbName", empresaDbName)
                if (usuarioFiltro.isNotEmpty()) put("usuarioFiltro", usuarioFiltro)
            }
            val requestBody = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            val response = apiService.getAllUsers(requestBody)
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Obtener usuario por ID
    suspend fun fetchUserById(idUsuario: Int): Result<Usuario> = withContext(Dispatchers.IO) {
        try {
            val requestBody = createRequestBody(idUsuario)
            val response = apiService.getUsuarioById(requestBody)
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Maneja la respuesta y simplifica el código de manejo de `Result` para todas las funciones
    private fun <T> handleResponse(response: Response<ApiResponse<T>>): Result<T> {
        return if (response.isSuccessful) {
            val apiResponse = response.body()
            Log.d("UsuarioRepository", "API Response: success=${apiResponse?.success}, data=${apiResponse?.data}, message=${apiResponse?.message}")

            // Validar éxito si `success` es `true` y `data` está presente o es una operación `Unit`
            if (apiResponse?.success == true) {
                if (apiResponse.data != null) {
                    Result.success(apiResponse.data)
                } else {
                    // Si `data` es nulo, pero el `success` es `true`, devolver `Unit` como resultado exitoso
                    @Suppress("UNCHECKED_CAST")
                    Result.success(Unit as T)
                }
            } else {
                // Devolver error con el mensaje de `apiResponse` o un mensaje genérico
                Result.failure(Exception(apiResponse?.message ?: "Error desconocido en la respuesta"))
            }
        } else {
            // Error en la solicitud HTTP
            val errorMessage = response.errorBody()?.string() ?: "Error desconocido"
            Log.e("UsuarioRepository", "Error HTTP: $errorMessage")
            Result.failure(Exception("Error HTTP: $errorMessage"))
        }
    }

    // Crea un RequestBody para obtener usuario por ID
    private fun createRequestBody(idUsuario: Int): RequestBody {
        val empresaDbName = sessionManager.getEmpresaData()?.db_name ?: ""
        val json = """{ "empresaDbName": "$empresaDbName", "id": $idUsuario }"""
        return json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
    }
}