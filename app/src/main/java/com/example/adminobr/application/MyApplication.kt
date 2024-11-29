package com.example.adminobr.application

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.adminobr.utils.NetworkStatusHelper

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Establecer el modo noche por defecto
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Inicializar NetworkStatusHelper
        NetworkStatusHelper.initialize(this)

    }
}
