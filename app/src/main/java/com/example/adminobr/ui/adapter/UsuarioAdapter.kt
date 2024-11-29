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
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.adminobr.R
import com.example.adminobr.data.Usuario
import com.example.adminobr.databinding.ItemUsuarioBinding
import com.example.adminobr.ui.usuarios.EditType
import com.example.adminobr.utils.SessionManager
import com.example.adminobr.viewmodel.UsuarioViewModel

class UsuarioAdapter(private val viewModel: UsuarioViewModel, private val context: Context) : ListAdapter<Usuario, UsuarioAdapter.UserViewHolder>(
    UserDiffCallback()
    ) {
    class UserViewHolder(
        val binding: ItemUsuarioBinding,
        val viewModel: UsuarioViewModel,
        val context: Context,
        val empresaDbName: String,
    ) : RecyclerView.ViewHolder(binding.root) {

        val sessionManager = SessionManager(context)

        fun bind(user: Usuario) {
            binding.userNameTextView.text = "${user.nombre} ${user.apellido}".uppercase()
            binding.userLegajoTextView.text = "Legajo: ${user.legajo}"
            binding.userRolTextView.text = if (user.principalRole.isNullOrEmpty()) { "Sin Rol" } else { "Rol: ${user.principalRole.replaceFirstChar { it.uppercaseChar() }}" }

            binding.root.setOnClickListener {
                user.id
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUsuarioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val sessionManager = SessionManager(parent.context)
        val empresaDbName = sessionManager.getEmpresaData()?.db_name ?: ""
        return UserViewHolder(binding, viewModel, parent.context, empresaDbName)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val usuario = getItem(position)
        usuario?.let { holder.bind(it) }

        // Control de visibilidad del menú
        val userRoles = holder.sessionManager.getUserRol()
        if (userRoles?.contains("supervisor") == true || userRoles?.contains("administrador") == true) {
            holder.binding.menuItemUsuario.visibility = View.VISIBLE
            // Configurar OnClickListener para el menú de tres puntos
            holder.binding.menuItemUsuario.setOnClickListener { view ->
                // Mostrar el menú contextual
                val popupMenu = PopupMenu(holder.context, view)
                popupMenu.inflate(R.menu.menu_item_usuario)
                popupMenu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_editar -> {
                            val position = holder.bindingAdapterPosition
                            val usuario = getItem(position)
                            if (usuario != null) {
                                val userId = usuario.id
                                val bundle = bundleOf("userId" to userId, "editType" to EditType.EDIT_ALL.name)
                                view.findNavController().navigate(R.id.action_nav_gestion_usuarios_to_nav_userFormFragment_edit, bundle)
                            }
                            true
                        }
                        R.id.action_eliminar -> {
                            val position = holder.bindingAdapterPosition
                            val usuario = getItem(position)
                            if (usuario != null) {
                                // Mostrar diálogo de confirmación
                                AlertDialog.Builder(context) // Usar context del adaptador
                                    .setTitle("Eliminar Usuario")
                                    .setMessage("¿Estás seguro de que quieres eliminar este usuario?")
                                    // Botón positivo ("Eliminar") con color rojo (danger_500)
                                    .setPositiveButton(SpannableString("Eliminar").apply {
                                        setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.danger_500)), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                    }) { _, _ ->
                                        usuario.id?.let { userId ->
                                            viewModel.eliminarUsuario(userId) // Llama al método de eliminación en el ViewModel
//                                            Toast.makeText(context, "Eliminando usuario...", Toast.LENGTH_SHORT).show()
                                        } ?: run {
                                            Toast.makeText(context, "Error: ID de usuario no válido.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    // Botón negativo ("Cancelar") con color negro (colorBlack)
                                    .setNegativeButton(SpannableString("Cancelar").apply {
                                        setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorBlack)), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                    }, null)
                                    .show()
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

    class UserDiffCallback : DiffUtil.ItemCallback<Usuario>() {
        override fun areItemsTheSame(oldItem: Usuario, newItem: Usuario): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Usuario, newItem: Usuario): Boolean {
            return oldItem == newItem
        }
    }

    override fun submitList(list: List<Usuario>?) {
        super.submitList(list)
    }

}