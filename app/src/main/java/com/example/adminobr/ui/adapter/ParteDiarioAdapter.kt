package com.example.adminobr.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.compose.ui.semantics.text
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.adminobr.R
import com.example.adminobr.data.ListarPartesDiarios
import com.example.adminobr.data.ParteDiario
import com.example.adminobr.databinding.ItemParteDiarioBinding
import com.example.adminobr.utils.SessionManager
import com.example.adminobr.utils.SharedPreferencesHelper
import com.example.adminobr.viewmodel.ParteDiarioViewModel

class ParteDiarioAdapter(
    private val context: Context,
    private val sessionManager: SessionManager, // Agrega este parámetro
    private val viewModel: ParteDiarioViewModel, // Agrega este parámetro
    private val empresaDbName: String // Agrega este parámetro
) : ListAdapter<ListarPartesDiarios, ParteDiarioAdapter.ParteDiarioViewHolder>(ParteDiarioDiffCallback()) {

    class ParteDiarioViewHolder(val binding: ItemParteDiarioBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(parteDiario: ListarPartesDiarios) { // Usar ListarPartesDiarios
            binding.fechaTextView.text = "Fecha: ${parteDiario.fecha}"
            binding.equipoTextView.text = "Equipo: ${parteDiario.interno}"
            binding.horasInicioTextView.text = "Horas Inicio: ${parteDiario.horas_inicio}"
            binding.horasFinTextView.text = "Horas Fin: ${parteDiario.horas_fin}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParteDiarioViewHolder {
        val binding = ItemParteDiarioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ParteDiarioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ParteDiarioViewHolder, position: Int) {
        val parteDiario = getItem(position)
        holder.bind(parteDiario)

        // Configurar OnClickListener para el menú de tres puntos
        holder.binding.menuItemParte.setOnClickListener { view ->
            // Obtener roles del usuario
            val userRoles = sessionManager.getUserRol()

            // Constrol de visivilidad segun roles
            if (userRoles?.contains("supervisor") == true || userRoles?.contains("administrador") == true) {
                // Mostrar el menú contextual
                val popupMenu = PopupMenu(context, view)
                popupMenu.inflate(R.menu.menu_item_parte)
                popupMenu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_editar -> {
                            // Lógica para editar el parte diario
                            true
                        }
                        R.id.action_eliminar -> {
                            // Lógica para eliminar el parte diario
                            val parteDiarioId = parteDiario.id_parte_diario
                            viewModel.deleteParteDiario(parteDiarioId, empresaDbName) { success ->
                                if (success) {
                                    // Mostrar un mensaje de éxito (Toast o Snackbar)
                                    Toast.makeText(context, "Parte diario eliminado correctamente", Toast.LENGTH_SHORT).show()
                                    // Actualizar la lista (puedes usar un callback o actualizar la lista en el ViewModel)
                                    // ...
                                } else {
                                    // Mostrar un mensaje de error
                                    Toast.makeText(context, "Error al eliminar el parte diario", Toast.LENGTH_SHORT).show()
                                }
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

    class ParteDiarioDiffCallback : DiffUtil.ItemCallback<ListarPartesDiarios>() {
        override fun areItemsTheSame(oldItem: ListarPartesDiarios, newItem: ListarPartesDiarios): Boolean {
            return oldItem.id_parte_diario == newItem.id_parte_diario
        }

        override fun areContentsTheSame(oldItem: ListarPartesDiarios, newItem: ListarPartesDiarios): Boolean {
            return oldItem == newItem
        }
    }

    override fun submitList(list: List<ListarPartesDiarios>?) { // Usar ListarPartesDiarios y eliminar take(5)
        super.submitList(list)
    }

}