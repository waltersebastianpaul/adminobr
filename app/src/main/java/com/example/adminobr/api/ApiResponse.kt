package com.example.adminobr.api

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?
)
