package com.example.adminobr.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.adminobr.data.Empresa
import com.example.adminobr.data.Usuario
import com.google.gson.Gson

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_EMPRESA = "empresa"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_LEGAJO = "user_legajo"
        private const val KEY_USER_NOMBRE = "user_nombre"
        private const val KEY_USER_APELLIDO = "user_apellido"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PASSWORD = "user_password"
        private const val KEY_USER_ROL = "user_rol"
        private const val KEY_USER_PERMISOS = "user_permisos"

        private const val KEY_DEBUGGABLE = "false"
    }

    // Métodos para guardar y obtener los datos de la empresa como objeto `Empresa`
    fun saveDebuggable(debuggable: Boolean) {
        val editor = prefs.edit()
        editor.putBoolean(KEY_DEBUGGABLE, debuggable)
        editor.apply()
    }

    fun getDebuggable(): Boolean {
        return prefs.getBoolean(KEY_DEBUGGABLE, false)
    }

    // Métodos para guardar y obtener los datos de la empresa como objeto `Empresa`
    fun saveEmpresaData(empresa: Empresa) {
        val editor = prefs.edit()
        val empresaJson = gson.toJson(empresa)
        editor.putString(KEY_EMPRESA, empresaJson)
        editor.apply()
    }

    fun getEmpresaData(): Empresa? {
        val empresaJson = prefs.getString(KEY_EMPRESA, null)
        return if (empresaJson != null) {
            gson.fromJson(empresaJson, Empresa::class.java)
        } else {
            null
        }
    }

    // Métodos para guardar y obtener los datos del usuario
    //fun saveUserData(id: Int, nombre: String, apellido: String, email: String, rol: List<String>, permisos: List<String>) {
    fun saveUserData(usuario: Usuario) {
        val editor = prefs.edit()
        editor.putInt(KEY_USER_ID, usuario.id ?: -1)
        editor.putString(KEY_USER_LEGAJO, usuario.legajo)
        editor.putString(KEY_USER_NOMBRE, usuario.nombre)
        editor.putString(KEY_USER_APELLIDO, usuario.apellido)
        editor.putString(KEY_USER_EMAIL, usuario.email)
        editor.putStringSet(KEY_USER_ROL, usuario.rol?.toSet())
        editor.putStringSet(KEY_USER_PERMISOS, usuario.permisos?.toSet())
        editor.apply()
    }

    fun getUserId(): Int {
        return prefs.getInt(KEY_USER_ID, -1)
    }

    fun getUserNombre(): String? {
        return prefs.getString(KEY_USER_NOMBRE, null)
    }

    fun getUserApellido(): String? {
        return prefs.getString(KEY_USER_APELLIDO, null)
    }

    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    fun getUserRol(): List<String>? {
        return prefs.getStringSet(KEY_USER_ROL, null)?.toList()
    }

    fun getUserPermisos(): List<String>? {
        return prefs.getStringSet(KEY_USER_PERMISOS, null)?.toList()
    }

    fun clearSession() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
}

