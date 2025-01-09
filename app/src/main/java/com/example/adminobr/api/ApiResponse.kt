package com.example.adminobr.api

data class ApiResponse<T>(
    val data: T? = null,
    val success: Boolean = false,
    val message: String? = null,
    val errorCode: String? = null,  // El código de error
    val id: Int? = null // Agregar campo id opcional
)
