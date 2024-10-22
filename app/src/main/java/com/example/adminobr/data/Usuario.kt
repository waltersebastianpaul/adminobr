package com.example.adminobr.data

data class Usuario(
    val id: Int? = null,
    val legajo: String,
    val dni: String,
    val nombre: String,
    val apellido: String,
    val email: String,
    val telefono: String,
    val userCreated: Int? = null,
    val password: String = "",
    val estadoId: Int,
    val roles: List<String>? = null, // Asegúrate de que sea List<String>?
    val principalRole: String? = null, // Asegúrate de que sea String?
    val permisos: List<String>? = null // Asegúrate de que sea List<String>?
)

//data class Usuario(
//    val id: Int? = null,
//    val legajo: String,
//    val dni: String,
//    val nombre: String,
//    val apellido: String,
//    val email: String,
//    val telefono: String,
//    val userCreated: Int,
//    val password: String = "",
//    val estadoId: Int,
//    val roles: List<String>? = null,
//    val principalRole: String? = null,
//    val permisos: List<String>? = null
//)
//

// Para agregar algun otro dato del user, se debe agregar el campo
// no solo en la clase User, sino tambien el el Backend, en la API login.php


// Obtiene el ID del user del Intent
// Luego en cualquier parte

// String
// val email = intent.getStringExtra("email", "No hay email disponible")

// Integer
// val userId = requireActivity().intent.getIntExtra("id", -1) // -1 como valor predeterminado si no se encuentra


// val email = intent.getStringExtra("email") ?: "No hay email disponible"

// Desde Fragmento
// Importante, si se trabaja desde un Fragmento, usarrequireActivity().intent
// val userId = requireActivity().intent.getIntExtra("id", -1) // -1 como valor predeterminado si no se encuentra
