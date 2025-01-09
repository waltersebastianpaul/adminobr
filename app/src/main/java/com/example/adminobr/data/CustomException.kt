package com.example.adminobr.data

class CustomException(
    val errorCode: String?, message: String?
) : Exception(message)
