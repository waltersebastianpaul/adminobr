package com.example.adminobr.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class UsuarioViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UsuarioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UsuarioViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
