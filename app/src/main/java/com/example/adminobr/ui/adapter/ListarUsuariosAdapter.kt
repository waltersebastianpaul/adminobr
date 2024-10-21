package com.example.adminobr.ui.adapter

import android.app.AlertDialog
import android.content.Context
import android.text.InputFilter
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
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.adminobr.R
import com.example.adminobr.data.Usuario
import com.example.adminobr.databinding.ItemUsuarioBinding
import com.example.adminobr.utils.SessionManager
import com.example.adminobr.viewmodel.UsuarioViewModel

class ListarUsuariosAdapter(private val viewModel: UsuarioViewModel, private val context: Context) : PagingDataAdapter<Usuario, ListarUsuariosAdapter.UsuarioViewHolder>(
    DIFF_CALLBACK
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val binding = ItemUsuarioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val sessionManager = SessionManager(parent.context)
        val empresaDbName = sessionManager.getEmpresaData()?.db_name ?: ""
        return UsuarioViewHolder(binding, viewModel, parent.context, empresaDbName)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        val parteDiario = getItem(position)
        parteDiario?.let { holder.bind(it) }

        // Control de visibilidad del menú
        val userRoles = holder.sessionManager.getUserRol()
        if (userRoles?.contains("supervisor") == true || userRoles?.contains("administrador") == true) {
            holder.binding.menuItemUsuario.visibility = View.VISIBLE
            // Configurar OnClickListener para el menú de tres puntos
            holder.binding.menuItemUsuario.setOnClickListener { view ->
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
                            val position = holder.bindingAdapterPosition
                            val usuario = getItem(position)
                            if (usuario != null) {
                                // Mostrar diálogo de confirmación
                                val builder = AlertDialog.Builder(context) // Usar context del adaptador
                                builder.setTitle("Eliminar Parte Diario")
                                builder.setMessage("¿Estás seguro de que quieres eliminar este parte diario?")

                                // Personalizar el texto del botón positivo
                                val positiveButtonText = SpannableString("Eliminar")
                                val colorRojo = ContextCompat.getColor(context, R.color.danger_500)
                                positiveButtonText.setSpan(
                                    ForegroundColorSpan(colorRojo),
                                    0,
                                    positiveButtonText.length,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                )

                                builder.setPositiveButton(positiveButtonText) { dialog, _ ->
                                    viewModel.deleteUsuario(usuario, holder.empresaDbName) { success, parteEliminado ->
                                        if (success) {
                                            refresh() // Recargar los datos del PagingDataAdapter
                                            Toast.makeText(holder.context, "Parte diario eliminado correctamente", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(holder.context, "Error al eliminar el parte diario", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    dialog.dismiss()
                                }

//                                builder.setPositiveButton(positiveButtonText) { dialog, _ ->
//                                    viewModel.deleteParteDiario(usuario, holder.empresaDbName) { success, parteEliminado ->
//                                        if (success) {
//                                            snapshot().invalidate() // Invalidar el snapshot
//                                            Toast.makeText(holder.context, "Parte diario eliminado correctamente", Toast.LENGTH_SHORT).show()
//                                        } else {
//                                            Toast.makeText(holder.context, "Error al eliminar el parte diario", Toast.LENGTH_SHORT).show()
//                                        }
//                                    }
//                                    dialog.dismiss()
//                                }
                                builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
                                builder.create().show()
                            } else {
                                Log.e("UsuarioViewHolder", "Error: usuario es nulo")
                            }
                            true
                        }
                        else -> false
                    }
                }
                popupMenu.show()
            }
        } else {
            holder.binding.menuItemUsuario.visibility = View.GONE
        }
    }

    class UsuarioViewHolder(
        val binding: ItemUsuarioBinding,
        private val viewModel: UsuarioViewModel,
        val context: Context,
        val empresaDbName: String
    ) : RecyclerView.ViewHolder(binding.root) {

        val sessionManager = SessionManager(context)

        fun bind(usuario: Usuario) {
            binding.userNameTextView.text = "UserName: ${usuario.nombre} ${usuario.apellido}".uppercase()
            binding.userLegajoTextView.text = "UserEmail: ${usuario.legajo}"
            binding.userRolTextView.text = "UserRol: ${usuario.principalRole}".uppercase()
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Usuario>() {
            override fun areItemsTheSame(oldItem: Usuario, newItem: Usuario): Boolean {
                return oldItem.dni == newItem.dni
            }

            override fun areContentsTheSame(oldItem: Usuario, newItem: Usuario): Boolean {
                return oldItem == newItem
            }
        }
    }
}