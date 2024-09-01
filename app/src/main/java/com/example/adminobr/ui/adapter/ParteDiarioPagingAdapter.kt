package com.example.adminobr.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.adminobr.data.ListarPartesDiarios
import com.example.adminobr.databinding.ItemParteDiarioBinding

class ParteDiarioPagingAdapter :
    PagingDataAdapter<ListarPartesDiarios, ParteDiarioPagingAdapter.ParteDiarioViewHolder>(
        DiffCallback
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParteDiarioViewHolder {
        val binding = ItemParteDiarioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ParteDiarioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ParteDiarioViewHolder, position: Int) {
        val parteDiario = getItem(position)
        holder.bind(parteDiario)
    }

    class ParteDiarioViewHolder(private val binding: ItemParteDiarioBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(parteDiario: ListarPartesDiarios?) {
            parteDiario?.let {
                binding.fechaTextView.text = it.fecha
                binding.equipoTextView.text = it.interno
                binding.horasInicioTextView.text = it.horas_inicio.toString()
                binding.horasFinTextView.text = it.horas_fin.toString()
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ListarPartesDiarios>() {
        override fun areItemsTheSame(oldItem: ListarPartesDiarios, newItem: ListarPartesDiarios): Boolean {
            return oldItem.id_parte_diario == newItem.id_parte_diario
        }

        override fun areContentsTheSame(oldItem: ListarPartesDiarios, newItem: ListarPartesDiarios): Boolean {
            return oldItem == newItem
        }
    }
}
