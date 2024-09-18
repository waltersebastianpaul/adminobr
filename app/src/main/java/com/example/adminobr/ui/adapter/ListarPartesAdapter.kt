package com.example.adminobr.ui.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.compose.ui.semantics.text
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.adminobr.R
import com.example.adminobr.data.ListarPartesDiarios
import com.example.adminobr.databinding.ItemParteDiarioBinding
import com.example.adminobr.utils.SessionManager
import com.example.adminobr.viewmodel.ParteDiarioViewModel

class ListarPartesAdapter(private val viewModel: ParteDiarioViewModel, private val context: Context) : PagingDataAdapter<ListarPartesDiarios, ListarPartesAdapter.ParteDiarioViewHolder>(
    DIFF_CALLBACK
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParteDiarioViewHolder {
        val binding = ItemParteDiarioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val sessionManager = SessionManager(parent.context)
        val empresaDbName = sessionManager.getEmpresaData()?.db_name ?: ""
        return ParteDiarioViewHolder(binding, viewModel, parent.context, empresaDbName)
    }

    override fun onBindViewHolder(holder: ParteDiarioViewHolder, position: Int) {
        val parteDiario = getItem(position)
        parteDiario?.let { holder.bind(it) }

        // Configurar OnClickListener para el menú de tres puntos
        holder.binding.menuItemParte.setOnClickListener { view ->
            // Obtener roles del usuario
            val userRoles = holder.sessionManager.getUserRol() // Acceder a sessionManager a través de holder

            // Control de visibilidad según roles
            if (userRoles?.contains("supervisor") == true || userRoles?.contains("administrador") == true) {
                // Mostrar el menú contextual
                val popupMenu = PopupMenu(holder.context, view)
                popupMenu.inflate(R.menu.menu_item_parte)
                popupMenu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_editar -> {
                            // Lógica para editar el parte diario
                            true
                        }
                        R.id.action_eliminar -> {
                            // Lógica para eliminar el parte diario
                            val parteDiarioId = parteDiario?.id_parte_diario
                            if (parteDiarioId != null) {
                                viewModel.deleteParteDiario(parteDiarioId, holder.empresaDbName) { success ->
                                    if (success) {
                                        Toast.makeText(holder.context, "Parte diario eliminado correctamente", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(holder.context, "Error al eliminar el parte diario", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Log.e("ParteDiarioViewHolder", "Error: parteDiarioId es nulo")
                            }
                            true
                        }
                        else -> false
                    }
                }
                popupMenu.show()
            }
        }
    }

    class ParteDiarioViewHolder(
        val binding: ItemParteDiarioBinding,
        private val viewModel: ParteDiarioViewModel,
        val context: Context,
        val empresaDbName: String
    ) : RecyclerView.ViewHolder(binding.root) {

        val sessionManager = SessionManager(context)

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