package com.example.adminobr.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.adminobr.data.ListarPartesDiarios
import com.example.adminobr.databinding.ItemParteDiarioBinding

class ListarPartesAdapter : PagingDataAdapter<ListarPartesDiarios, ListarPartesAdapter.ParteDiarioViewHolder>(
    DIFF_CALLBACK
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParteDiarioViewHolder {
        val binding = ItemParteDiarioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ParteDiarioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ParteDiarioViewHolder, position: Int) {
        val parteDiario = getItem(position)
        parteDiario?.let { holder.bind(it) }
    }

    class ParteDiarioViewHolder(private val binding: ItemParteDiarioBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(parteDiario: ListarPartesDiarios) {
            binding.fechaTextView.text = "Fecha: ${parteDiario.fecha}"
            binding.equipoTextView.text = "Equipo: ${parteDiario.interno}"
            binding.horasInicioTextView.text = "Horas: ${parteDiario.horas_inicio}"
            binding.horasFinTextView.text = "Horas: ${parteDiario.horas_fin}"
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ListarPartesDiarios>() {
            override fun areItemsTheSame(oldItem: ListarPartesDiarios, newItem: ListarPartesDiarios): Boolean {
                return oldItem.id_parte_diario == newItem.id_parte_diario
            }

            override fun areContentsTheSame(oldItem: ListarPartesDiarios, newItem: ListarPartesDiarios): Boolean {
                return oldItem == newItem
            }
        }
    }
}