package com.example.adminobr.ui.usuarios

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.adminobr.R
import com.example.adminobr.data.Usuario
import com.example.adminobr.databinding.FragmentUserFormBinding
import com.example.adminobr.ui.common.ProgressDialogFragment
import com.example.adminobr.utils.SessionManager
import com.example.adminobr.viewmodel.UsuarioViewModel
import com.example.adminobr.viewmodel.UsuarioViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout

class UsuarioFormFragment : Fragment() {
    private val viewModel: UsuarioViewModel by viewModels {
        UsuarioViewModelFactory(requireActivity().application)
    }

    private lateinit var binding: FragmentUserFormBinding
    private val _usuario = MutableLiveData<Usuario?>()
    private val usuario: LiveData<Usuario?> = _usuario

    private val estadoItems = arrayOf("Activo", "Inactivo")
    private var userId: Int? = null
    private var selectedEstado = 1
    private var isEditMode = false

    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserFormBinding.inflate(inflater, container, false)

        sessionManager = SessionManager(requireContext())

        // Configurar AutoCompleteTextView para estado de usuario
        val usuarioAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, estadoItems)
        binding.estadoAutocomplete.setAdapter(usuarioAdapter)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isEditMode = arguments?.getBoolean("isEditMode", false) ?: false
        (activity as? AppCompatActivity)?.supportActionBar?.title = if (isEditMode) "Editar Usuario" else "Nuevo Usuario"

        val userIdEdit = arguments?.getInt("userId", -1) ?: -1
        if (isEditMode && userIdEdit != -1) {
            cargarDatosUsuario(userIdEdit)
        }

        setupFab()
        observeViewModels()
        setupTextWatchers()
        setupListeners()

        setTextViewToUppercase(binding.nombreEditText)
        setTextViewToUppercase(binding.apellidoEditText)
        setTextViewToLowercase(binding.emailEditText)
    }

    private fun actualizarUiConDatosDeUsuario(usuario: Usuario) {
        binding.apply {
            nuevoUsuarioIdTextView.setText(usuario.id.toString())
            legajoEditText.setText(usuario.legajo)
            emailEditText.setText(usuario.email)
            dniEditText.setText(usuario.dni)
            nombreEditText.setText(usuario.nombre)
            apellidoEditText.setText(usuario.apellido)
            telefonoEditText.setText(usuario.telefono)
            estadoAutocomplete.setText(if (usuario.estadoId == 1) "Activo" else "Inactivo", false)
        }
    }

    private fun setTextViewToLowercase(textView: TextView) {
        textView.filters = arrayOf(InputFilter { source, start, end, dest, dstart, dend ->
            source.toString().lowercase()
        })
    }

    private fun setTextViewToUppercase(textView: TextView) {
        textView.filters = arrayOf(InputFilter.AllCaps())
    }

    private fun cargarDatosUsuario(id: Int) {
        viewModel.obtenerUsuarioPorId(id)
        viewModel.users.observe(viewLifecycleOwner) { usuarios ->
            if (!usuarios.isNullOrEmpty()) {
                actualizarUiConDatosDeUsuario(usuarios[0])
                usuarios[0].id?.let { userId = it } // Asignación segura si el ID no es nulo
            }
        }
    }

    private fun setupTextWatchers() {
        addTextWatcher(binding.legajoTextInputLayout, "Campo requerido")
        addTextWatcher(binding.emailTextInputLayout, "Campo requerido")
        addTextWatcher(binding.dniTextInputLayout, "Campo requerido")
        addTextWatcher(binding.nombreTextInputLayout, "Campo requerido")
        addTextWatcher(binding.apellidoTextInputLayout, "Campo requerido")
        addTextWatcher(binding.telefonoTextInputLayout, "Campo requerido")
    }

    private fun setupListeners() {
        binding.estadoAutocomplete.setOnItemClickListener { parent, _, position, _ ->
            val selectedEstadoText = parent.getItemAtPosition(position) as String
            selectedEstado = if (selectedEstadoText == "Activo") 1 else 0
        }

        binding.guardarButton.setOnClickListener {
            if (validarContrasenas()) {
                guardarUsuario()
            }
        }
    }

        private fun validarContrasenas(): Boolean {
        val password = binding.passwordEditText.text.toString()
        val password2 = binding.password2EditText.text.toString()
        return if (password != password2) {
            Toast.makeText(requireContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }

    private fun guardarUsuario() {
        if (validarCampos()) {
            val usuario = Usuario(
                id = userId,
                legajo = binding.legajoEditText.text.toString(),
                email = binding.emailEditText.text.toString(),
                dni = binding.dniEditText.text.toString(),
                password = binding.passwordEditText.text.toString(),
                nombre = binding.nombreEditText.text.toString(),
                apellido = binding.apellidoEditText.text.toString(),
                telefono = binding.telefonoEditText.text.toString(),
                userCreated = sessionManager.getUserId(),
                estadoId = selectedEstado
            )

            if (isEditMode) {
                Toast.makeText(context, "Guardando cambios...", Toast.LENGTH_SHORT).show()
                val newPassword = if (binding.passwordEditText.text?.isNotEmpty() == true) binding.passwordEditText.text.toString() else null
                viewModel.actualizarUsuario(usuario, newPassword)
            } else {
                viewModel.crearUsuario(usuario)
            }
        }
    }

    private fun validarCampos(): Boolean {
        var camposValidos = true
        binding.apply {
            if (legajoEditText.text.isNullOrEmpty()) {
                legajoTextInputLayout.error = "Campo requerido"
                camposValidos = false
            }
            if (emailEditText.text.isNullOrEmpty()) {
                emailTextInputLayout.error = "Campo requerido"
                camposValidos = false
            }
            if (dniEditText.text.isNullOrEmpty()) {
                dniTextInputLayout.error = "Campo requerido"
                camposValidos = false
            }
            if (nombreEditText.text.isNullOrEmpty()) {
                nombreTextInputLayout.error = "Campo requerido"
                camposValidos = false
            }
            if (apellidoEditText.text.isNullOrEmpty()) {
                apellidoTextInputLayout.error = "Campo requerido"
                camposValidos = false
            }
            if (telefonoEditText.text.isNullOrEmpty()) {
                telefonoTextInputLayout.error = "Campo requerido"
                camposValidos = false
            }
        }

        if (!camposValidos) {
            Toast.makeText(requireContext(), "Por favor, complete todos los campos requeridos", Toast.LENGTH_SHORT).show()
        }
        return camposValidos
    }

    private fun setupFab() {
        val fab: FloatingActionButton? = activity?.findViewById(R.id.fab)
        fab?.apply {
            visibility = View.GONE
            setImageResource(R.drawable.ic_add)
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
            setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white)))

            setOnClickListener {
                limpiarFormulario()
                visibility = View.GONE
            }
        }
    }

    private fun limpiarFormulario() {
        binding.apply {
            legajoEditText.text?.clear()
            nuevoUsuarioIdTextView.text?.clear()
            emailEditText.text?.clear()
            dniEditText.text?.clear()
            nombreEditText.text?.clear()
            apellidoEditText.text?.clear()
            telefonoEditText.text?.clear()
            estadoAutocomplete.text?.clear()
            passwordEditText.text?.clear()
            password2EditText.text?.clear()
        }
        habilitarFormulario()
    }

    private fun habilitarFormulario() {
        binding.apply {
            legajoEditText.isEnabled = true
            emailEditText.isEnabled = true
            dniEditText.isEnabled = true
            nombreEditText.isEnabled = true
            apellidoEditText.isEnabled = true
            telefonoEditText.isEnabled = true
            estadoTextInputLayout.isEnabled = true
            passwordEditText.isEnabled = true
            password2EditText.isEnabled = true
            guardarButton.isEnabled = true
        }
    }

    private fun deshabilitarFormulario() {
        binding.apply {
            legajoEditText.isEnabled = false
            emailEditText.isEnabled = false
            dniEditText.isEnabled = false
            nombreEditText.isEnabled = false
            apellidoEditText.isEnabled = false
            telefonoEditText.isEnabled = false
            estadoTextInputLayout.isEnabled = false
            passwordEditText.isEnabled = false
            password2EditText.isEnabled = false
            guardarButton.isEnabled = false
        }
    }

    private var progressDialog: ProgressDialogFragment? = null // Mostrar ProgressDialog

    private fun observeViewModels() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                showProgressDialog()
            } else {
                dismissProgressDialog()
            }
        }

        // Aquí consumimos el mensaje usando getContentIfNotHandled()
        viewModel.errorMessage.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { mensaje ->
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.mensaje.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { mensaje ->
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = ProgressDialogFragment()
        }
        if (!progressDialog!!.isAdded) {
            progressDialog!!.show(childFragmentManager, "progress")
        }
    }

    private fun dismissProgressDialog() {
        progressDialog?.dismiss()
    }

    private fun addTextWatcher(textInputLayout: TextInputLayout, errorMessage: String) {
        textInputLayout.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                textInputLayout.error = if (s.isNullOrEmpty()) errorMessage else null
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
}
