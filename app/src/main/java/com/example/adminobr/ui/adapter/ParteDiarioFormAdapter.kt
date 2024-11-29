package com.example.adminobr.ui.adapter

import android.app.AlertDialog
import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.adminobr.R
import com.example.adminobr.data.ParteDiario
import com.example.adminobr.databinding.ItemParteDiarioBinding
import com.example.adminobr.ui.partediario.ParteDiarioDetalleDialog
import com.example.adminobr.viewmodel.ParteDiarioViewModel

class ParteDiarioFormAdapter(
    private val viewModel: ParteDiarioViewModel,
    private val context: Context,
    private val userId: Int,
    private val fragmentManager: FragmentManager, // Agregar FragmentManager
    private val onEditParte: (ParteDiario) -> Unit, // Callback para editar
) : ListAdapter<ParteDiario, ParteDiarioFormAdapter.ParteDiarioViewHolder>(ParteDiarioDiffCallback()) {

    inner class ParteDiarioViewHolder(val binding: ItemParteDiarioBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(parte: ParteDiario) {
            binding.apply {
                fechaTextView.text = "Fecha: ${parte.fecha}"
                equipoTextView.text = "Equipo: ${parte.equipoInterno}"
                horasInicioTextView.text = "Ini: ${parte.horasInicio}"
                horasFinTextView.text = "Fin: ${parte.horasFin}"

//                menuItemParte.setOnClickListener { view ->
//                    val popupMenu = PopupMenu(context, view)
//                    popupMenu.inflate(R.menu.menu_item_parte)
//                    popupMenu.setOnMenuItemClickListener { menuItem ->
//                        when (menuItem.itemId) {
//                            R.id.action_detalles -> {
//                                // Cierra el PopupMenu antes de mostrar el diálogo de detalles
//                                popupMenu.dismiss()
//                                showParteDetalleDialog(parte) // Mostrar diálogo de detalles
//                                true
//                            }
//                            R.id.action_editar -> {
//                                // Cierra el PopupMenu antes de navegar al formulario de edición
//                                popupMenu.dismiss()
//                                onEditParte(parte) // Llama al callback de edición
//                                true
//                            }
//                            R.id.action_eliminar -> {
//                                // Cierra el PopupMenu antes de mostrar la confirmación de eliminación
//                                popupMenu.dismiss()
//                                confirmarEliminarParte(parte)
//                                true
//                            }
//                            else -> false
//                        }
//                    }
//                    popupMenu.show()
//                }

                menuItemParte.setOnClickListener { view ->
                    val popupMenu = PopupMenu(context, view)
                    popupMenu.inflate(R.menu.menu_item_parte)

                    popupMenu.setOnMenuItemClickListener { menuItem ->
                        popupMenu.dismiss() // Asegurarse de que el menú esté cerrado
                        when (menuItem.itemId) {
                            R.id.action_detalles -> {
                                view.post {
                                    showParteDetalleDialog(parte)
                                }
                                true
                            }
                            R.id.action_editar -> {
                                view.post {
                                    onEditParte(parte)
                                }
                                true
                            }
                            R.id.action_eliminar -> {
                                view.post {
                                    confirmarEliminarParte(parte)
                                }
                                true
                            }
                            else -> false
                        }
                    }
                    popupMenu.show()
                }

                // Condición para mostrar el badge de mantenimiento
                val hasMantenimiento = (parte.engraseGeneral == 1 || parte.filtroAire == 1 ||
                        parte.filtroAceite == 1 || parte.filtroComb == 1 ||
                        parte.filtroOtro == 1)

                Log.d("ParteDiarioAdapter", "engraseGeneral: ${parte.engraseGeneral}, filtroAire: ${parte.filtroAire}, filtroAceite: ${parte.filtroAceite}, filtroComb: ${parte.filtroComb}, filtroOtro: ${parte.filtroOtro}")
                mantenimientoBadge.visibility = if (hasMantenimiento) View.VISIBLE else View.GONE
            }
        }

        private fun showParteDetalleDialog(parte: ParteDiario) {
            val parte = ParteDiarioDetalleDialog(parte)
            parte.show(fragmentManager, "ParteDiarioDetalleDialog")
        }

        private fun confirmarEliminarParte(parte: ParteDiario) {
            AlertDialog.Builder(context)
                .setTitle("Eliminar Parte")
                .setMessage("¿Estás seguro de que quieres eliminar este parte?")
                .setPositiveButton(SpannableString("Eliminar").apply {
                    setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.danger_500)), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }) { _, _ ->
                    parte.idParteDiario?.let { id ->

                        viewModel.eliminarParteDiario(id, "form")
                        viewModel.cargarUltimosPartesPorUsuario(userId) // Recargar lista

//                        Toast.makeText(context, "Eliminando Parte...", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(SpannableString("Cancelar").apply {
                    setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorBlack)), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }, null)
                .show()
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParteDiarioViewHolder {
        val binding = ItemParteDiarioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ParteDiarioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ParteDiarioViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ParteDiarioDiffCallback : DiffUtil.ItemCallback<ParteDiario>() {
        override fun areItemsTheSame(oldItem: ParteDiario, newItem: ParteDiario) = oldItem.idParteDiario == newItem.idParteDiario
        override fun areContentsTheSame(oldItem: ParteDiario, newItem: ParteDiario) = oldItem == newItem
    }
}
