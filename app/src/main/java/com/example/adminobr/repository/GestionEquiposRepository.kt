//package com.example.gestionequipos.repository
//
//import android.app.Application
//import com.example.gestionequipos.api.AutocompletesApi
//import com.example.gestionequipos.data.Empresa
//import com.example.gestionequipos.data.Equipo
//import com.example.gestionequipos.data.Obra
//
//class GestionEquiposRepository(application: Application) {
//    private val api = AutocompletesApi.create(application)
//
//    suspend fun getEquipos(): List<Equipo> {
//        return api.getEquipos()
//    }
//
//    suspend fun getObras(): List<Obra> {
//        return api.getObras()
//    }
//
//    suspend fun getEmpresas(): List<Empresa> {
//        return api.getEmpresas()
//    }
//}