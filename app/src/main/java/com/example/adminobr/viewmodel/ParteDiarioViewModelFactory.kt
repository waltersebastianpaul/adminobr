package com.example.adminobr.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ParteDiarioViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ParteDiarioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ParteDiarioViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}