package com.example.adminobr.data

data class LoginRequest(
    val usuario: String,
    val password: String,
    val empresaDbName: String
)