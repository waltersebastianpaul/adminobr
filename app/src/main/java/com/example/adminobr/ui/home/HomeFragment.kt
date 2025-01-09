package com.example.adminobr.ui.home

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.adminobr.R
import com.example.adminobr.databinding.FragmentHomeBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import com.example.adminobr.utils.SessionManager

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var empresaName: String // Declaración como propiedad de la clase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inicializar sessionManager aquí
        sessionManager = SessionManager(requireContext())

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.parteDiarioCardView.setOnClickListener {
            val bundle = bundleOf("editMode" to false)
            findNavController().navigate(R.id.nav_parteDiarioFormFragment, bundle) // Reemplaza R.id.nav_parteDiarioFormFragment con el ID del fragmento destino
        }

        binding.parteSimpleCardView.setOnClickListener {
            findNavController().navigate(R.id.nav_partesimple)
        }

        binding.listarPartesCardView.setOnClickListener {
            findNavController().navigate(R.id.nav_listarpartes)
        }

        // Obtener roles del usuario
        val userRoles = sessionManager.getUserRol()
        Log.d("HomeFragment", "Roles del usuario: $userRoles")
        // Constrol de visivilidad segun roles
        if (userRoles?.contains("supervisor") == true || userRoles?.contains("administrador") == true) {
            binding.parteSimpleCardView.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()

        // Establecer el título en el ActionBar
        (activity as? AppCompatActivity)?.supportActionBar?.title = "AdminObr"

        // Configuración del FloatingActionButton regular
        val fab: FloatingActionButton = requireActivity().findViewById(R.id.fab)
        fab.visibility = View.VISIBLE
        fab.setImageResource(R.drawable.ic_add)
        fab.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
        fab.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorWhite))

        fab.setOnClickListener {
            val bundle = bundleOf("editMode" to false)
            findNavController().navigate(R.id.nav_parteDiarioFormFragment, bundle) // Reemplaza R.id.nav_partediario con el ID del fragmento destino
        }

//        // Configuración del ExtendedFloatingActionButton
//        val extendedFab: ExtendedFloatingActionButton? = requireActivity().findViewById(R.id.extended_fab)
//        extendedFab?.let { fab ->
//            fab.setIconResource(R.drawable.ic_add)
//            fab.text = getString(R.string.etiqueta_fab)
//            fab.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
//            fab.backgroundTintList = ColorStateList.valueOf(
//                ContextCompat.getColor(requireContext(), R.color.colorPrimary)
//            )
//            fab.iconTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
//
//            fab.setOnClickListener {
//                try {
//                    findNavController().navigate(R.id.nav_partediario)
//                } catch (e: Exception) {
//                    Snackbar.make(requireView(), "Error al navegar: ${e.message}", Snackbar.LENGTH_LONG).show()
//                }
//            }
//        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
