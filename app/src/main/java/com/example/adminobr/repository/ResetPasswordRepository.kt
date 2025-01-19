package com.example.adminobr.repository

import com.example.adminobr.api.ApiService
import com.example.adminobr.data.ResultData
import org.json.JSONObject

object ResetPasswordRepository {
    private val apiService = ApiService.create()

    suspend fun sendRecoveryCode(email: String, empresaDbName: String): ResultData<Unit> {
        return try {
            val body = mapOf("email" to email, "empresaDbName" to empresaDbName)
            val response = apiService.sendRecoveryCode(body)
            if (response.isSuccessful) {
                ResultData.Success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = JSONObject(errorBody).optString("message", "Error desconocido")
                ResultData.Error(errorMessage)
            }
        } catch (e: Exception) {
            ResultData.Error("Error de red: ${e.localizedMessage}")
        }
    }

    suspend fun verifyCode(email: String, code: String, empresaDbName: String): ResultData<Unit> {
        return try {
            val body = mapOf(
                "email" to email,
                "code" to code,
                "empresaDbName" to empresaDbName
            )
            val response = apiService.verifyCode(body)
            if (response.isSuccessful) {
                ResultData.Success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = JSONObject(errorBody).optString("message", "Error desconocido")
                ResultData.Error(errorMessage)
            }
        } catch (e: Exception) {
            ResultData.Error("Error de red: ${e.localizedMessage}")
        }
    }

    suspend fun changePassword(email: String, newPassword: String, empresaDbName: String): ResultData<Unit> {
        return try {
            val body = mapOf(
                "email" to email,
                "newPassword" to newPassword,
                "empresaDbName" to empresaDbName
            )
            val response = apiService.changePassword(body)
            if (response.isSuccessful) {
                ResultData.Success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = JSONObject(errorBody).optString("message", "Error desconocido")
                ResultData.Error(errorMessage)
//                ResultData.Error(response.message())
            }
        } catch (e: Exception) {
            ResultData.Error("Error de red: ${e.localizedMessage}")
        }
    }

}
