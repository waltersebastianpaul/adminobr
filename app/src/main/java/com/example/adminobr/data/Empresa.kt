package com.example.adminobr.data

class Empresa (
    val id: Int,
    val nombre: String,
    val code: String,
    val cuit: String,
    val db_name: String,
    val db_username: String,
    val db_password: String,
    val estado: String,
    val message: String?,
    val errorCode: String?
) {
    override fun toString(): String {
        return nombre
    }
}
