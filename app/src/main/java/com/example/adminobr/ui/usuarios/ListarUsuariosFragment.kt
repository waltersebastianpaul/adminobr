package com.example.adminobr.ui.usuarios

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminobr.R
import com.example.adminobr.databinding.FragmentListarUsuariosBinding
import com.example.adminobr.ui.adapter.UsuarioAdapter
import com.example.adminobr.viewmodel.UsuarioViewModel
import com.example.adminobr.viewmodel.UsuarioViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ListarUsuariosFragment : Fragment(R.layout.fragment_listar_usuarios) {

    private var _binding: FragmentListarUsuariosBinding? = null
    private val binding get() = _binding!!
    private lateinit var userAdapter: UsuarioAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListarUsuariosBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val usuarioViewModel: UsuarioViewModel by viewModels {
        UsuarioViewModelFactory(requireActivity().application)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userAdapter = UsuarioAdapter { userId ->
            navigateToEditUserForm(userId)
        }

        binding.userListRecyclerView.adapter = userAdapter
        binding.userListRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        usuarioViewModel.error.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { errorMessage ->
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            }
        }

        usuarioViewModel.users.observe(viewLifecycleOwner) { users ->
            userAdapter.submitList(users)
        }

        usuarioViewModel.loadUsers() // Llamar a loadUsers() aquí

        // Configurar el FloatingActionButton
        setupFab()
    }

    private fun setupFab() {
        // Obtener referencia al FAB y configurar su OnClickListener
        val fab: FloatingActionButton? = activity?.findViewById(R.id.fab)

        fab?.visibility = View.VISIBLE
        fab?.setImageResource(R.drawable.ic_add)
        fab?.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
        fab?.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white)))

        fab?.setOnClickListener {
            navigateToCreateUserForm()
            //limpiarFormulario()
            fab.visibility = View.GONE
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun navigateToCreateUserForm() {
        findNavController().navigate(
            R.id.action_nav_gestion_usuarios_to_nav_userFormFragment_create,
            bundleOf("isEditMode" to false)
        )
    }

    private fun navigateToEditUserForm(userId: Int) {
        findNavController().navigate(
            R.id.action_nav_gestion_usuarios_to_nav_userFormFragment_edit,
            bundleOf("isEditMode" to true, "userId" to userId)
        )
    }
}