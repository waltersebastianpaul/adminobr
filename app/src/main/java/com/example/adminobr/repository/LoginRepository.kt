package com.example.adminobr.repository

import android.util.Log
import com.example.adminobr.api.ApiService
import com.example.adminobr.data.Empresa
import com.example.adminobr.data.LoginResponse
import com.example.adminobr.data.ResultData
import org.json.JSONObject

class LoginRepository {

    private val apiService = ApiService.create()

    /**
     * Valida la empresa con el código proporcionado.
     */
    suspend fun validateCompany(code: String): ResultData<Empresa> {
        Log.d("LoginRepository", "Iniciando validación para código: $code")

        return try {
            val response = apiService.validateCompany(mapOf("code" to code))
            Log.d("LoginRepository", "Respuesta recibida: ${response.code()} ${response.message()}")

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.db_name != null) { // Verificar que el body y el db_name no sean nulos
                    Log.d("LoginRepository", "Validación exitosa: $body")
                    ResultData.Success(body)
                } else {
                    ResultData.Error(
                        message = body?.message ?: "Error desconocido.",
                        errorCode = body?.errorCode // Capturamos el error_code de la respuesta exitosa
                    )
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("LoginRepository", "Error en la respuesta: Código ${response.code()}, cuerpo: $errorBody")

                val errorJson = if (!errorBody.isNullOrEmpty()) {
                    try {
                        JSONObject(errorBody)
                    } catch (e: Exception) {
                        null
                    }
                } else null

                val errorMessage = errorJson?.optString("message", "Error desconocido.") ?: "Error desconocido en la respuesta del servidor."
                val errorCode = errorJson?.optString("error_code", null) // Capturamos error_code del errorBody

                ResultData.Error(
                    message = errorMessage,
                    errorCode = errorCode
                )
            }
        } catch (e: Exception) {
            Log.e("LoginRepository", "Excepción en validateCompany: ${e.localizedMessage}", e)
            ResultData.Error("Error al validar la empresa: ${e.localizedMessage}")
        }
    }

    /**
     * Realiza el login con usuario, contraseña y nombre de base de datos.
     */
    suspend fun login(usuario: String, password: String, dbName: String): ResultData<LoginResponse> {
        return try {
            val response = apiService.login(
                mapOf(
                    "usuario" to usuario,
                    "password" to password,
                    "empresaDbName" to dbName
                )
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    ResultData.Success(body)
                } else {
                    ResultData.Error(
                        message = body?.message ?: "Error desconocido.",
                        errorCode = body?.errorCode // Capturamos el error_code de la respuesta exitosa
                    )
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorJson = if (!errorBody.isNullOrEmpty()) {
                    try {
                        JSONObject(errorBody)
                    } catch (e: Exception) {
                        null
                    }
                } else null

                val errorMessage = errorJson?.optString("message", "Error desconocido.") ?: "Error desconocido en la respuesta del servidor."
                val errorCode = errorJson?.optString("error_code", null) // Capturamos error_code del errorBody

                ResultData.Error(
                    message = errorMessage,
                    errorCode = errorCode
                )
            }
        } catch (e: Exception) {
            ResultData.Error("Error al iniciar sesión: ${e.localizedMessage}")
        }
    }

    suspend fun validateToken(token: String): ResultData<Unit> {
        return try {
            val response = apiService.validateToken("Bearer $token")
            Log.d("TokenDebug", "Token enviado: Bearer $token")

            if (response.isSuccessful) {
                ResultData.Success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("TokenDebug", "Error de validación: $errorBody")
                ResultData.Error("Token inválido o expirado: $errorBody")
            }
        } catch (e: Exception) {
            Log.e("TokenDebug", "Error de red: ${e.localizedMessage}")
            ResultData.Error("Error de red: ${e.localizedMessage}")
        }
    }

    suspend fun logout(token: String): ResultData<Unit> {
        return try {
            val response = apiService.logout("Bearer $token")
            if (response.isSuccessful) {
                ResultData.Success(Unit)
            } else {
                ResultData.Error("Error al cerrar sesión: ${response.message()}")
            }
        } catch (e: Exception) {
            ResultData.Error("Error de red: ${e.localizedMessage}")
        }
    }
}
