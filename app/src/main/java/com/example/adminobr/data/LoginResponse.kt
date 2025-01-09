package com.example.adminobr.data

data class LoginResponse(
    val success: Boolean,
    val message: String?,
    val user: Usuario?,
    val token: String?,
    val errorCode: String? // Nuevo campo para el código de error
)