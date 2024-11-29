package com.example.adminobr.ui.partediario

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.adminobr.R
import com.example.adminobr.data.ParteDiario

class ParteDiarioDetalleDialog(
    private val parteDiario: ParteDiario
) : DialogFragment() {

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_parte_diario_detalle, container, false)

        view.findViewById<TextView>(R.id.obraTextView).text = "Obra: ${parteDiario.obraCentroCosto ?: "N/A"} - ${parteDiario.obraNombre ?: "N/A"}"
        view.findViewById<TextView>(R.id.fechaTextView).text = "Fecha: ${parteDiario.fecha ?: ""}"
        view.findViewById<TextView>(R.id.equipoTextView).text = "Equipo: ${parteDiario.equipoInterno ?: ""}"
        view.findViewById<TextView>(R.id.horasInicioTextView).text = "Inicio: ${parteDiario.horasInicio ?: ""}"
        view.findViewById<TextView>(R.id.horasFinTextView).text = "Fin: ${parteDiario.horasFin ?: ""}"
        view.findViewById<TextView>(R.id.observacionesTextView).text = "Observaciones: ${parteDiario.observaciones ?: ""}"

        // Consolidación de combustible y mantenimiento en un solo bloque
        val mantenimientoInfo = buildString {
            if (parteDiario.combustibleCant != null && parteDiario.combustibleCant != 0) {
                append("Combustible: ${parteDiario.combustibleTipo ?: ""}\n")
                append("Cantidad: ${parteDiario.combustibleCant} L\n")
            }
            if (parteDiario.aceiteMotorCant != null && parteDiario.aceiteMotorCant != 0) {
                append("Aceite Motor: ${parteDiario.aceiteMotorCant} L\n")
            }
            if (parteDiario.aceiteHidraCant != null && parteDiario.aceiteHidraCant != 0) {
                append("Aceite Hidráulico: ${parteDiario.aceiteHidraCant} L\n")
            }
            if (parteDiario.aceiteOtroCant != null && parteDiario.aceiteOtroCant != 0) {
                append("Aceite Otro: ${parteDiario.aceiteOtroCant} L\n")
            }
            
            if (parteDiario.engraseGeneral == 1) append("Engrase General: Sí\n")
            if (parteDiario.filtroAire == 1) append("Filtro Aire: Sí\n")
            if (parteDiario.filtroAceite == 1) append("Filtro Aceite: Sí\n")
            if (parteDiario.filtroComb == 1) append("Filtro Combustible: Sí\n")
            if (parteDiario.filtroOtro == 1) append("Otro Filtro: Sí\n")
        }.trim()

        view.findViewById<TextView>(R.id.mantenimientoTextView).text = mantenimientoInfo
        // Configurar botón de cierre en la esquina superior derecha
        view.findViewById<ImageButton>(R.id.closeImageButton).setOnClickListener { dismiss() }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }
}