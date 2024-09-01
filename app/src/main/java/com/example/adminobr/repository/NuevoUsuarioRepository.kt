//package com.example.gestionequipos.repository
//
//
//import com.example.gestionequipos.api.ApiResponse
//import com.example.gestionequipos.apiimport.UsuariosApi
//import com.example.gestionequipos.data.Estado
//import com.example.gestionequipos.data.Rol
//import com.example.gestionequipos.data.Usuario
//
//
//class NuevoUsuarioRepository {
//    private val api = UsuariosApi.create()
//
//
//    suspend fun obtenerUsuarios(): List<Usuario> {
//        return api.getUsuarios()
//    }
//
//
//    suspend fun obtenerRoles(): List<Rol> {
//        return api.getRoles()
//    }
//
//
//    suspend fun obtenerEstados(): List<Estado> {
//        return api.getEstados()
//    }
//
//
//    suspend fun guardarUsuario(usuario: Usuario): ApiResponse {
//        return api.guardarUsuario(usuario)
//    }
//}