package com.example.adminobr.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.adminobr.data.ParteDiario
import com.example.adminobr.databinding.ItemParteDiarioBinding
import com.example.adminobr.utils.SharedPreferencesHelper

class ParteDiarioAdapter(private val context: Context) : ListAdapter<ParteDiario, ParteDiarioAdapter.ParteDiarioViewHolder>(
    ParteDiarioDiffCallback()
) {

    class ParteDiarioViewHolder(private val binding: ItemParteDiarioBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(parteDiario: ParteDiario) {
            binding.fechaTextView.text = "Fecha: ${parteDiario.fecha}"
            binding.equipoTextView.text = "Equipo: ${parteDiario.equipoInterno}"
            binding.horasInicioTextView.text = "Horas Inicio: ${parteDiario.horasInicio}"
            binding.horasFinTextView.text = "Horas Fin: ${parteDiario.horasFin}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParteDiarioViewHolder {
        val binding = ItemParteDiarioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ParteDiarioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ParteDiarioViewHolder, position: Int) {
        val parteDiario = getItem(position)
        holder.bind(parteDiario)
    }

    class ParteDiarioDiffCallback : DiffUtil.ItemCallback<ParteDiario>() {
        override fun areItemsTheSame(oldItem: ParteDiario, newItem: ParteDiario): Boolean {
            return oldItem.id_parte_diario == newItem.id_parte_diario
        }

        override fun areContentsTheSame(oldItem: ParteDiario, newItem: ParteDiario): Boolean {
            return oldItem == newItem
        }
    }


    fun addParteDiario(parteDiario: ParteDiario) {
        val currentList = currentList.toMutableList()
        val limitePartes = 30
        //
        if (currentList.size >= limitePartes) {
            currentList.removeAt(currentList.size - 1) // Eliminar el último ítem si llego al limite $limitePartes
        }

        currentList.add(0, parteDiario)
        submitList(currentList)

        // Guardar la lista actualizada en SharedPreferences
        SharedPreferencesHelper.savePartesList(context, currentList)
    }

}