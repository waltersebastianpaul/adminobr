package com.example.adminobr.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.adminobr.data.ParteDiario
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SharedPreferencesHelper {
    private const val PREFS_NAME = "ParteDiarioPrefs"
    private const val PARTES_LIST_KEY = "partesList"

    fun savePartesList(context: Context, partesList: List<ParteDiario>) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(partesList)
        editor.putString(PARTES_LIST_KEY, json)
        editor.apply()
    }

    fun getPartesList(context: Context): List<ParteDiario> {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString(PARTES_LIST_KEY, null)
        val type = object : TypeToken<List<ParteDiario>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun clearPartesList(context: Context) {
        val sharedPreferences =context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove(PARTES_LIST_KEY)
        editor.apply()
    }
}
