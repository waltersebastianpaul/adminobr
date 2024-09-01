package com.example.adminobr.application

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

class MyApplication : Application() {

    private val viewModelStore: ViewModelStore = ViewModelStore()

    private val viewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore
            get() = this@MyApplication.viewModelStore
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("MyApplication", "onCreate: Iniciando aplicación")


    }

    override fun onTerminate() {
        super.onTerminate()
        viewModelStore.clear()
    }

}