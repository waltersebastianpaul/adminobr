package com.example.adminobr.utils

import android.app.DatePickerDialog
import android.content.Context
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.ui.semantics.text
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object AppUtils {

    // Función para cerrar el teclado

        // Llamar la función desde la Activity
        /* AppUtils.closeKeyboard(this, currentFocus) */

        // Llamar la función desde el Fragment
        /* AppUtils.closeKeyboard(requireActivity(), view) */

    fun closeKeyboard(context: Context, view: View?) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        view?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }


    fun showDatePickerDialog(context: Context, editText: EditText, onDateSetListener: DatePickerDialog.OnDateSetListener) {
        val locale = Locale.getDefault()
        val calendar = Calendar.getInstance(locale)
        val dateString = editText.text.toString() // Usar editText en lugar de fechaEditText

        if (dateString.isNotBlank()) {
            val formatter = SimpleDateFormat("dd/MM/yyyy", locale)
            val date = formatter.parse(dateString)
            date?.let { calendar.time = it }
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            context,
            onDateSetListener, // Pasar el listener como parámetro
            year,
            month,
            day
        )
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }
}