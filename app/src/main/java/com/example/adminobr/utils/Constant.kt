package com.example.adminobr.utils

import android.util.Log
import com.example.adminobr.BuildConfig
import java.net.URL

sealed class Constants {

    companion object {

        // Establece el modo de depuración
        private const val DEBUG = true // Cambiar a true en desarrollo, false en producción
        // Establece el modo de red de depuración
        private const val NETWORK_STATUS_HELPER = true // Cambiar a true en desarrollo, false en producción

        // Configuración de la URL base
        private const val BASE_URL = "http://adminobr.site"
        private const val DEBUG_DIR = "/debug/"
        private const val RELEASE_DIR = "/"
        private var CURRENT_DIR = ""

        // Inicializa la dirección actual según el modo de depuración
        init {
            CURRENT_DIR = if (BuildConfig.DEBUG && DEBUG) DEBUG_DIR else RELEASE_DIR
        }

        fun getBaseUrl(): URL {
            val url = URL("$BASE_URL$CURRENT_DIR")
            Log.d("Constants", "Current DIR: $url")
            return url
        }

        // Función auxiliar para construir URLs completas
        fun buildUrl(path: String): URL {
            val url = URL("$BASE_URL$CURRENT_DIR$path")
            Log.d("Constants", "Built URL: $url")
            return url
        }

        fun getNetworkStatusHelper(): Boolean {
            return NETWORK_STATUS_HELPER
        }
    }

    object Update {
        val UPDATE_DIR = buildUrl("updates/apk/")
    }

    object PartesDiarios {
        const val CREATE = "api/equipos/partes/create.php"
        const val UPDATE = "api/equipos/partes/update.php"
        const val DELETE = "api/equipos/partes/delete.php"
        const val GET_BY_EQUIPO = "api/equipos/partes/getByEquipoId.php"
        const val GET_BY_USER = "api/equipos/partes/getByUserId.php"
        const val GET_BY_ID = "api/equipos/partes/getById.php"
        const val GET_ALL = "api/equipos/partes/getAll.php"
    }

    object Equipos {
        const val GET_LISTA = "api/equipos/getEquipos.php"
    }

    object Obras {
        const val GET_LISTA = "api/obras/getObras.php"
    }

    object Estados {
        const val GET_LISTA = "api/estados/getEstados.php"
    }

    object Empresas {
        const val GET_LISTA = "api/empresas/getEmpresas.php"
    }

    object Auth {
        const val LOGIN = "api/auth/login.php"
        const val LOGOUT = "api/auth/logout.php"
    }

    object Roles {
        const val GET_LISTA = "api/roles/getRoles.php"
        const val SET_ROL = "api/roles/asignarRolUsuario.php"
    }

    object Usuarios {
        const val GET_ALL = "api/usuarios/getAll.php"
        const val CREATE = "api/usuarios/create.php"
        const val UPDATE = "api/usuarios/update.php"
        const val DELETE = "api/usuarios/delete.php"
        const val GET_BY_ID = "api/usuarios/getById.php"
    }
}
