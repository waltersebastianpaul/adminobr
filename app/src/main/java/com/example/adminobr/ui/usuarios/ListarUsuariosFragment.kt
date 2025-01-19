package com.example.adminobr.ui.usuarios

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminobr.R
import com.example.adminobr.databinding.FragmentListarUsuariosBinding
import com.example.adminobr.ui.adapter.UsuarioAdapter
import com.example.adminobr.utils.NetworkStatusHelper
import com.example.adminobr.viewmodel.UsuarioViewModel
import com.example.adminobr.viewmodel.UsuarioViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ListarUsuariosFragment : Fragment(R.layout.fragment_listar_usuarios) {

    private var _binding: FragmentListarUsuariosBinding? = null
    private val binding get() = _binding!!
    private lateinit var userAdapter: UsuarioAdapter

    private var previousConnectionState: Boolean? = null

    private val usuarioViewModel: UsuarioViewModel by viewModels {
        UsuarioViewModelFactory(requireActivity().application)
    }
    private var previousQuery: String? = null
    private var isFirstQuery = true  // Bandera para controlar la primera vez

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListarUsuariosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userAdapter = UsuarioAdapter(usuarioViewModel, requireContext())

        val searchView = binding.searchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true  // No es necesario hacer nada aquí
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Solo cargar los usuarios si el texto ha cambiado y no es la primera vez que se accede
                if (isFirstQuery) {
                    isFirstQuery = false  // Se establece la bandera después de la primera vez
                    return true
                }

                // Solo cargar los usuarios si el texto ha cambiado
                if (newText != previousQuery) {
                    usuarioViewModel.cargarUsuarios(usuarioFiltro = newText ?: "")
                    previousQuery = newText
                }
                return true
            }
        })

        binding.userListRecyclerView.adapter = userAdapter
        binding.userListRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        usuarioViewModel.errorMessage.observe(viewLifecycleOwner) { event -> // Cambia error a errorMessage
            event.getContentIfNotHandled()?.let { errorMessage ->
//                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                Snackbar.make(requireView(), errorMessage, Snackbar.LENGTH_LONG).show()
            }
        }

        usuarioViewModel.mensaje.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { mensaje ->
//                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
                Snackbar.make(requireView(), mensaje, Snackbar.LENGTH_LONG).show()
            }
        }

        usuarioViewModel.users.observe(viewLifecycleOwner) { users ->
            userAdapter.submitList(users)
        }

        // Cargar usuarios
        //cargarUsuarios()

        setupFab()

        // Observa el estado de la red y ejecuta una acción específica en reconexión
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                NetworkStatusHelper.networkAvailable
                    .collect { isConnected ->
                        if (isConnected && previousConnectionState == null) {
                            cargarUsuarios()
                        } else if (isConnected && previousConnectionState == false) {
//                                Log.d("ParteDiarioFormFragment", "Conexión restaurada, recargando datos...")
                            // Realiza una acción específica al recuperar conexión

                            reloadData()
                        }

                        previousConnectionState = isConnected
                    }
            }
        }
    }

    private fun reloadData() {
//        Toast.makeText(requireContext(), "Conexión restaurada, recargando datos...", Toast.LENGTH_SHORT).show()

        // Cargar usuarios
        cargarUsuarios()

    }

    private fun cargarUsuarios() {
        usuarioViewModel.cargarUsuarios()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        previousConnectionState = null // Restablecer el estado de la conexión
    }

    override fun onResume() {
        super.onResume()
        binding.searchView.setQuery("", false)
    }

    private fun setupFab() {
        val fab: FloatingActionButton? = activity?.findViewById(R.id.fab)
        fab?.visibility = View.VISIBLE
        fab?.setImageResource(R.drawable.ic_add)

        fab?.setOnClickListener {
            navigateToCreateUserForm()
        }
    }

    private fun navigateToCreateUserForm() {
        val bundle = bundleOf("editMode" to false)
        findNavController().navigate(
            R.id.action_nav_gestion_usuarios_to_nav_userFormFragment_create,bundle
        )
    }

}