package com.example.adminobr.utils

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.net.ParseException
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object AppUtils {

    // Función para cerrar el teclado

    // Llamar la función desde la Activity
    /* AppUtils.closeKeyboard(this, currentFocus) */

    // Llamar la función desde el Fragment
    /* AppUtils.closeKeyboard(requireActivity(), view) */

    fun closeKeyboard(context: Context, view: View? = null) {
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val targetView = view ?: (context as? Activity)?.currentFocus ?: (context as? Activity)?.window?.decorView
        targetView?.let {
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }

        // Opcionalmente, puedes quitar el foco de la vista actual
        (context as? Activity)?.currentFocus?.clearFocus()
    }

    fun clearFocus(context: Context) {
        // En tu Activity:
        // AppUtils.clearFocus(this)

        // En tu Fragment:
        // AppUtils.clearFocus(requireContext())

        val view = when (context) {
            is Activity -> context.currentFocus
            is Fragment -> context.view?.findFocus()
            else -> null
        }
        view?.clearFocus()
    }

    fun showDatePickerDialog(context: Context, editText: EditText, onDateSetListener: DatePickerDialog.OnDateSetListener) {
        val locale = Locale.getDefault()
        val calendar = Calendar.getInstance(locale)
        val dateString = editText.text.toString()

        // Intenta analizar la fecha solo si no está vacía y tiene el formato correcto
        if (dateString.isNotBlank() && dateString.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))) {
            try {
                val formatter = SimpleDateFormat("dd/MM/yyyy", locale)
                val date = formatter.parse(dateString)
                date?.let { calendar.time = it }
            } catch (e: ParseException) {
                Log.e("AppUtils", "Error al analizar la fecha: ${e.message}")
                // Si hay un error al analizar, usa la fecha actual
            }
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            context,
            onDateSetListener,
            year,
            month,
            day
        )
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

}