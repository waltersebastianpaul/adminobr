package com.example.adminobr.api

data class ApiResponse<T>(
    val data: T? = null,
    val success: Boolean = false,
    val message: String? = null,
    val id: Int? = null // Agregar campo id opcional
)
