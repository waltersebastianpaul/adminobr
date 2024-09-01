package com.example.adminobr.data

class Empresa (
    val id: Int,
    val nombre: String,
    val cuit: String,
    val db_name: String,
    val db_username: String,
    val db_password: String,
    val estado: String) {
    override fun toString(): String {
        return "$nombre"
    }
}
