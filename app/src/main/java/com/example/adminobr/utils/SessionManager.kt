package com.example.adminobr.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
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

    fun getUserLegajo(): String? {
        return prefs.getString(KEY_USER_LEGAJO, null)
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



// SessionManager.kt

    @OptIn(UnstableApi::class)
    fun saveUserDetails(legajo: String, nombre: String, apellido: String) {
        val editor = prefs.edit()
        editor.putString("user_legajo", legajo)
        editor.putString("user_nombre", nombre)
        editor.putString("user_apellido", apellido)
        editor.apply()

        // Log para verificar los datos que se guardan
        Log.d("SessionManager", "Datos guardados en SharedPreferences:")
        Log.d("SessionManager", "Legajo: $legajo")
        Log.d("SessionManager", "Nombre: $nombre")
        Log.d("SessionManager", "Apellido: $apellido")
    }

    @OptIn(UnstableApi::class)
    fun getUserDetails(): Map<String, String?> {
        // Obtener los datos guardados
        val legajo = prefs.getString("user_legajo", null)
        val nombre = prefs.getString("user_nombre", null)
        val apellido = prefs.getString("user_apellido", null)

        // Log para verificar qué datos se recuperan
        Log.d("SessionManager", "Datos recuperados de SharedPreferences:")
        Log.d("SessionManager", "Legajo: $legajo")
        Log.d("SessionManager", "Nombre: $nombre")
        Log.d("SessionManager", "Apellido: $apellido")

        return mapOf(
            "legajo" to legajo,
            "nombre" to nombre,
            "apellido" to apellido
        )
    }

    @OptIn(UnstableApi::class)
    fun clearUserDetails() {
        val editor = prefs.edit()
        editor.remove("user_legajo")
        editor.remove("user_nombre")
        editor.remove("user_apellido")
        editor.apply()

        // Log para confirmar que los datos se han eliminado
        Log.d("SessionManager", "Datos de usuario eliminados de SharedPreferences")
    }

}

