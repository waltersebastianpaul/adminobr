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

    private val estadoItems = arrayOf("Activo", "Inactivo", "Suspendido")
    private var userId: Int? = null
    private var selectedEstado = 1
    private var editUserMode = false
    private var editMode: EditMode = EditMode.EDIT_ALL

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

        // Obtener el modo de edición del argumento
        editUserMode = arguments?.getBoolean("editUserMode", false) ?: false

        // Obtener el modo de edición del argumento (EDIT_PROFILE, CHANGE_PASSWORD, EDIT_ALL)
        editMode = arguments?.getString("editMode")?.let { EditMode.valueOf(it) } ?: EditMode.EDIT_ALL

        // Cargar el usuario si es un modo de edición, independientemente del tipo de edición
        val userId = arguments?.getInt("userId", -1) ?: -1
        if (userId != -1) {
            cargarDatosUsuario(userId)
        }

        // Configura el título y visibilidad de los campos basados en `editMode`
        setFormTitleAndVisibility()
        setupFab()
        observeViewModels()
        setupTextWatchers()
        setupListeners()

// Ver de implementar para nombre y apellido, tipo Titulo, solo primera de cada palabra en mayuscula
        setTextViewToUppercase(binding.nombreEditText)
        setTextViewToUppercase(binding.apellidoEditText)
        setTextViewToLowercase(binding.emailEditText)
    }

    private fun setFormTitleAndVisibility() {
        when (editMode) {
            EditMode.EDIT_PROFILE -> {
                (activity as? AppCompatActivity)?.supportActionBar?.title = "Editar Perfil"
                configureFieldsForProfileEdit()
            }
            EditMode.CHANGE_PASSWORD -> {
                (activity as? AppCompatActivity)?.supportActionBar?.title = "Cambiar Contraseña"
                configureFieldsForPasswordChange()
            }
            EditMode.EDIT_ALL -> {
                val title = if (editUserMode) "Editar Usuario" else "Nuevo Usuario"
                (activity as? AppCompatActivity)?.supportActionBar?.title = title
                configureFieldsForFullEdit()
            }
        }
    }

    private fun configureFieldsForProfileEdit() {
        binding.apply {
            // Mostrar solo los campos de perfil editables
            legajoEditText.isEnabled = false
            estadoTextInputLayout.visibility = View.GONE
        }
    }

    private fun configureFieldsForPasswordChange() {
        binding.apply {
            // Mostrar solo los campos de cambio de contraseña
            legajoTextInputLayout.visibility = View.GONE
            nuevoUsuarioIdTextInputLayout.visibility = View.GONE
            emailTextInputLayout.visibility = View.GONE
            dniTextInputLayout.visibility = View.GONE
            nombreTextInputLayout.visibility = View.GONE
            apellidoTextInputLayout.visibility = View.GONE
            telefonoTextInputLayout.visibility = View.GONE
            estadoTextInputLayout.visibility = View.GONE
        }
    }

    private fun configureFieldsForFullEdit() {
        binding.apply {
            // Mostrar todos los campos para crear o editar usuario
            legajoEditText.isEnabled = true
            legajoTextInputLayout.visibility = View.VISIBLE
            nuevoUsuarioIdTextInputLayout.visibility = View.VISIBLE
            emailTextInputLayout.visibility = View.VISIBLE
            dniTextInputLayout.visibility = View.VISIBLE
            nombreTextInputLayout.visibility = View.VISIBLE
            apellidoTextInputLayout.visibility = View.VISIBLE
            telefonoTextInputLayout.visibility = View.VISIBLE
            passwordTextInputLayout.visibility = View.VISIBLE
            password2TextInputLayout.visibility = View.VISIBLE
            estadoTextInputLayout.visibility = View.VISIBLE
        }
    }


//    private fun configureFields() {
//        when (editMode) {
//            EditMode.EDIT_PROFILE -> {
//                binding.apply {
//                    // Ocultar campos innecesarios para edición de perfil
//                    passwordTextInputLayout.visibility = View.GONE
//                    password2TextInputLayout.visibility = View.GONE
//                    legajoEditText.isEnabled = false
//                }
//            }
//            EditMode.CHANGE_PASSWORD -> {
//                binding.apply {
//                    // Mostrar solo campos de contraseña
//                    passwordTextInputLayout.visibility = View.VISIBLE
//                    password2TextInputLayout.visibility = View.VISIBLE
//                    legajoTextInputLayout.visibility = View.GONE
//                    nuevoUsuarioIdTextInputLayout.visibility = View.GONE
//                    emailTextInputLayout.visibility = View.GONE
//                    dniTextInputLayout.visibility = View.GONE
//                    nombreTextInputLayout.visibility = View.GONE
//                    apellidoTextInputLayout.visibility = View.GONE
//                    telefonoTextInputLayout.visibility = View.GONE
//                    estadoTextInputLayout.visibility = View.GONE
//                }
//            }
//            EditMode.EDIT_ALL -> {
//                // Mostrar todos los campos para crear o editar usuario
//                binding.apply {
//                    passwordTextInputLayout.visibility = View.VISIBLE
//                    password2TextInputLayout.visibility = View.VISIBLE
//                    legajoTextInputLayout.visibility = View.VISIBLE
//                    legajoEditText.isEnabled = true
//                    nuevoUsuarioIdTextInputLayout.visibility = View.VISIBLE
//                    emailTextInputLayout.visibility = View.VISIBLE
//                    dniTextInputLayout.visibility = View.VISIBLE
//                    nombreTextInputLayout.visibility = View.VISIBLE
//                    apellidoTextInputLayout.visibility = View.VISIBLE
//                    telefonoTextInputLayout.visibility = View.VISIBLE
//                    estadoTextInputLayout.visibility = View.VISIBLE
//                }
//            }
//        }
//    }

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

//            selectedEstado = when (selectedEstadoText) {
//                "Activo" -> 1
//                "Inactivo" -> 2
//                "Suspendido" -> 3
//                else -> 0 // Valor por defecto si no coincide con ninguna opción
//            }

            selectedEstado = when (selectedEstadoText) {
                "Activo" -> 1
                "Inactivo" -> 2
                "Suspendido" -> 3
                else -> throw IllegalArgumentException("Estado desconocido: $selectedEstadoText")
            }
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
        when (editMode) {
            EditMode.EDIT_PROFILE -> {
                if (validarCamposPerfil()) {
                    val usuario = Usuario(
                        id = userId,
                        email = binding.emailEditText.text.toString(),
                        dni = binding.dniEditText.text.toString(),
                        nombre = binding.nombreEditText.text.toString(),
                        apellido = binding.apellidoEditText.text.toString(),
                        telefono = binding.telefonoEditText.text.toString()
                    )

                    val nuevaContrasena = if (binding.passwordEditText.text.isNullOrEmpty()) {
                        null // Si la contraseña está vacía, se pasa como null
                    } else {
                        binding.passwordEditText.text.toString()
                    }

                    viewModel.actualizarPerfilUsuario(usuario, nuevaContrasena)
                    Toast.makeText(requireContext(), "Actualizando perfil...", Toast.LENGTH_SHORT).show()
                }
            }
            EditMode.CHANGE_PASSWORD -> {
                val nuevaContrasena = binding.passwordEditText.text.toString()
                if (nuevaContrasena != "") {
                    val usuario = Usuario(
                        id = userId
                    )
                    viewModel.actualizarContrasenaUsuario(usuario, binding.passwordEditText.text.toString())
                    Toast.makeText(requireContext(), "Actualizando contraseña...", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Contraseña vacía", Toast.LENGTH_SHORT).show()
                }

            }

            EditMode.EDIT_ALL -> {
                if (validarCamposCompletos()) {
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
                    if (editUserMode) {
                        viewModel.actualizarUsuario(usuario, binding.passwordEditText.text?.toString())
                        Toast.makeText(requireContext(), "Actualizando usuario...", Toast.LENGTH_SHORT).show()
                        deshabilitarFormulario()
                    } else {
                        viewModel.crearUsuario(usuario)
                        Toast.makeText(requireContext(), "Creando usuario...", Toast.LENGTH_SHORT).show()
                        deshabilitarFormulario()
                    }
                }
            }
        }
    }

    private fun validarCamposPerfil(): Boolean {
        var camposValidos = true
        binding.apply {
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


    private fun validarCamposCompletos(): Boolean {
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
