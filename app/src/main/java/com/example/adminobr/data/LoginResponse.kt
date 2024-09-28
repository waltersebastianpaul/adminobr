package com.example.adminobr.data

data class LoginResponse(
    val success: Boolean,
    val message: String?,
    val user: Usuario,
)