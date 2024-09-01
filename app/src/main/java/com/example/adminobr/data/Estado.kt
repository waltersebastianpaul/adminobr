package com.example.adminobr.data

data class Estado(
    val id: Int,
    val nombre: String) {
    override fun toString(): String {
        return nombre
    }
}