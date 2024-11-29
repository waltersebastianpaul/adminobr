package com.example.adminobr.utils

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import java.util.*

class DatePickerRangeDialog(private val context: Context, private val onDateRangeSelected: (String, String) -> Unit) : DialogFragment(), DatePickerDialog.OnDateSetListener {

    private var isStartDateSelected = true
    private var startDate: String = ""
    private var endDate: String = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): DatePickerDialog {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        return DatePickerDialog(context, this, year, month, day)
    }

    @SuppressLint("DefaultLocale")
    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        val formattedDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
        if (isStartDateSelected) {
            startDate = formattedDate
            isStartDateSelected = false
            DatePickerDialog(context, this, year, month, dayOfMonth).show()
        } else {
            endDate = formattedDate
            onDateRangeSelected(startDate, endDate)
        }
    }
}
