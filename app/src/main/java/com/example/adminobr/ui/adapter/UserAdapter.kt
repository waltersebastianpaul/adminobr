package com.example.adminobr.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.ui.semantics.text
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.adminobr.data.Usuario
import com.example.adminobr.databinding.UserListItemBinding

class UserAdapter(private val onUserClick: (Int) -> Unit) :
    ListAdapter<Usuario, UserAdapter.UserViewHolder>(UserDiffCallback()) {

    class UserViewHolder(private val binding: UserListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: Usuario, onUserClick: (Int) -> Unit) { // Agregar onUserClick como parámetro
            binding.userNameTextView.text = user.nombre
            binding.userEmailTextView.text = user.email

            binding.root.setOnClickListener {
                user.id?.let { it1 -> onUserClick(it1) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = UserListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    @OptIn(UnstableApi::class)
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val usuario = getItem(position)
        Log.d("UserAdapter", "Binding user: $usuario")
        holder.bind(usuario, onUserClick) // Pasar onUserClick al método bind
    }

    class UserDiffCallback : DiffUtil.ItemCallback<Usuario>() {
        override fun areItemsTheSame(oldItem: Usuario, newItem: Usuario): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Usuario, newItem: Usuario): Boolean {
            return oldItem == newItem
        }
    }
}