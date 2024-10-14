package com.example.adminobr.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.example.adminobr.data.Empresa
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
        private const val KEY_USER_ROLES = "user_rol"
        private const val KEY_USER_PRINCIPAL_ROL = "user_principal_rol"
        private const val KEY_USER_PERMISOS = "user_permisos"
        private const val KEY_DEBUGGABLE = "false"
        private const val KEY_PENDING_UPDATE_URL = "pending_update_url"
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
        return prefs.getStringSet(KEY_USER_ROLES, null)?.toList()
    }

    fun getUserPrincipalRol(): String? {
        return prefs.getString(KEY_USER_PRINCIPAL_ROL, null)
    }

    fun getUserPermisos(): List<String>? {
        return prefs.getStringSet(KEY_USER_PERMISOS, null)?.toList()
    }

    fun clearSession() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }

    @OptIn(UnstableApi::class)
    fun saveUserDetails(
        id: Int,
        legajo: String,
        nombre: String,
        apellido: String,
        roles: List<String>?,
        principalRole: String?
    ) {
        clearUserDetails()
        val editor = prefs.edit()
        editor.putInt(KEY_USER_ID, id)
        editor.putString(KEY_USER_LEGAJO, legajo)
        editor.putString(KEY_USER_NOMBRE, nombre)
        editor.putString(KEY_USER_APELLIDO, apellido)
        editor.putStringSet(KEY_USER_ROLES, roles?.toSet()) // Guardar la lista de roles como un Set<String>
        editor.putString(KEY_USER_PRINCIPAL_ROL, principalRole) // Guardar el rol principal como una cadena
        editor.apply()

        // Log para verificar los datos que se guardan
        Log.d("SessionManager", "Datos guardados en SharedPreferences:")
        Log.d("SessionManager", "ID: $id")
        Log.d("SessionManager", "Legajo: $legajo")
        Log.d("SessionManager", "Nombre: $nombre")
        Log.d("SessionManager", "Apellido: $apellido")
        Log.d("SessionManager", "Roles: $roles")
        Log.d("SessionManager", "Principal Rol: $principalRole")
    }

    @OptIn(UnstableApi::class)
    fun getUserDetails(): Map<String, String?> {
        // Obtener los datos guardados
        val id = prefs.getInt(KEY_USER_ID, -1)
        val legajo = prefs.getString(KEY_USER_LEGAJO, null)
        val nombre = prefs.getString(KEY_USER_NOMBRE, null)
        val apellido = prefs.getString(KEY_USER_APELLIDO, null)
        val roles = prefs.getStringSet(KEY_USER_ROLES, null)?.toList()
        val principalRole = prefs.getString(KEY_USER_PRINCIPAL_ROL, null)

        // Log para verificar qué datos se recuperan
        Log.d("SessionManager", "Datos recuperados de SharedPreferences:")
        Log.d("SessionManager", "ID: $id")
        Log.d("SessionManager", "Legajo: $legajo")
        Log.d("SessionManager", "Nombre: $nombre")
        Log.d("SessionManager", "Apellido: $apellido")
        Log.d("SessionManager", "Roles: $roles")
        Log.d("SessionManager", "Rol Principal: $principalRole")

        return mapOf(
            "id" to id.toString(),
            "legajo" to legajo,
            "nombre" to nombre,
            "apellido" to apellido,
            "roles" to roles?.joinToString(", "),
            "principalRole" to principalRole
        )
    }

    @OptIn(UnstableApi::class)
    fun clearUserDetails() {
        val editor = prefs.edit()
        editor.remove(KEY_USER_ID)
        editor.remove(KEY_USER_LEGAJO)
        editor.remove(KEY_USER_NOMBRE)
        editor.remove(KEY_USER_APELLIDO)
        editor.remove(KEY_USER_ROLES)
        editor.remove(KEY_USER_PRINCIPAL_ROL)
        editor.apply()

        // Log para confirmar que los datos se han eliminado
        Log.d("SessionManager", "Datos de usuario eliminados de SharedPreferences")
    }

    fun savePendingUpdateUrl(url: String) {
        val editor = prefs.edit()
        editor.putString(KEY_PENDING_UPDATE_URL, url)
        editor.apply()
    }

    fun getPendingUpdateUrl(): String? {
        return prefs.getString(KEY_PENDING_UPDATE_URL, null)
    }

    fun clearPendingUpdateUrl() {
        val editor = prefs.edit()
        editor.remove(KEY_PENDING_UPDATE_URL)
        editor.apply()
    }

}

