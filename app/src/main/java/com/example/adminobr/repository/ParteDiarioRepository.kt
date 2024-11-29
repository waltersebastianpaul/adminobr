package com.example.adminobr.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.adminobr.api.ApiResponse
import com.example.adminobr.api.ApiService
import com.example.adminobr.data.ParteDiario
import com.example.adminobr.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Response
import okhttp3.RequestBody.Companion.toRequestBody

class ParteDiarioRepository(
    val apiService: ApiService,
    val sessionManager: SessionManager
) {
    val errorMessage = MutableLiveData<String>()

    suspend fun getUltimoPartePorEquipo(equipoId: Int): Result<ParteDiario?> = withContext(Dispatchers.IO) {
        try {
            val empresaDbName = sessionManager.getEmpresaData()?.db_name
                ?: return@withContext Result.failure(Exception("Empresa DB no especificado."))

            val jsonObject = JSONObject().apply {
                put("equipoId", equipoId)
                put("empresaDbName", empresaDbName)
            }

            val requestBody = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            val response = apiService.getUltimoPartePorEquipo(requestBody)
            if (response.isSuccessful) {
                val parte = response.body()?.data // Data es un único objeto `ParteDiario`
                Result.success(parte)
            } else {
                Result.failure(Exception("Error en la respuesta HTTP: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUltimosPartesPorUsuario(userId: Int): Result<List<ParteDiario>> = withContext(Dispatchers.IO) {
        try {
            val empresaDbName = sessionManager.getEmpresaData()?.db_name
                ?: return@withContext Result.failure(Exception("Empresa DB no especificado."))

            val jsonObject = JSONObject().apply {
                put("userId", userId)
                put("empresaDbName", empresaDbName)
            }

            val requestBody = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            val response = apiService.getUltimosPartesPorUsuario(requestBody)
            if (response.isSuccessful) {
                response.body()?.data?.let {
                    Result.success(it.take(10))
                } ?: Result.failure(Exception("Respuesta JSON vacía o inesperada"))
            } else {
                // Si la respuesta no es exitosa, capturamos el error detallado
                val errorBody = response.errorBody()?.string()
                Log.e("ParteDiarioRepository", "Error en la respuesta HTTP: $errorBody")
                Result.failure(Exception("Error en la respuesta HTTP: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("ParteDiarioRepository", "Error al obtener partes: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    suspend fun createParteDiario(parte: ParteDiario): Result<Pair<Boolean, Int?>> = withContext(Dispatchers.IO) {
        try {
            val empresaDbName = sessionManager.getEmpresaData()?.db_name
                ?: return@withContext Result.failure<Pair<Boolean, Int?>>(Exception("Empresa DB no especificado."))

            val response = apiService.crearParteDiario(
                fecha = parte.fecha ?: "",                       // Valor predeterminado si es null
                equipoId = parte.equipoId ?: 0,                  // Ejemplo: usa 0 si es null
                horasInicio = parte.horasInicio ?: 0,
                horasFin = parte.horasFin ?: 0,
                horasTrabajadas = parte.horasTrabajadas ?: 0,
                observaciones = parte.observaciones,       // Usa cadena vacía si es null
                obraId = parte.obraId ?: 0,
                userCreated = parte.userCreated ?: 0,
                estadoId = parte.estadoId ?: 0,
                combustibleTipo = parte.combustibleTipo,
                combustibleCant = parte.combustibleCant,
                aceiteMotorCant = parte.aceiteMotorCant,
                aceiteHidraCant = parte.aceiteHidraCant,
                aceiteOtroCant = parte.aceiteOtroCant,
                engraseGeneral = parte.engraseGeneral,  // Falso si es null
                filtroAire = parte.filtroAire,
                filtroAceite = parte.filtroAceite,
                filtroComb = parte.filtroComb,
                filtroOtro = parte.filtroOtro,
                empresaDbName = empresaDbName
            )

            if (response.isSuccessful) {
                val responseBody = response.body()
                Log.d("ParteDiarioRepository", "Respuesta de creación: success=${responseBody?.success}, id=${responseBody?.id}")
                if (responseBody?.success == true) {
                    val nuevoId = responseBody.id // Obtener el ID del parte creado
                    return@withContext Result.success(Pair(true, nuevoId))
                } else {
                    // Log detallado si `success` es `false`
                    Log.e("ParteDiarioRepository", "Error en la respuesta: ${responseBody?.message}")
                    return@withContext Result.failure<Pair<Boolean, Int?>>(Exception("Error en la creación de parte"))
                }
            } else {
                Log.e("ParteDiarioRepository", "Error en la respuesta HTTP: ${response.errorBody()?.string()}")
                return@withContext Result.failure<Pair<Boolean, Int?>>(Exception("Error en la respuesta HTTP"))
            }
        } catch (e: Exception) {
            Log.e("ParteDiarioRepository", "Error al crear parte: ${e.localizedMessage}")
            return@withContext Result.failure<Pair<Boolean, Int?>>(e)
        }
    }

    // Actualizar parte diario
    suspend fun updateParteDiario(parte: ParteDiario): Result<ParteDiario> = withContext(Dispatchers.IO) {
        try {
            val empresaDbName = sessionManager.getEmpresaData()?.db_name
                ?: return@withContext Result.failure<ParteDiario>(Exception("Empresa DB no especificado."))

            val jsonObject = JSONObject().apply {
                put("empresaDbName", empresaDbName)
                put("id_parte_diario", parte.idParteDiario)
                put("fecha", parte.fecha)
                put("equipo_id", parte.equipoId)
                put("horas_inicio", parte.horasInicio)
                put("horas_fin", parte.horasFin)
                put("horas_trabajadas", parte.horasTrabajadas)
                put("observaciones", parte.observaciones)
                put("obra_id", parte.obraId)
                put("user_updated", parte.userUpdated)
                put("estado_id", parte.estadoId)
                put("combustible_tipo", parte.combustibleTipo)
                put("combustible_cant", parte.combustibleCant)
                put("aceite_motor_cant", parte.aceiteMotorCant)
                put("aceite_hidra_cant", parte.aceiteHidraCant)
                put("aceite_otro_cant", parte.aceiteOtroCant)
                put("engrase_general", parte.engraseGeneral)
                put("filtro_aire", parte.filtroAire)
                put("filtro_aceite", parte.filtroAceite)
                put("filtro_comb", parte.filtroComb)
                put("filtro_otro", parte.filtroOtro)
            }

            val requestBody = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val response = apiService.updateParteDiario(requestBody)
            handleResponse(response)
        } catch (e: Exception) {
            Log.e("ParteDiarioRepository", "Error al actualizar parte: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    // Eliminar parte diario
    suspend fun deleteParteDiario(idParteDiario: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val empresaDbName = sessionManager.getEmpresaData()?.db_name
                ?: return@withContext Result.failure<Unit>(Exception("Empresa DB no especificado."))

            val response = apiService.deleteParteDiario(idParteDiario, empresaDbName)
            handleResponse(response)
        } catch (e: Exception) {
            Log.e("ParteDiarioRepository", "Excepción al eliminar parte: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    // Obtener todos los partes
    suspend fun fetchPartes(parteFiltro: String = ""): Result<List<ParteDiario>> = withContext(Dispatchers.IO) {
        try {
            val empresaDbName = sessionManager.getEmpresaData()?.db_name
                ?: return@withContext Result.failure<List<ParteDiario>>(Exception("Empresa DB no especificado."))

            val jsonObject = JSONObject().apply {
                put("empresaDbName", empresaDbName)
                if (parteFiltro.isNotEmpty()) put("parteFiltro", parteFiltro)
            }
            val requestBody = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            val response = apiService.getAllPartesDiarios(requestBody)
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Obtener parte por ID
    suspend fun fetchParteById(idParteDiario: Int): Result<ParteDiario> = withContext(Dispatchers.IO) {
        try {
            val requestBody = createRequestBody(idParteDiario)
            val response = apiService.getParteDiarioById(requestBody)
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Maneja la respuesta y simplifica el código de manejo de `Result` para todas las funciones
    private fun <T> handleResponse(response: Response<ApiResponse<T>>): Result<T> {
        return if (response.isSuccessful) {
            val apiResponse = response.body()
            Log.d("ParteDiarioRepository", "API Response: success=${apiResponse?.success}, data=${apiResponse?.data}, message=${apiResponse?.message}")

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
            Log.e("ParteDiarioRepository", "Error HTTP: $errorMessage")
            Result.failure(Exception("Error HTTP: $errorMessage"))
        }
    }

    // Crea un RequestBody para obtener parte por ID
    private fun createRequestBody(idParteDiario: Int): RequestBody {
        val empresaDbName = sessionManager.getEmpresaData()?.db_name ?: ""
        val json = """{ "empresaDbName": "$empresaDbName", "id": $idParteDiario }"""
        return json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
    }
}
