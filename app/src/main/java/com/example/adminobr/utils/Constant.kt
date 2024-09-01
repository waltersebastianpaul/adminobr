package com.example.adminobr.utils

import java.net.URL

sealed class Constants {

    companion object {
        private const val BASE_URL = "http://adminobr.site"

        fun getBaseUrl(): String {
            return BASE_URL
        }

        // Función auxiliar para construir URLs completas
        fun buildUrl(path: String): URL {
            return URL("$BASE_URL$path")
        }
    }

    object Update {
        const val DEBUG_DIR = "${BASE_URL}/updates/apk/debug/"
        const val RELEASE_DIR = "${BASE_URL}/updates/apk/release/"
        // ... otras rutas de obras si es necesario
    }


    object PartesDiarios {
        const val GET_LISTA = "/api/equipos/partes/get_partes_diarios.php"
        const val GUARDAR = "/api/equipos/partes/guardar_parte_diario.php"
        const val GET_ULTIMO_PARTE = "/api/equipos/partes/get_ultimo_parte_diario.php"
        // ... otras rutas de obras si es necesario
    }

    object Equipos {
        const val GET_LISTA = "/api/equipos/get_equipos.php"
        // ... otras rutas de obras si es necesario
    }

    object Obras {
        const val GET_LISTA = "/api/obras/get_obras.php"
        // ... otras rutas de obras si es necesario
    }

    object Estados {
        const val GET_LISTA = "/api/estados/get_estados.php"
        // ... otras rutas de obras si es necesario
    }

    object Empresas {
        const val GET_LISTA = "/api/empresas/get_empresas.php"
        // ... otras rutas de obras si es necesario
    }

    object Auth {
        const val LOGIN = "/api/auth/login.php"
        const val LOGOUT = "/api/auth/logout.php"
        // ... otras rutas de obras si es necesario
    }

    object Roles {
        const val GET_LISTA = "/api/roles/get_roles.php"
        // ... otras rutas de obras si es necesario
    }

    object Usuarios {
        const val GET_LISTA = "/api/usuarios/get_usuarios.php"
        const val GUARDAR = "/api/usuarios/guardar_usuario.php" // Nueva ruta
        // ... otras rutas para actualizar, eliminar, etc.
    }
}
