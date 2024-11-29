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
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.navigation.findNavController
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.adminobr.R
import com.example.adminobr.data.ParteDiario
import com.example.adminobr.databinding.ItemParteDiarioBinding
import com.example.adminobr.ui.partediario.EditType
import com.example.adminobr.ui.partediario.ParteDiarioDetalleDialog
import com.example.adminobr.viewmodel.ParteDiarioViewModel

class ListarPartesAdapter(
    private val viewModel: ParteDiarioViewModel,
    private val context: Context,
    private val fragmentManager: FragmentManager // Agregar FragmentManager
) : PagingDataAdapter<ParteDiario, ListarPartesAdapter.ParteDiarioViewHolder>(ParteDiarioDiffCallback()) {

    inner class ParteDiarioViewHolder(val binding: ItemParteDiarioBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(parte: ParteDiario) {
            binding.apply {
                fechaTextView.text = "Fecha: ${parte.fecha}"
                equipoTextView.text = "Equipo: ${parte.equipoInterno}"
                horasInicioTextView.text = "Ini: ${parte.horasInicio}"
                horasFinTextView.text = "Fin: ${parte.horasFin}"

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
                                    val bundle = bundleOf("parteDiarioId" to parte.idParteDiario, "editType" to EditType.EDIT_ALL.name)
                                    view.findNavController().navigate(R.id.action_nav_listarpartes_to_nav_parteDiarioFormFragment_edit, bundle)
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

                Log.d("ListarPartesAdapter", "engraseGeneral: ${parte.engraseGeneral}, filtroAire: ${parte.filtroAire}, filtroAceite: ${parte.filtroAceite}, filtroComb: ${parte.filtroComb}, filtroOtro: ${parte.filtroOtro}")
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
                    setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorDanger)), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }) { _, _ ->
                    parte.idParteDiario?.let { id ->

                        viewModel.eliminarParteDiario(id, "listar")
                        //viewModel.cargarPartes()

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
    Log.d("ListarPartesAdapter", "Vinculando elemento en la posición $position")
        val parte = getItem(position)
        parte?.let { holder.bind(it) }
    }

    class ParteDiarioDiffCallback : DiffUtil.ItemCallback<ParteDiario>() {
        override fun areItemsTheSame(oldItem: ParteDiario, newItem: ParteDiario) = oldItem.idParteDiario == newItem.idParteDiario
        override fun areContentsTheSame(oldItem: ParteDiario, newItem: ParteDiario) = oldItem == newItem
    }

}


//
//package com.example.adminobr.ui.adapter
//
//import android.app.AlertDialog
//import android.content.Context
//import android.text.Spannable
//import android.text.SpannableString
//import android.text.style.ForegroundColorSpan
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.PopupMenu
//import android.widget.Toast
//
//import androidx.core.content.ContextCompat
//import androidx.paging.PagingDataAdapter
//import androidx.recyclerview.widget.DiffUtil
//import androidx.recyclerview.widget.RecyclerView
//import com.example.adminobr.R
//import com.example.adminobr.data.ListarPartesDiarios
//import com.example.adminobr.databinding.ItemParteDiarioBinding
//import com.example.adminobr.utils.SessionManager
//import com.example.adminobr.viewmodel.ParteDiarioViewModel
//
//class ListarPartesAdapter(private val viewModel: ParteDiarioViewModel, private val context: Context) : PagingDataAdapter<ListarPartesDiarios, ListarPartesAdapter.ParteDiarioViewHolder>(
//    DIFF_CALLBACK
//) {
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParteDiarioViewHolder {
//        val binding = ItemParteDiarioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
//        val sessionManager = SessionManager(parent.context)
//        val empresaDbName = sessionManager.getEmpresaData()?.db_name ?: ""
//        return ParteDiarioViewHolder(binding, viewModel, parent.context, empresaDbName)
//    }
//
//    override fun onBindViewHolder(holder: ParteDiarioViewHolder, position: Int) {
//        val parteDiario = getItem(position)
//        parteDiario?.let { holder.bind(it) }
//
//        // Control de visibilidad del menú
//        val userRoles = holder.sessionManager.getUserRol()
//        if (userRoles?.contains("supervisor") == true || userRoles?.contains("administrador") == true) {
//            holder.binding.menuItemParte.visibility = View.VISIBLE
//            // Configurar OnClickListener para el menú de tres puntos
//            holder.binding.menuItemParte.setOnClickListener { view ->
//                // Mostrar el menú contextual
//                val popupMenu = PopupMenu(holder.context, view)
//                popupMenu.inflate(R.menu.menu_item_parte)
//                popupMenu.setOnMenuItemClickListener { menuItem ->
//                    when (menuItem.itemId) {
//                        R.id.action_editar -> {
//                            // Lógica para editar el parte diario
//                            true
//                        }
//                        R.id.action_eliminar -> {
//                            val position = holder.bindingAdapterPosition
//                            val parteDiario = getItem(position)
//                            if (parteDiario != null) {
//                                // Mostrar diálogo de confirmación
//                                val builder = AlertDialog.Builder(context) // Usar context del adaptador
//                                builder.setTitle("Eliminar Parte Diario")
//                                builder.setMessage("¿Estás seguro de que quieres eliminar este parte diario?")
//
//                                // Personalizar el texto del botón positivo
//                                val positiveButtonText = SpannableString("Eliminar")
//                                val colorRojo = ContextCompat.getColor(context, R.color.danger_500)
//                                positiveButtonText.setSpan(
//                                    ForegroundColorSpan(colorRojo),
//                                    0,
//                                    positiveButtonText.length,
//                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
//                                )
//
//                                builder.setPositiveButton(positiveButtonText) { dialog, _ ->
//                                    viewModel.deleteParteDiario(parteDiario, holder.empresaDbName) { success, parteEliminado ->
//                                        if (success) {
//                                            refresh() // Recargar los datos del PagingDataAdapter
//                                            Toast.makeText(holder.context, "Parte diario eliminado correctamente", Toast.LENGTH_SHORT).show()
//                                        } else {
//                                            Toast.makeText(holder.context, "Error al eliminar el parte diario", Toast.LENGTH_SHORT).show()
//                                        }
//                                    }
//                                    dialog.dismiss()
//                                }
//
////                                builder.setPositiveButton(positiveButtonText) { dialog, _ ->
////                                    viewModel.deleteParteDiario(parteDiario, holder.empresaDbName) { success, parteEliminado ->
////                                        if (success) {
////                                            snapshot().invalidate() // Invalidar el snapshot
////                                            Toast.makeText(holder.context, "Parte diario eliminado correctamente", Toast.LENGTH_SHORT).show()
////                                        } else {
////                                            Toast.makeText(holder.context, "Error al eliminar el parte diario", Toast.LENGTH_SHORT).show()
////                                        }
////                                    }
////                                    dialog.dismiss()
////                                }
//                                builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
//                                builder.create().show()
//                            } else {
//                                Log.e("ParteDiarioViewHolder", "Error: parteDiario es nulo")
//                            }
//                            true
//                        }
//                        else -> false
//                    }
//                }
//                popupMenu.show()
//            }
//        } else {
//            holder.binding.menuItemParte.visibility = View.GONE
//        }
//    }
//
//    class ParteDiarioViewHolder(
//        val binding: ItemParteDiarioBinding,
//        val viewModel: ParteDiarioViewModel,
//        val context: Context,
//        val empresaDbName: String
//    ) : RecyclerView.ViewHolder(binding.root) {
//
//        val sessionManager = SessionManager(context)
//
//        fun bind(parteDiario: ListarPartesDiarios) {
//            binding.fechaTextView.text = "Fecha: ${parteDiario.fecha}"
//            binding.equipoTextView.text = "Equipo: ${parteDiario.interno}"
//            binding.horasInicioTextView.text = "Ini: ${parteDiario.horas_inicio}"
//            binding.horasFinTextView.text = "Fin: ${parteDiario.horas_fin}"
//        }
//    }
//
//    companion object {
//        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ListarPartesDiarios>() {
//            override fun areItemsTheSame(oldItem: ListarPartesDiarios, newItem: ListarPartesDiarios): Boolean {
//                return oldItem.id_parte_diario == newItem.id_parte_diario
//            }
//
//            override fun areContentsTheSame(oldItem: ListarPartesDiarios, newItem: ListarPartesDiarios): Boolean {
//                return oldItem == newItem
//            }
//        }
//    }
//}