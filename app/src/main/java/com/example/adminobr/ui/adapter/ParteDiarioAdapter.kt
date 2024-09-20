package com.example.adminobr.ui.adapter

import android.app.AlertDialog
import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu

import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.text
import androidx.core.content.ContextCompat

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
    private val sessionManager: SessionManager,
    private val viewModel: ParteDiarioViewModel,
    private val empresaDbName: String
) : ListAdapter<ListarPartesDiarios, ParteDiarioAdapter.ParteDiarioViewHolder>(ParteDiarioDiffCallback()) {

    class ParteDiarioViewHolder(val binding: ItemParteDiarioBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(parteDiario: ListarPartesDiarios) {
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

        // Control de visibilidad del menú
        val userRoles = sessionManager.getUserRol()
        if (userRoles?.contains("supervisor") == true || userRoles?.contains("administrador") == true) {
            holder.binding.menuItemParte.visibility = View.VISIBLE
            // Configurar OnClickListener para el menú de tres puntos
            holder.binding.menuItemParte.setOnClickListener { view ->
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
                            val position = holder.bindingAdapterPosition
                            val parteDiario = getItem(position)
                            if (parteDiario != null) {
                                // Mostrar diálogo de confirmación
                                val builder = AlertDialog.Builder(context)
                                builder.setTitle("Eliminar Parte Diario")
                                builder.setMessage("¿Estás seguro de que quieres eliminar este parte diario?")

                                // Personalizar el texto del botón positivo
                                val positiveButtonText = SpannableString("Eliminar")
                                val colorRojo = ContextCompat.getColor(context, R.color.colorAlert)
                                positiveButtonText.setSpan(
                                    ForegroundColorSpan(colorRojo),
                                    0,
                                    positiveButtonText.length,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                )

                                builder.setPositiveButton(positiveButtonText) { dialog, _ ->
                                    viewModel.deleteParteDiario(parteDiario, empresaDbName) { success, parteEliminado ->
                                        if (success) {
                                            parteEliminado?.let {
                                                val newList = currentList.toMutableList().also { it.remove(parteEliminado) }
                                                submitList(newList)
                                                notifyItemRemoved(position)
                                            }
                                        } else {
                                            // Manejar el error de eliminación si es necesario
                                        }
                                    }
                                    dialog.dismiss()
                                }
                                builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
                                builder.create().show()
                            }
                            true
                        }
                        else -> false
                    }
                }
                popupMenu.show()
            }
        } else {
            holder.binding.menuItemParte.visibility = View.GONE
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

    override fun submitList(list: List<ListarPartesDiarios>?) {
        super.submitList(list)
    }
}