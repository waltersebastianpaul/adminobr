package com.example.adminobr.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.adminobr.api.ApiResponse
import com.example.adminobr.api.ApiService
import com.example.adminobr.data.CustomException
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
    val errorMessage = MutableLiveData<String>()

    // Crear usuario
    suspend fun createUser(usuario: Usuario): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val empresaDbName = sessionManager.getEmpresaData()["empresaDbName"]
                ?: return@withContext Result.failure(Exception("Empresa DB no especificado."))

            val response = apiService.crearUsuario(
                usuario.legajo,
                usuario.email,
                usuario.dni,
                usuario.password,
                usuario.nombre,
                usuario.apellido,
                usuario.telefono,
                usuario.userCreated ?: 0,
                usuario.estadoId,
                usuario.roleId,
                empresaDbName
            )

            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.success == true) {
                    // Aseguramos que el ID sea siempre un valor no nulo
                    return@withContext Result.success(mapOf("success" to true, "id" to (responseBody.id ?: -1)))
                } else {
                    // Si no es un éxito, lanzamos una CustomException con el error
                    val errorCode = responseBody?.errorCode ?: "unknown"
                    return@withContext Result.failure(CustomException(errorCode, responseBody?.message))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorJson = JSONObject(errorBody ?: "{}")
                // Retornamos un error con los detalles adecuados
                return@withContext Result.failure(
                    CustomException(
                        errorJson.optString("error_code", "-1"), // Si no hay error_code, usamos "-1" como fallback
                        errorJson.optString("message", "Error desconocido")
                    )
                )
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    // Actualizar usuario
    suspend fun updateUser(usuario: Usuario, currentPassword: String?, newPassword: String?): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val empresaDbName = sessionManager.getEmpresaData()["empresaDbName"]
                ?: return@withContext Result.failure<Map<String, Any>>(Exception("Empresa DB no especificado."))

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
                if (!newPassword.isNullOrEmpty()) {
                    put("currentPassword", currentPassword)
                    put("password", newPassword)
                }
            }

            val requestBody = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val response = apiService.editarUsuario(requestBody)

            // Manejamos la respuesta de la API
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.success == true) {
                    return@withContext Result.success(mapOf("success" to true, "id" to (responseBody.id ?: -1)))
                } else {
                    val errorCode = responseBody?.errorCode
                    return@withContext Result.failure(CustomException(errorCode.toString(), responseBody?.message))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorJson = JSONObject(errorBody ?: "{}")
                return@withContext Result.failure(
                    CustomException(
                        errorJson.optString("error_code", "-1"),
                        errorJson.optString("message", "Error desconocido")
                    )
                )
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    // Actualizar solo datos del perfil
    suspend fun updateProfile(usuario: Usuario, currentPassword: String?, newPassword: String?): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val empresaDbName = sessionManager.getEmpresaData()["empresaDbName"]
                ?: return@withContext Result.failure<Map<String, Any>>(Exception("Empresa DB no especificado."))

            val jsonObject = JSONObject().apply {
                put("empresaDbName", empresaDbName)
                put("id_usuario", usuario.id)
                put("email", usuario.email)
                put("dni", usuario.dni)
                put("nombre", usuario.nombre)
                put("apellido", usuario.apellido)
                put("telefono", usuario.telefono)
                if (!newPassword.isNullOrEmpty()) {
                    put("currentPassword", currentPassword)
                    put("password", newPassword)
                }
            }

            val requestBody = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val response = apiService.editarUsuario(requestBody)

            // Manejamos la respuesta de la API
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.success == true) {
                    return@withContext Result.success(mapOf("success" to true, "id" to (responseBody.id ?: -1)))
                } else {
                    val errorCode = responseBody?.errorCode
                    return@withContext Result.failure(CustomException(errorCode.toString(), responseBody?.message))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorJson = JSONObject(errorBody ?: "{}")
                return@withContext Result.failure(
                    CustomException(
                        errorJson.optString("error_code", "-1"),
                        errorJson.optString("message", "Error desconocido")
                    )
                )
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    // Actualizar solo la contraseña
    suspend fun updatePassword(usuario: Usuario, currentPassword: String, newPassword: String): Result<Usuario> {
        return withContext(Dispatchers.IO) {
            try {
                val empresaDbName = sessionManager.getEmpresaData()["empresaDbName"]
                    ?: return@withContext Result.failure<Usuario>(CustomException("missing_db", "Empresa DB no especificado."))

                val jsonObject = JSONObject().apply {
                    put("empresaDbName", empresaDbName)
                    put("id_usuario", usuario.id)
                    put("currentPassword", currentPassword)
                    put("password", newPassword)
                }

                val requestBody = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val response = apiService.editarUsuario(requestBody)

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody?.success == true) {
                        val usuarioActualizado = responseBody.data ?: usuario
                        Result.success(usuarioActualizado)
                    } else {
                        val errorCode = responseBody?.errorCode ?: "unknown_error"
                        Result.failure(CustomException(errorCode, responseBody?.message ?: "Error desconocido"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorJson = JSONObject(errorBody ?: "{}")
                    val errorCode = errorJson.optString("error_code", "unknown_error")
                    val errorMessage = errorJson.optString("message", "Error desconocido")
                    Result.failure(CustomException(errorCode, errorMessage))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Eliminar usuario
    suspend fun deleteUser(idUsuario: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val empresaDbName = sessionManager.getEmpresaData()["empresaDbName"]
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
            val empresaDbName = sessionManager.getEmpresaData()["empresaDbName"]
                ?: return@withContext Result.failure<List<Usuario>>(Exception("Empresa DB no especificado."))

            val jsonObject = JSONObject().apply {
                put("empresaDbName", empresaDbName)
                if (usuarioFiltro.isNotEmpty()) put("usuarioFiltro", usuarioFiltro)
            }
            val requestBody = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            val response = apiService.obtenerUsuarios(requestBody)
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Obtener usuario por ID
    suspend fun fetchUserById(idUsuario: Int): Result<Usuario> = withContext(Dispatchers.IO) {
        try {
            val requestBody = createRequestBody(idUsuario)
            val response = apiService.obtenerUsuario(requestBody)
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Asignar rol al usuario
    suspend fun assignRole(usuarioId: Int, rolId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val empresaDbName = sessionManager.getEmpresaData()["empresaDbName"]
                ?: return@withContext Result.failure<Unit>(Exception("Empresa DB no especificada."))

            // Crear el JSON para el cuerpo de la solicitud
            val jsonObject = JSONObject().apply {
                put("empresaDbName", empresaDbName)
                put("usuario_id", usuarioId)
                put("rol_id", rolId)
            }
            //Log.d("UsuarioRepository", "URL para asignar rol: ${apiService.asignarRolUsuario}")
            Log.d("UsuarioRepository", "JSON para asignar rol: $jsonObject")
            val requestBody = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            // Realizar la llamada a la API usando el requestBody
            val response = apiService.asignarRolUsuario(requestBody)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                // Log detallado del error cuando el response no es successful
                val errorBody = response.errorBody()?.string() ?: "Respuesta sin detalles de error"
                Log.e("UsuarioRepository", "Error en respuesta asignar rol: ${response.code()} - $errorBody")
                Result.failure(Exception("Error al asignar rol: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("UsuarioRepository", "Excepción al asignar rol: ${e.message}")
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
        val empresaDbName = sessionManager.getEmpresaData()["empresaDbName"] ?: ""
        val json = """{ "empresaDbName": "$empresaDbName", "id": $idUsuario }"""
        return json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
    }

}


