package com.example.adminobr.ui.adapter

import android.app.AlertDialog
import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.adminobr.R
import com.example.adminobr.data.Usuario
import com.example.adminobr.databinding.ItemUsuarioBinding
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

    @OptIn(UnstableApi::class)
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val usuario = getItem(position)
        usuario?.let { holder.bind(it) }
        Log.d("UserAdapter", "Binding user: $usuario")

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
                                val bundle = bundleOf("userId" to userId, "isEditMode" to true) // Pasar isEditMode como true
                                view.findNavController().navigate(R.id.action_nav_gestion_usuarios_to_nav_userFormFragment_edit, bundle)
                            }
                            true
                        }
                        R.id.action_eliminar -> {
                            val position = holder.bindingAdapterPosition
                            val usuario = getItem(position)
                            if (usuario != null) {
                                // Mostrar diálogo de confirmación
                                val builder = AlertDialog.Builder(context) // Usar context del adaptador
                                builder.setTitle("Eliminar Usuario")
                                builder.setMessage("¿Estás seguro de que quieres eliminar este usuario?")

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
                                    usuario.id?.let { userId ->
                                        viewModel.eliminarUsuario(userId) // Llama al método de eliminación en el ViewModel
                                        Toast.makeText(context, "Eliminando usuario...", Toast.LENGTH_SHORT).show()
                                    } ?: run {
                                        Toast.makeText(context, "Error: ID de usuario no válido.", Toast.LENGTH_SHORT).show()
                                    }
                                    dialog.dismiss()
                                }

                                builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
                                builder.create().show()
                            } else {
                                android.util.Log.e("UsuarioViewHolder", "Error: usuario es nulo")
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