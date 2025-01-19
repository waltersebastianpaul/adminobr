package com.example.adminobr.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.adminobr.data.Obra
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_VERSION = "last_version" // Nueva clave para almacenar la versión
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_EMPRESA = "empresa"
        private const val KEY_EMPRESA_CODE = "empresa_code"
        private const val KEY_EMPRESA_NAME = "empresa_name"
        private const val KEY_EMPRESA_DB_NAME = "empresa_db_name"
        private const val KEY_OBRA_ID = "obra_id"
        private const val KEY_OBRA_NOMBRE = "obra_nombre"
        private const val KEY_OBRA_CENTRO_COSTO = "obra_centro_costo"
        private const val KEY_OBRA_LIST = "obra_list"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_LEGAJO = "user_legajo"
        private const val KEY_USER_NOMBRE = "user_nombre"
        private const val KEY_USER_APELLIDO = "user_apellido"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_TELEFONO = "user_telefono"
        private const val KEY_USER_ROLES = "user_rol"
        private const val KEY_USER_PRINCIPAL_ROL = "user_principal_rol"
        private const val KEY_USER_PERMISOS = "user_permisos"

        private const val KEY_DEBUGGABLE = "debuggable"
    }

    fun saveLastVersion(version: String) {
        val editor = prefs.edit()
        editor.putString(KEY_LAST_VERSION, version)
        editor.apply()
    }

    fun getLastVersion(): String? {
        return prefs.getString(KEY_LAST_VERSION, null)
    }

    fun saveAuthToken(token: String) {
        val editor = prefs.edit()
        editor.putString(KEY_AUTH_TOKEN, token)
        editor.apply()
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun clearAuthToken() {
        val editor = prefs.edit()
        editor.remove(KEY_AUTH_TOKEN)
        editor.apply()
    }

    fun logout() {
        clearAuthToken() // Borra el token
        //clearUserDetails() // Opcional: borra otros datos relacionados al usuario
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
    fun saveEmpresaData(
        empresaCode: String?,
        empresaName: String?,
        empresaDbName: String?
    ) {
        if (empresaCode == null || empresaName == null || empresaDbName == null) {
            Log.e("SessionManager", "Intento de guardar datos nulos: $empresaCode, $empresaName, $empresaDbName")
            return
        }

        clearUserDetails()
        val editor = prefs.edit()
        editor.putString(KEY_EMPRESA_CODE, empresaCode)
        editor.putString(KEY_EMPRESA_NAME, empresaName)
        editor.putString(KEY_EMPRESA_DB_NAME, empresaDbName)
        editor.apply()

        Log.d("SessionManager", "Datos de la empresa guardados: $empresaCode, $empresaName, $empresaDbName")
    }

    fun getEmpresaData(): Map<String, String?> {
        // Obtener los datos guardados
        val empresaCode = prefs.getString(KEY_EMPRESA_CODE, null)
        val empresaName = prefs.getString(KEY_EMPRESA_NAME, null)
        val empresaDbName = prefs.getString(KEY_EMPRESA_DB_NAME, null)

        return mapOf(
            "empresaCode" to empresaCode,
            "empresaName" to empresaName,
            "empresaDbName" to empresaDbName
        )
    }

    fun getEmpresaDbName(): String? {
        return prefs.getString(KEY_EMPRESA_DB_NAME, null)
    }

    fun clearEmpresaData() {
        val editor = prefs.edit()
        editor.remove(KEY_EMPRESA)
        editor.remove(KEY_EMPRESA_CODE)
        editor.remove(KEY_EMPRESA_NAME)
        editor.remove(KEY_EMPRESA_DB_NAME)
        editor.apply()

        // Log para confirmar que los datos se han eliminado
        Log.d("SessionManager", "Datos de empresa eliminados de SharedPreferences")
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

    fun saveUserDetails(
        id: Int,
        legajo: String,
        nombre: String,
        apellido: String,
        roles: List<String>?,
        principalRole: String?,
        email: String?,
        telefono: String?
    ) {
        val editor = prefs.edit()
        editor.putInt(KEY_USER_ID, id)
        editor.putString(KEY_USER_LEGAJO, legajo)
        editor.putString(KEY_USER_NOMBRE, nombre)
        editor.putString(KEY_USER_APELLIDO, apellido)
        editor.putStringSet(KEY_USER_ROLES, roles?.toSet()) // Guardar la lista de roles como un Set<String>
        editor.putString(KEY_USER_PRINCIPAL_ROL, principalRole) // Guardar el rol principal como una cadena
        editor.putString(KEY_USER_EMAIL, email)
        editor.putString(KEY_USER_TELEFONO, telefono)
        editor.apply()

    }

    fun getUserDetails(): Map<String, String?> {
        // Obtener los datos guardados
        val id = prefs.getInt(KEY_USER_ID, -1)
        val legajo = prefs.getString(KEY_USER_LEGAJO, null)
        val nombre = prefs.getString(KEY_USER_NOMBRE, null)
        val apellido = prefs.getString(KEY_USER_APELLIDO, null)
        val roles = prefs.getStringSet(KEY_USER_ROLES, null)?.toList()
        val principalRole = prefs.getString(KEY_USER_PRINCIPAL_ROL, null)

        return mapOf(
            "id" to id.toString(),
            "legajo" to legajo,
            "nombre" to nombre,
            "apellido" to apellido,
            "roles" to roles?.joinToString(", "),
            "principalRole" to principalRole
        )
    }

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


    fun saveObraData(obraId: Int, obraNombre: String, centroCosto: String) {
        val editor = prefs.edit()
        editor.putInt(KEY_OBRA_ID, obraId)
        editor.putString(KEY_OBRA_NOMBRE, obraNombre)
        editor.putString(KEY_OBRA_CENTRO_COSTO, centroCosto)
        editor.apply()
        Log.d("SessionManager", "Obra guardada: $obraNombre ($centroCosto)")
    }

    fun getObraData(): Map<String, String?> {
        val obraId = prefs.getInt(KEY_OBRA_ID, -1)
        val obraNombre = prefs.getString(KEY_OBRA_NOMBRE, null)
        val centroCosto = prefs.getString(KEY_OBRA_CENTRO_COSTO, null)

        return mapOf(
            "obraId" to if (obraId != -1) obraId.toString() else null,
            "obraNombre" to obraNombre,
            "centroCosto" to centroCosto
        )
    }

    fun clearObraData() {
        val editor = prefs.edit()
        editor.remove(KEY_OBRA_ID)
        editor.remove(KEY_OBRA_NOMBRE)
        editor.remove(KEY_OBRA_CENTRO_COSTO)
        editor.remove(KEY_OBRA_LIST)
        editor.apply()
        Log.d("SessionManager", "Datos de obra eliminados")
    }

    // Guardar lista de obras (convertimos a JSON usando Gson)
    fun saveObraList(obras: List<Obra>) {
        val obrasJson = Gson().toJson(obras)
        prefs.edit().putString(KEY_OBRA_LIST, obrasJson).apply()
    }

    // Recuperar lista de obras
    fun getObraList(): List<Obra>? {
        val obrasJson = prefs.getString(KEY_OBRA_LIST, null)
        return if (obrasJson != null) {
            val type = object : TypeToken<List<Obra>>() {}.type
            Gson().fromJson(obrasJson, type)
        } else {
            null
        }
    }

    // Eliminar las claves específicas
    /**
     * Uso:
     * sessionManager.removeKeys("last_version") // agregar una o varias key a eliminar
     */
    fun removeKeys(vararg keys: String) {
        val editor = prefs.edit()
        for (key in keys) {
            editor.remove(key)
        }
        editor.apply()
    }

}

