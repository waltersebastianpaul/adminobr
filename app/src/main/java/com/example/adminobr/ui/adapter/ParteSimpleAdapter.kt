package com.example.adminobr.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.adminobr.data.ParteSimple
import com.example.adminobr.databinding.ItemParteSimpleBinding
import com.example.adminobr.viewmodel.ParteSimpleViewModel

class ParteSimpleAdapter(private var viewModel: ParteSimpleViewModel) : ListAdapter<ParteSimple, ParteSimpleAdapter.ViewHolder>(
    ParteSimpleDiffCallback()
) {

    class ViewHolder(val binding: ItemParteSimpleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(parte: ParteSimple, position: Int, viewModel: ParteSimpleViewModel, adapter: ParteSimpleAdapter) { // Agrega adapter como parámetro
            binding.fechaTextView.text = "Fecha: ${parte.fecha}"
            binding.equipoTextView.text = "Equipo: ${parte.equipo}"
            binding.horasTextView.text = "Horas: ${parte.horas}"
            binding.observacionesTextView.text = "Observaciones: ${parte.observaciones}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemParteSimpleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val parte = getItem(position)
        holder.bind(parte, position, viewModel, this) // Pasa el adaptador al ViewHolder
    }

    class ParteSimpleDiffCallback : DiffUtil.ItemCallback<ParteSimple>() {
        override fun areItemsTheSame(oldItem: ParteSimple, newItem: ParteSimple): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: ParteSimple, newItem: ParteSimple): Boolean {
            return oldItem == newItem
        }
    }

    fun setViewModel(viewModel: ParteSimpleViewModel) {
        this.viewModel = viewModel
    }
}