package com.example.adminobr.data

sealed class ResultData<out T> {
    data class Success<out T>(
        val data: T
    ) : ResultData<T>()

    data class Error(
        val message: String?,
        val code: Int? = null,
        val errorCode: String? = null // Nuevo campo para `error_code`
    ) : ResultData<Nothing>()
}


