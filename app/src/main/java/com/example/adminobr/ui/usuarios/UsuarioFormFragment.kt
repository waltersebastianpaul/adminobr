package com.example.adminobr.ui.usuarios

import android.app.Activity
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.adminobr.R
import com.example.adminobr.data.Estado
import com.example.adminobr.data.Rol
import com.example.adminobr.data.Usuario
import com.example.adminobr.databinding.FragmentUserFormBinding
import com.example.adminobr.utils.AppUtils
import com.example.adminobr.utils.AutocompleteManager
import com.example.adminobr.utils.FeedbackVisualUtils
import com.example.adminobr.utils.NetworkStatusHelper
import com.example.adminobr.utils.SessionManager
import com.example.adminobr.viewmodel.AppDataViewModel
import com.example.adminobr.viewmodel.UsuarioViewModel
import com.example.adminobr.viewmodel.UsuarioViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout

class UsuarioFormFragment : Fragment() {
    private val viewModel: UsuarioViewModel by viewModels {
        UsuarioViewModelFactory(requireActivity().application)
    }

    private lateinit var autocompleteManager: AutocompleteManager
    private val appDataViewModel: AppDataViewModel by activityViewModels()

    private var _binding: FragmentUserFormBinding? = null
    private val binding get() = _binding!! // Binding para acceder a los elementos del layout

    private val _usuario = MutableLiveData<Usuario?>()
    private val usuario: LiveData<Usuario?> = _usuario

    //private val estadoItems = arrayOf("Activo", "Inactivo", "Suspendido")
    private var userId: Int? = null
    private var selectedEstado: Estado? = null
    private var selectedRole: Rol? = null
    private var editMode = false
    private var editType: EditType = EditType.EDIT_ALL

    private lateinit var roleAutocomplete: AutoCompleteTextView
    private lateinit var estadoAutocomplete: AutoCompleteTextView

    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserFormBinding.inflate(inflater, container, false)

        sessionManager = SessionManager(requireContext())

//        // Configurar AutoCompleteTextView para estado de usuario
//        val usuarioAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, estadoItems)
//        binding.estadoAutocomplete.setAdapter(usuarioAdapter)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        roleAutocomplete = binding.rolAutoComplete
        estadoAutocomplete = binding.estadoAutocomplete

        // Inicializar AutocompleteManager
        autocompleteManager = AutocompleteManager(requireContext(), appDataViewModel)

        // Cargar estados y capturar el objeto Estado seleccionado
        autocompleteManager.loadEstados(
            estadoAutocomplete,
            this
        ) { estado ->
            selectedEstado = estado
            Log.d("UsuarioFormFragment", "Estado seleccionado selectedEstado: $estado")
        }

        // Cargar roles y capturar el objeto Rol seleccionado
        autocompleteManager.loadRoles(
            roleAutocomplete,
            this
        ) { rol ->
            selectedRole = rol // Asignar el ID del rol a selectedRole
        }

        // Obtener el modo de edición del argumento
        editMode = arguments?.getBoolean("editMode", false) ?: false

        // Obtener el modo de edición del argumento (EDIT_PROFILE, CHANGE_PASSWORD, EDIT_ALL)
        editType = arguments?.getString("editType")?.let { EditType.valueOf(it) } ?: EditType.EDIT_ALL

        // Configura el título y visibilidad de los campos basados en `editType`
        setFormTitleAndVisibility()
        setupFab()
        observeViewModels()
        setupTextWatchers()
        setupListeners()

        setTextViewToUppercase(binding.nombreEditText)
        setTextViewToUppercase(binding.apellidoEditText)

        setTextViewToLowercase(binding.emailEditText)

//        setTextViewToUppercase(roleAutocomplete)
//        setTextViewToUppercase(estadoAutocomplete)

        // Cargar el usuario si es un modo de edisión, independientemente del tipo de edición
        val userId = arguments?.getInt("userId", -1) ?: -1
        if (userId != -1) {
            cargarDatosUsuario(userId)
        }

    }

    // Método que se ejecuta al destruir la actividad.
    override fun onDestroyView() {
        super.onDestroyView()

        // Restaurar el color original
        FeedbackVisualUtils.restaurarColorOriginal(requireActivity(), binding.guardarButton)

        // Limpiar el binding
        _binding = null
    }

    private fun setFormTitleAndVisibility() {
        when (editType) {
            EditType.EDIT_PROFILE -> {
                (activity as? AppCompatActivity)?.supportActionBar?.title = "Editar Perfil"
                configureFieldsForProfileEdit()
            }
            EditType.CHANGE_PASSWORD -> {
                (activity as? AppCompatActivity)?.supportActionBar?.title = "Cambiar Contraseña"
                configureFieldsForPasswordChange()
            }
            EditType.EDIT_ALL -> {
                val title = if (editMode) "Editar Usuario" else "Nuevo Usuario"
                (activity as? AppCompatActivity)?.supportActionBar?.title = title
                configureFieldsForFullEdit()
            }
        }
    }

    private fun configureFieldsForProfileEdit() {
        binding.apply {
            // Mostrar solo los campos de perfil editables
            legajoTextInputLayout.isEnabled = false
            estadoTextInputLayout.visibility = View.GONE
            rolTextInputLayout.visibility = View.GONE
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
            rolTextInputLayout.visibility = View.GONE
        }
    }

    private fun configureFieldsForFullEdit() {
        binding.apply {
            // Mostrar todos los campos para crear o editar usuario
            legajoTextInputLayout.isEnabled = true
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
            if (editMode) {
//                rolTextInputLayout.visibility = View.GONE
                rolTextInputLayout.isEnabled = false

            } else {
//                rolTextInputLayout.visibility = View.VISIBLE
                rolTextInputLayout.isEnabled = true
            }
        }
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

            // Configura el texto en `estadoAutocomplete` sin abrir el menú desplegable.
            selectedEstado = autocompleteManager.getEstadoById(usuario.estadoId)
            estadoAutocomplete.setText(selectedEstado?.toString() ?: "", false)
            estadoAutocomplete.dismissDropDown()

            // Configura el texto en `roleAutocomplete` sin abrir el menú desplegable.
            val rolId = usuario.principalRole?.let { autocompleteManager.getRolByName(it) }
            if (rolId != null) {
                selectedRole = rolId.id.let { autocompleteManager.getRolById(it) }
            }
            roleAutocomplete.setText(selectedRole?.toString() ?: "", false)
            roleAutocomplete.dismissDropDown()

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
        // Ocultar el teclado usando AppUtils
        AppUtils.closeKeyboard(requireActivity(), view)

        when (editType) {
            EditType.EDIT_PROFILE -> {
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
                    viewModel.actualizarPerfilUsuario(usuario, nuevaContrasena) { success ->
                        if (success) {
                            FeedbackVisualUtils.mostrarFeedbackVisualSuccessTemp(requireActivity(),  4000L, binding.guardarButton)
                            Toast.makeText(requireContext(), "Perfil actualizado exitosamente", Toast.LENGTH_SHORT).show()
//                            deshabilitarFormulario()
                        } else {
                            FeedbackVisualUtils.mostrarFeedbackVisualErrorTemp(requireActivity(), 4000L, binding.guardarButton)
                            Toast.makeText(requireContext(), "Error al actualizar perfil", Toast.LENGTH_SHORT).show()
                        }
                    }
//                    Toast.makeText(requireContext(), "Actualizando perfil...", Toast.LENGTH_SHORT).show()
                }
            }
            EditType.CHANGE_PASSWORD -> {
                val nuevaContrasena = binding.passwordEditText.text.toString()
                if (nuevaContrasena != "") {
                    val usuario = Usuario(
                        id = userId
                    )
                    viewModel.actualizarContrasenaUsuario(usuario, nuevaContrasena) { success ->
                        if (success) {
                            FeedbackVisualUtils.mostrarFeedbackVisualSuccessTemp(requireActivity(),  4000L, binding.guardarButton)
                            Toast.makeText(requireContext(), "Contraseña actualizada exitosamente", Toast.LENGTH_SHORT).show()
//                            deshabilitarFormulario()
                        } else {
                            FeedbackVisualUtils.mostrarFeedbackVisualErrorTemp(requireActivity(), 4000L, binding.guardarButton)
                            Toast.makeText(requireContext(), "Error al actualizar contraseña", Toast.LENGTH_SHORT).show()
                        }
                    }
//                    Toast.makeText(requireContext(), "Actualizando contraseña...", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Contraseña vacía", Toast.LENGTH_SHORT).show()
                }
            }

            EditType.EDIT_ALL -> {
                if (validarCamposCompletos()) {
                    // Verificar si hay conexión a internet
                    if (!NetworkStatusHelper.isNetworkAvailable()) {
                        // Opcionalmente, puedes quitar el foco de la vista actual
                        AppUtils.clearFocus(requireContext())

                        Toast.makeText(requireContext(), "No hay conexión a internet", Toast.LENGTH_SHORT).show()
                        return
                    }

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
                        estadoId = selectedEstado?.id ?: 1
                    )
                    if (editMode) {
                        val newPassword = binding.passwordEditText.text?.toString()
                        viewModel.actualizarUsuario(usuario, newPassword) { success ->
                            if (success) {
                                // Feedback visual para éxito
                                FeedbackVisualUtils.mostrarFeedbackVisualSuccess(requireActivity(), binding.guardarButton)
                                Toast.makeText(requireContext(), "Usuario actualizado exitosamente", Toast.LENGTH_SHORT).show()
                                deshabilitarFormulario()

                                // Mostrar el botón flotante
                                val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
                                fab.visibility = View.VISIBLE
                            } else {
                                // Feedback visual para error
                                FeedbackVisualUtils.mostrarFeedbackVisualError(requireActivity(), binding.guardarButton)
                                Toast.makeText(requireContext(), "Error al actualizar usuario", Toast.LENGTH_SHORT).show()
                            }
                        }
//                        Toast.makeText(requireContext(), "Actualizando Usuario...", Toast.LENGTH_SHORT).show()
                    } else {
                        // Llamada a la función de creación con callback
                        viewModel.crearUsuario(usuario) { success, nuevoId ->
                            if (success) {
                                nuevoId?.let {
                                    // Actualizar el ID en la vista cuando se crea exitosamente
                                    binding.nuevoUsuarioIdTextView.setText(it.toString())
                                }
                                // Usar la función de feedback visual
                                FeedbackVisualUtils.mostrarFeedbackVisualSuccess(requireActivity(), binding.guardarButton) // Cambiar el color al guardar con éxito
                                deshabilitarFormulario()

                                // Mostrar el botón flotante
                                val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
                                fab.visibility = View.VISIBLE
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Error al crear usuario",
                                    Toast.LENGTH_SHORT
                                ).show()
                                // Usar la función de feedback visual
                                FeedbackVisualUtils.mostrarFeedbackVisualError(requireActivity(), binding.guardarButton) // Cambiar el color al error
                            }
                        }
//                        Toast.makeText(requireContext(), "Creando usuario...", Toast.LENGTH_SHORT).show()
                    }
                    (context as? Activity)?.currentFocus?.clearFocus()
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
            setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorWhite)))

            setOnClickListener {
                limpiarFormulario()
                visibility = View.GONE
            }
        }
    }

    private fun limpiarFormulario() {
        // Limpiar todos los campos de texto y autocompletado
        binding.apply {
            listOf(
                legajoEditText,
                nuevoUsuarioIdTextView,
                emailEditText,
                dniEditText,
                nombreEditText,
                apellidoEditText,
                telefonoEditText,
                estadoAutocomplete,
                passwordEditText,
                password2EditText
            ).forEach {
                it.text?.clear()   // Limpia el contenido del campo
                it.error = null    // Limpia cualquier mensaje de error en el campo
            }
        }

        habilitarFormulario()
    }

    private fun habilitarFormulario() {
        binding.apply {
            legajoTextInputLayout.isEnabled = true
            emailTextInputLayout.isEnabled = true
            dniTextInputLayout.isEnabled = true
            nombreTextInputLayout.isEnabled = true
            apellidoTextInputLayout.isEnabled = true
            telefonoTextInputLayout.isEnabled = true
            estadoTextInputLayout.isEnabled = true
            rolTextInputLayout.isEnabled = true
            passwordTextInputLayout.isEnabled = true
            password2TextInputLayout.isEnabled = true
            guardarButton.isEnabled = true
        }
    }

    private fun deshabilitarFormulario() {
        binding.apply {
            legajoTextInputLayout.isEnabled = false
            emailTextInputLayout.isEnabled = false
            dniTextInputLayout.isEnabled = false
            nombreTextInputLayout.isEnabled = false
            apellidoTextInputLayout.isEnabled = false
            telefonoTextInputLayout.isEnabled = false
            estadoTextInputLayout.isEnabled = false
            rolTextInputLayout.isEnabled = false
            passwordTextInputLayout.isEnabled = false
            password2TextInputLayout.isEnabled = false
            guardarButton.isEnabled = false
        }
    }

    private fun observeViewModels() {

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
