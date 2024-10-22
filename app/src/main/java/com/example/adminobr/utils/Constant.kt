package com.example.adminobr.utils

import android.util.Log
import com.example.adminobr.BuildConfig
import java.net.URL

sealed class Constants {

    companion object {

        // Configuración de la URL base
        private const val DEBUG = true // Cambiar a true en desarrollo, false en producción

        private const val BASE_URL = "http://adminobr.site"
        private const val DEBUG_DIR = "/debug/"
        private const val RELEASE_DIR = "/"
        private var CURRENT_DIR = ""

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
    }

    object Update {
        val UPDATE_DIR = buildUrl("updates/apk/")
        // ... otras rutas de obras si es necesario
    }

    object PartesDiarios {
        const val GET_LISTA = "api/equipos/partes/get_partes_diarios.php"
        const val GUARDAR_PARTE_DIARIO = "api/equipos/partes/guardar_parte_diario.php"
        const val GET_ULTIMO_PARTE = "api/equipos/partes/get_ultimo_parte_diario.php"
        const val GET_ULTIMOS_PARTES = "api/equipos/partes/get_ultimo_parte_diario.php"
        // ... otras rutas de obras si es necesario
    }

    object Equipos {
        const val GET_LISTA = "api/equipos/get_equipos.php"
        // ... otras rutas de obras si es necesario
    }

    object Obras {
        const val GET_LISTA = "api/obras/get_obras.php"
        // ... otras rutas de obras si es necesario
    }

    object Estados {
        const val GET_LISTA = "api/estados/get_estados.php"
        // ... otras rutas de obras si es necesario
    }

    object Empresas {
        const val GET_LISTA = "api/empresas/get_empresas.php"
        // ... otras rutas de obras si es necesario
    }

    object Auth {
        const val LOGIN = "api/auth/login.php"
        const val LOGOUT = "api/auth/logout.php"
        // ... otras rutas de obras si es necesario
    }

    object Roles {
        const val GET_LISTA = "api/roles/get_roles.php"
        // ... otras rutas de obras si es necesario
    }

    object Usuarios {
        const val GET_LISTA = "api/usuarios/get_usuarios.php"
        const val GUARDAR = "api/usuarios/guardar_usuario.php" // Nueva ruta
        const val ACTUALIZAR = "api/usuarios/actualizar_usuario.php" // Nueva ruta
        // ... otras rutas para actualizar, eliminar, etc.
    }
}
