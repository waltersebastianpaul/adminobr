package com.example.adminobr.ui.adapter

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.adminobr.data.ListarPartesDiarios
import com.example.adminobr.databinding.ItemParteDiarioBinding
import com.example.adminobr.utils.SessionManager
import com.example.adminobr.viewmodel.ParteDiarioViewModel

class ListarPartesAdapter(private val viewModel: ParteDiarioViewModel) : PagingDataAdapter<ListarPartesDiarios, ListarPartesAdapter.ParteDiarioViewHolder>(
    DIFF_CALLBACK
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParteDiarioViewHolder {
        val binding = ItemParteDiarioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ParteDiarioViewHolder(binding, viewModel, parent.context)
    }

    override fun onBindViewHolder(holder: ParteDiarioViewHolder, position: Int) {
        val parteDiario = getItem(position)
        parteDiario?.let { holder.bind(it) }
    }

    class ParteDiarioViewHolder(
        private val binding: ItemParteDiarioBinding,
        private val viewModel: ParteDiarioViewModel,
        private val context: Context

    ) : RecyclerView.ViewHolder(binding.root) {

        private val sessionManager = SessionManager(context)
        private val empresaDbName = sessionManager.getEmpresaData()?.db_name

        fun bind(parteDiario: ListarPartesDiarios) {
            binding.fechaTextView.text = "Fecha: ${parteDiario.fecha}"
            binding.equipoTextView.text = "Equipo: ${parteDiario.interno}"
            binding.horasInicioTextView.text = "Horas: ${parteDiario.horas_inicio}"
            binding.horasFinTextView.text = "Horas: ${parteDiario.horas_fin}"

            binding.buttonDelete.setOnClickListener {
                onItemDeleted(parteDiario)
            }

        }

//        init {
//            binding.root.setOnClickListener {
//                val parteDiario = getItem(adapterPosition)
//                val empresaDbName = sessionManager.getEmpresaData()?.db_name
//
//                val action = ListarPartesFragmentDirections.actionListarPartesFragmentToEditParteDiarioFragment(parteDiario)
//                findNavController().navigate(action)
//            }
//        }

        private fun onItemDeleted(parteDiario: ListarPartesDiarios) {
            val builder = AlertDialog.Builder(context)
                .setTitle("Confirmar eliminación")
                .setMessage("¿Estás seguro de que deseas eliminar este parte diario?")
                .setPositiveButton("Sí") { _, _ ->
                    Log.d("ParteDiarioViewHolder", "Eliminando parte diario con ID: ${parteDiario.id_parte_diario}")
                    if (empresaDbName != null) {
                        viewModel.deleteParteDiario(parteDiario.id_parte_diario, empresaDbName) { success ->
                            if (success) {
                                Log.d("ParteDiarioViewHolder", "Parte diario eliminado correctamente")
                                // Actualizar la lista
                            } else {
                                Log.e("ParteDiarioViewHolder", "Error al eliminar parte diario")
                                // Mostrar mensaje de error al usuario
                            }
                        }
                    } else {
                        // Mostrar un mensaje de error al usuario indicando que no se pudo obtener empresaDbName
                        Log.e("ParteDiarioViewHolder", "Error: empresaDbName es nulo")
                    }
                }
                .setNegativeButton("No", null)
            val dialog = builder.create()
            dialog.show()
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