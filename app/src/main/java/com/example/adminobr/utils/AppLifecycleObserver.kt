package com.example.adminobr.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class AppLifecycleObserver(private val onAppForeground: (Boolean) -> Unit) :
    DefaultLifecycleObserver {
    private var isForeground = false

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (!isForeground) {
            isForeground = true
            onAppForeground(true) // La app está en primer plano
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if (isForeground) {
            isForeground = false
            onAppForeground(false) // La app está en segundo plano
        }
    }
}
