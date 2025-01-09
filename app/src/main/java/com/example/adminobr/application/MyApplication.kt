package com.example.adminobr.application

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.adminobr.utils.AppLifecycleObserver
import com.example.adminobr.utils.NetworkStatusHelper

class MyApplication : Application() {

    var isAppInForeground = false
        private set

    override fun onCreate() {
        super.onCreate()

        // Establecer el modo noche por defecto
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Inicializar NetworkStatusHelper
        NetworkStatusHelper.initialize(this)

        // Configurar el observador
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            AppLifecycleObserver { isForeground ->
                isAppInForeground = isForeground
            }
        )

    }
}
