package com.example.adminobr.data

data class Usuario(
    var isEditing: Boolean = false,
    val id: Int? = null,
    val legajo: String = "",
    val dni: String = "",
    val nombre: String = "",
    val apellido: String = "",
    val email: String = "",
    val telefono: String = "",
    val userCreated: Int? = null,
    val password: String = "",
    val estadoId: Int = 1,
    val roleId: Int? = null,
    val roles: List<String>? = null,
    val principalRole: String? = null
//    val permisos: List<String>? = null,
)
