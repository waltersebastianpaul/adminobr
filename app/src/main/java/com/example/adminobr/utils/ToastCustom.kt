package com.example.adminobr.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.example.adminobr.R

object ToastCustom {
    fun makeText(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        val inflater = LayoutInflater.from(context)

        // Inflar la vista personalizada con un ViewGroup padre (null está bien si no se va a adjuntar de inmediato)
        val layout = inflater.inflate(R.layout.custom_toast, null as ViewGroup?)

        // Configurar el mensaje en la vista personalizada
        val textView: TextView = layout.findViewById(R.id.text)
        textView.text = message

        // Crear el Toast y establecer la vista personalizada
        Toast(context).apply {
            this.duration = duration
            this.view = layout
            show()
        }
    }
}

