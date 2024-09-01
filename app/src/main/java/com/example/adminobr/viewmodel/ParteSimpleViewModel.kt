package com.example.adminobr.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.adminobr.data.ParteSimple

class ParteSimpleViewModel : ViewModel() {

    private val _partesList = MutableLiveData<List<ParteSimple>>(emptyList())
    val partesList: LiveData<List<ParteSimple>> = _partesList

    fun addParte(parte: ParteSimple) {
        val currentList = _partesList.value.orEmpty().toMutableList()
        currentList.add(0, parte)
        _partesList.value = currentList
    }

    fun clearPartesList() {
        _partesList.value = emptyList()
    }

    fun removeParte(position: Int) {
        val currentList = _partesList.value.orEmpty().toMutableList()
        currentList.removeAt(position)
        _partesList.value = currentList
    }

    fun setPartesList(partes: List<ParteSimple>) {
        _partesList.value = partes
    }

}