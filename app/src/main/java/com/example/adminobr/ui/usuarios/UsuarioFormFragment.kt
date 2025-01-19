package com.example.adminobr.ui.usuarios

import android.app.Activity
import android.content.Intent
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.adminobr.MainActivity
import com.example.adminobr.R
import com.example.adminobr.data.Estado
import com.example.adminobr.data.Rol
import com.example.adminobr.data.Usuario
import com.example.adminobr.databinding.FragmentUserFormBinding
import com.example.adminobr.utils.AppUtils
import com.example.adminobr.utils.AutocompleteManager
import com.example.adminobr.utils.FeedbackVisualUtil
import com.example.adminobr.utils.LoadingDialogUtil
import com.example.adminobr.utils.NetworkStatusHelper
import com.example.adminobr.utils.SessionManager
import com.example.adminobr.viewmodel.AppDataViewModel
import com.example.adminobr.viewmodel.UsuarioViewModel
import com.example.adminobr.viewmodel.UsuarioViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
    private var editMode: Boolean = false
    private var editType: EditType = EditType.EDIT_ALL

    private lateinit var roleAutocomplete: AutoCompleteTextView
    private lateinit var estadoAutocomplete: AutoCompleteTextView

    // Manager para la gestión de sesiones, carga solo cuando se accede a él
    private val sessionManager by lazy { SessionManager(requireContext()) }

    private var previousConnectionState: Boolean? = null

    // Job para cancelar las corrutinas
    private var networkJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentUserFormBinding.inflate(inflater, container, false)


//        // Configurar AutoCompleteTextView para estado de usuario
//        val usuarioAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, estadoItems)
//        binding.estadoAutocomplete.setAdapter(usuarioAdapter)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtener el modo de edición
        editMode = arguments?.getBoolean("editMode", false) ?: false
        editType = arguments?.getString("editType")?.let { EditType.valueOf(it) } ?: EditType.EDIT_ALL

        Log.d("UsuarioFormFragment", "Modo de edición: $editMode, Tipo de edición: $editType, userId: $userId")

        // Cargar el usuario si es un modo de edición, independientemente del tipo de edición
        val userId = arguments?.getInt("userId", -1) ?: -1
        if (editMode && userId != -1) {
            cargarDatosUsuario(userId)
            deshabilitarFormulario()

            // Escuchar cambios en el estado de la red
            networkJob = lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    NetworkStatusHelper.networkAvailable.collect { isConnected ->
                        if (isConnected) {
                            cargarDatosUsuario(userId)
                            networkJob?.cancel() // Detener la escucha tras el éxito
                        }
                    }
                }
            }
        }

        // Observa el estado de la red y ejecuta una acción específica en reconexión
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                NetworkStatusHelper.networkAvailable.collect { isConnected ->
                    // Si la conexión se restauró
                    if (previousConnectionState == false && isConnected) {
                        // Recargar datos u otras acciones necesarias
                        reloadData()
                    }

                    // Si está en modo edición y el formulario estaba deshabilitado
                    if (editMode && isConnected && !binding.guardarButton.isEnabled) {
                        habilitarFormulario()
                    }

                    // Actualizar el estado de conexión previo
                    previousConnectionState = isConnected
                }
            }
        }

        roleAutocomplete = binding.rolAutoComplete
        estadoAutocomplete = binding.estadoAutocomplete

        // Inicializar AutocompleteManager
        autocompleteManager = AutocompleteManager(requireContext(), appDataViewModel, sessionManager)

        // Cargar datos de Autocomplete
        loadAllAutocompleteData()

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

    }

    private fun reloadData() {
//        Toast.makeText(requireContext(), "Conexión restaurada, recargando datos...", Toast.LENGTH_SHORT).show()

        // Acción que se ejecuta al reconectarse
        loadAllAutocompleteData()
    }

    private fun loadAllAutocompleteData() {
        loadDataIfEmpty(estadoAutocomplete) {
            autocompleteManager.loadEstados(
                estadoAutocomplete,
                this
            ) { estado ->
                selectedEstado = estado
                Log.d("UsuarioFormFragment", "Estado seleccionado: $estado")
            }
        }

        loadDataIfEmpty(roleAutocomplete) {
            autocompleteManager.loadRoles(
                roleAutocomplete,
                this
            ) { role ->
                selectedRole = role
                Log.d("UsuarioFormFragment", "Role seleccionado: $role")
            }
        }
    }

    /**
     * Verifica si el adaptador del AutoCompleteTextView está vacío antes de cargar los datos.
     *
     * @param autoCompleteTextView El AutoCompleteTextView a verificar.
     * @param loadFunction La función de carga que se ejecutará si el adaptador está vacío.
     */
    private fun loadDataIfEmpty(autoCompleteTextView: AutoCompleteTextView, loadFunction: () -> Unit) {
        val adapterCount = autoCompleteTextView.adapter?.count ?: 0
        if (adapterCount == 0) {
            loadFunction()
        }
    }

    // Método que se ejecuta al destruir la actividad.
    override fun onDestroyView() {
        super.onDestroyView()

        // Restaurar el color original
        FeedbackVisualUtil.restaurarColorOriginal(requireActivity(), binding.guardarButton)

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
            passwordSectionTitle.visibility = View.VISIBLE
            legajoTextInputLayout.isEnabled = false
            estadoTextInputLayout.visibility = View.GONE
            rolTextInputLayout.visibility = View.GONE

            passwordSectionTitle.visibility = View.VISIBLE
            actualPasswordTextInputLayout.visibility = View.VISIBLE
            passwordTextInputLayout.visibility = View.VISIBLE
            passwordTextInputLayout.hint = "Nueva contraseña"
            password2TextInputLayout.visibility = View.VISIBLE
            guardarButton.visibility = View.VISIBLE
            guardarButton.text = "Guardar Cambios"
        }
    }

    private fun configureFieldsForPasswordChange() {
        binding.apply {
            // Mostrar solo los campos de cambio de contraseña
            binding.passwordFieldsContainer.visibility = View.VISIBLE
            passwordSectionTitle.visibility = View.GONE
            actualPasswordTextInputLayout.visibility = View.VISIBLE
            passwordTextInputLayout.visibility = View.VISIBLE
            passwordTextInputLayout.hint = "Nueva contraseña"
            password2TextInputLayout.visibility = View.VISIBLE
            guardarButton.visibility = View.VISIBLE
            guardarButton.text = "Guardar Cambios"

            // Ocultar los campos de perfil editables
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
            guardarButton.visibility = View.VISIBLE

            estadoTextInputLayout.visibility = View.VISIBLE
            if (editMode) {
//                rolTextInputLayout.visibility = View.GONE
                rolTextInputLayout.isEnabled = false
                actualPasswordTextInputLayout.visibility = View.VISIBLE
                guardarButton.text = "Guardar Cambios"
            } else {
//                rolTextInputLayout.visibility = View.VISIBLE
                rolTextInputLayout.isEnabled = true
                passwordSectionTitle.visibility = View.GONE
                binding.passwordFieldsContainer.visibility = View.VISIBLE
                actualPasswordTextInputLayout.visibility = View.GONE
                guardarButton.text = "Crear Usuario"
            }
        }
    }

    private fun actualizarUiConDatosDeUsuario(usuario: Usuario) {
        binding.apply {
            nuevoUsuarioIdTextView.setText(usuario.id.toString())
            // Asignar un nuevo valor al hint
            nuevoUsuarioIdTextInputLayout.hint = "Usuario ID"
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
    }

    private fun setupTextWatchers() {
        addTextWatcher(binding.legajoTextInputLayout, "Campo requerido")
        addTextWatcher(binding.emailTextInputLayout, "Campo requerido")
        addTextWatcher(binding.dniTextInputLayout, "Campo requerido")
        addTextWatcher(binding.nombreTextInputLayout, "Campo requerido")
        addTextWatcher(binding.apellidoTextInputLayout, "Campo requerido")
        addTextWatcher(binding.telefonoTextInputLayout, "Campo requerido")
        addTextWatcher(binding.actualPasswordTextInputLayout, "Campo requerido")
        addTextWatcher(binding.passwordTextInputLayout, "Campo requerido")
        addTextWatcher(binding.password2TextInputLayout, "Campo requerido")
    }

    private fun setupListeners() {
        binding.guardarButton.setOnClickListener {
            if (NetworkStatusHelper.isConnected()) {
                guardarUsuario()
            } else {
                Snackbar.make(binding.root, "No hay conexión a internet, intenta mas tardes.", Snackbar.LENGTH_LONG)
//                    .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.colorDanger))
                    .setTextColor(ContextCompat.getColor(requireContext(), R.color.danger_400))
                    .show()
                return@setOnClickListener
            }
        }

        // Configurar el clic en el título de la sección de contraseña
        binding.passwordSectionTitle.setOnClickListener {
            binding.passwordFieldsContainer.visibility = View.VISIBLE
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

                    val currentPassword = binding.actualPasswordEditText.text.toString()
                    val newPassword = binding.passwordEditText.text.toString()

                    viewModel.actualizarPerfilUsuario(
                        usuario,
                        currentPassword,
                        newPassword
                    ) { success ->
                        if (success) {
                            deshabilitarFormulario()
                            FeedbackVisualUtil.mostrarFeedbackVisualSuccessTemp(viewLifecycleOwner, requireActivity(), 4000L, binding.guardarButton){
                                // Este código se ejecuta cuando el feedback visual ha terminado
//                                binding.actualPasswordEditText.setText("")
//                                binding.passwordEditText.setText("")
//                                binding.password2EditText.setText("")

                                // Navegar a la actividad principal
                                context?.let {
                                    val intent = Intent(it, MainActivity::class.java)
                                    startActivity(intent)
                                    requireActivity().finish()
                                }
                            }
                        } else {
                            FeedbackVisualUtil.mostrarFeedbackVisualErrorTemp(viewLifecycleOwner, requireActivity(), 4000L, binding.guardarButton)
                        }
                    }
//                    Toast.makeText(requireContext(), "Actualizando perfil...", Toast.LENGTH_SHORT).show()
                }
            }
            EditType.CHANGE_PASSWORD -> {
                val currentPassword = binding.actualPasswordEditText.text.toString()
                val newPassword = binding.passwordEditText.text.toString()

                if (validarNuevaContrasena()) {
                    val usuario = Usuario(id = userId)

                    viewModel.actualizarContrasenaUsuario(
                        usuario,
                        currentPassword,
                        newPassword
                    ) { success ->
                        if (success) {
                            deshabilitarFormulario()
                            FeedbackVisualUtil.mostrarFeedbackVisualSuccessTemp(viewLifecycleOwner, requireActivity(), 4000L, binding.guardarButton){
                                // Este código se ejecuta cuando el feedback visual ha terminado
//                                binding.actualPasswordEditText.setText("")
//                                binding.passwordEditText.setText("")
//                                binding.password2EditText.setText("")

                                // Navegar a la actividad principal
                                context?.let {
                                    val intent = Intent(it, MainActivity::class.java)
                                    startActivity(intent)
                                    requireActivity().finish()
                                }
                            }
                        } else {
                            FeedbackVisualUtil.mostrarFeedbackVisualErrorTemp(viewLifecycleOwner, requireActivity(), 4000L, binding.guardarButton)
                        }
                    }
                }
            }
            EditType.EDIT_ALL -> {
                if (validarCamposCompletos()) {
//                    // Verificar si hay conexión a internet
//                    if (!NetworkStatusHelper.isNetworkAvailable()) {
//                        // Opcionalmente, puedes quitar el foco de la vista actual
//                        AppUtils.clearFocus(requireContext())
//
//                        Toast.makeText(requireContext(), "No hay conexión a internet", Toast.LENGTH_SHORT).show()
//                        return
//                    }

                    val usuario = Usuario(
                        id = userId,
                        legajo = binding.legajoEditText.text.toString().trim(),
                        email = binding.emailEditText.text.toString().trim(),
                        dni = binding.dniEditText.text.toString().trim(),
                        password = binding.passwordEditText.text.toString(),
                        nombre = binding.nombreEditText.text.toString().trim(),
                        apellido = binding.apellidoEditText.text.toString().trim(),
                        telefono = binding.telefonoEditText.text.toString(),
                        userCreated = sessionManager.getUserId(),
                        estadoId = selectedEstado?.id ?: 1,
                        roleId = selectedRole?.id ?: 3
                    )

                    if (editMode) {
                        val currentPassword = binding.actualPasswordEditText.text.toString()
                        val newPassword = binding.passwordEditText.text.toString()

                        viewModel.actualizarUsuario(usuario, currentPassword, newPassword) { success ->
                            if (success) {
                                // Feedback visual para éxito
                                FeedbackVisualUtil.mostrarFeedbackVisualSuccess(requireActivity(), binding.guardarButton)
//                                Toast.makeText(requireContext(), "Usuario actualizado exitosamente", Toast.LENGTH_SHORT).show()
                                deshabilitarFormulario()

                                // Mostrar el botón flotante
//                                val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
//                                fab.visibility = View.VISIBLE
                            } else {
                                // Feedback visual para error
                                FeedbackVisualUtil.mostrarFeedbackVisualErrorTemp(viewLifecycleOwner, requireActivity(), 4000L, binding.guardarButton)
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
                                    // Asignar un nuevo valor al hint
                                    binding.nuevoUsuarioIdTextInputLayout.hint = "Usuario ID"
                                }
                                // Usar la función de feedback visual
                                FeedbackVisualUtil.mostrarFeedbackVisualSuccess(requireActivity(), binding.guardarButton) // Cambiar el color al guardar con éxito
                                deshabilitarFormulario()

                                // Mostrar el botón flotante
                                val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
                                fab.visibility = View.VISIBLE
                            } else {
                                // Usar la función de feedback visual
                                FeedbackVisualUtil.mostrarFeedbackVisualErrorTemp(viewLifecycleOwner, requireActivity(), 4000L, binding.guardarButton) // Cambiar el color al error
                            }
                        }
//                        Toast.makeText(requireContext(), "Creando usuario...", Toast.LENGTH_SHORT).show()
                    }
                    (context as? Activity)?.currentFocus?.clearFocus()
                }
            }
        }
    }

    private fun validarNuevaContrasena(): Boolean {
        val currentPassword = binding.actualPasswordEditText.text.toString()
        val newPassword = binding.passwordEditText.text.toString()
        val confirmPassword = binding.password2EditText.text.toString()

        var isValid = true

        // Función auxiliar para validar un campo y establecer el error
        fun validateField(text: String?, textInputLayout: TextInputLayout, errorMessage: String): Boolean {
            return if (text.isNullOrEmpty()) {
                textInputLayout.error = errorMessage
                false
            } else {
                textInputLayout.isErrorEnabled = false
                true
            }
        }

        // Validar campos individuales
        val isCurrentPasswordValid = validateField(currentPassword, binding.actualPasswordTextInputLayout, "Campo requerido")
        val isNewPasswordValid = validateField(newPassword, binding.passwordTextInputLayout, "Campo requerido")
        val isConfirmPasswordValid = validateField(confirmPassword, binding.password2TextInputLayout, "Campo requerido")

        isValid = isValid && isCurrentPasswordValid && isNewPasswordValid && isConfirmPasswordValid

        // Validar que las contraseñas coincidan si ambos campos están llenos
        if (newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
            binding.passwordTextInputLayout.error = "Las contraseñas no coinciden"
            binding.password2TextInputLayout.error = "Las contraseñas no coinciden"
            isValid = false
        } else if (newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword == confirmPassword){
            binding.passwordTextInputLayout.isErrorEnabled = false
            binding.password2TextInputLayout.isErrorEnabled = false
        }

        return isValid
    }

    private fun validarCamposPerfil(): Boolean {
        var isValid = true
        binding.apply {
            if (emailEditText.text.isNullOrEmpty()) {
                emailTextInputLayout.error = "Campo requerido"
                isValid = false
            } else {
                emailTextInputLayout.isErrorEnabled = false
            }
            if (dniEditText.text.isNullOrEmpty()) {
                dniTextInputLayout.error = "Campo requerido"
                isValid = false
            } else {
                dniTextInputLayout.isErrorEnabled = false
            }
            if (nombreEditText.text.isNullOrEmpty()) {
                nombreTextInputLayout.error = "Campo requerido"
                isValid = false
            } else {
                nombreTextInputLayout.isErrorEnabled = false
            }
            if (apellidoEditText.text.isNullOrEmpty()) {
                apellidoTextInputLayout.error = "Campo requerido"
                isValid = false
            } else {
                apellidoTextInputLayout.isErrorEnabled = false
            }
            if (telefonoEditText.text.isNullOrEmpty()) {
                telefonoTextInputLayout.error = "Campo requerido"
                isValid = false
            } else {
                telefonoTextInputLayout.isErrorEnabled = false
            }

            if (passwordEditText.text.isNullOrEmpty() && password2EditText.text.isNullOrEmpty()) {
                // Si ambos campos de contraseña están vacíos, no hay validación de contraseña que hacer
                binding.actualPasswordTextInputLayout.isErrorEnabled = false
                passwordTextInputLayout.isErrorEnabled = false
                password2TextInputLayout.isErrorEnabled = false
            } else {
                if (actualPasswordEditText.text.isNullOrEmpty()) {
                    binding.actualPasswordTextInputLayout.error = "Campo requerido"
                    isValid = false
                } else {
                    binding.actualPasswordTextInputLayout.isErrorEnabled = false
                }

                if (passwordEditText.text.toString() != password2EditText.text.toString()) {
                    passwordTextInputLayout.error = "Las contraseñas no coinciden"
                    password2TextInputLayout.error = "Las contraseñas no coinciden"
                    isValid = false
                } else {
                    passwordTextInputLayout.isErrorEnabled = false
                    password2TextInputLayout.isErrorEnabled = false
                }
            }

        }
        if (!isValid) {
//            Toast.makeText(requireContext(), "Por favor, complete todos los campos requeridos", Toast.LENGTH_SHORT).show()
            Snackbar.make(requireView(), "Por favor, complete todos los campos requeridos", Snackbar.LENGTH_LONG).show()
        }
        return isValid
    }

    private fun validarCamposCompletos(): Boolean {
        var isValid = true
        binding.apply {
            if (legajoEditText.text.isNullOrEmpty()) {
                legajoTextInputLayout.error = "Campo requerido"
                isValid = false
            } else {
                legajoTextInputLayout.isErrorEnabled = false
            }
            if (emailEditText.text.isNullOrEmpty()) {
                emailTextInputLayout.error = "Campo requerido"
                isValid = false
            } else {
                emailTextInputLayout.isErrorEnabled = false
            }
            if (dniEditText.text.isNullOrEmpty()) {
                dniTextInputLayout.error = "Campo requerido"
                isValid = false
            } else {
                dniTextInputLayout.isErrorEnabled = false
            }
            if (nombreEditText.text.isNullOrEmpty()) {
                nombreTextInputLayout.error = "Campo requerido"
                isValid = false
            } else {
                nombreTextInputLayout.isErrorEnabled = false
            }
            if (apellidoEditText.text.isNullOrEmpty()) {
                apellidoTextInputLayout.error = "Campo requerido"
                isValid = false
            } else {
                apellidoTextInputLayout.isErrorEnabled = false
            }
            if (telefonoEditText.text.isNullOrEmpty()) {
                telefonoTextInputLayout.error = "Campo requerido"
                isValid = false
            } else {
                telefonoTextInputLayout.isErrorEnabled = false
            }

            if (editMode) {
                if (passwordEditText.text.isNullOrEmpty() && password2EditText.text.isNullOrEmpty()) {
                    // Si ambos campos de contraseña están vacíos, no hay validación de contraseña que hacer
                    binding.actualPasswordTextInputLayout.isErrorEnabled = false
                    passwordTextInputLayout.isErrorEnabled = false
                    password2TextInputLayout.isErrorEnabled = false
                } else {
                    if (actualPasswordEditText.text.isNullOrEmpty()) {
                        binding.actualPasswordTextInputLayout.error = "Campo requerido"
                        isValid = false
                    } else {
                        binding.actualPasswordTextInputLayout.isErrorEnabled = false
                    }

                    if (passwordEditText.text.toString() != password2EditText.text.toString()) {
                        passwordTextInputLayout.error = "Las contraseñas no coinciden"
                        password2TextInputLayout.error = "Las contraseñas no coinciden"
                        isValid = false
                    } else {
                        passwordTextInputLayout.isErrorEnabled = false
                        password2TextInputLayout.isErrorEnabled = false
                    }
                }
            } else {
                if (passwordEditText.text.toString() != password2EditText.text.toString()) {
                    passwordTextInputLayout.error = "Las contraseñas no coinciden"
                    password2TextInputLayout.error = "Las contraseñas no coinciden"
                    isValid = false
                } else {
                    passwordTextInputLayout.isErrorEnabled = false
                    password2TextInputLayout.isErrorEnabled = false
                }
            }
        }

        if (!isValid) {
//            Toast.makeText(requireContext(), "Por favor, complete todos los campos requeridos", Toast.LENGTH_SHORT).show()
            Snackbar.make(requireView(), "Por favor, complete todos los campos requeridos", Snackbar.LENGTH_LONG).show()
        }
        return isValid
    }

    private fun setupFab() {
        val fab: FloatingActionButton? = activity?.findViewById(R.id.fab)
        fab?.apply {
            visibility = View.GONE
            setImageResource(R.drawable.ic_add)
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
            setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorWhite)))

            setOnClickListener {
                // Restaurar el color original
                FeedbackVisualUtil.restaurarColorOriginal(requireActivity(), binding.guardarButton)

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
                actualPasswordEditText,
                passwordEditText,
                password2EditText
            ).forEach {
                it.text?.clear()   // Limpia el contenido del campo
                it.error = null    // Limpia cualquier mensaje de error en el campo
            }

            // Limpia el texto visible en los AutoCompleteTextView sin borrar los ítems del adaptador
            estadoAutocomplete.setText("", false)
            roleAutocomplete.setText("", false)
        }

        // Limpia los valores seleccionados internamente
        selectedRole = null
        selectedEstado = null

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
            passwordSectionTitle.isEnabled = true
            actualPasswordTextInputLayout.isEnabled = true
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
            passwordSectionTitle.isEnabled = false
            actualPasswordTextInputLayout.isEnabled = false
            passwordTextInputLayout.isEnabled = false
            password2TextInputLayout.isEnabled = false
            guardarButton.isEnabled = false
        }
    }

    private fun observeViewModels() {

        // Aquí consumimos el mensaje usando getContentIfNotHandled()
        viewModel.errorMessage.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { mensaje ->
//                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
                Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_LONG)
//                    .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.colorDanger))
                    .setTextColor(ContextCompat.getColor(requireContext(), R.color.danger_400))
                    .show()
            }
        }
//        viewModel.errorMessage.observe(viewLifecycleOwner) { event ->
//            event.getContentIfNotHandled()?.let { mensaje ->
//                Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_LONG).show()
//            }
//        }

        viewModel.fieldErrorMessage.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { errorCode ->
                when (errorCode) {
                    "wrong_password" -> {
                        binding.actualPasswordTextInputLayout.error = "Contraseña incorrecta."
                    }
                    "email" -> {
                        binding.emailTextInputLayout.error = "Email ya registrado."
                    }
                    "DNI" -> {
                        binding.dniTextInputLayout.error = "DNI ya registrado."
                    }
                    "legajo" -> {
                        binding.legajoTextInputLayout.error = "Legajo ya registrado."
                    }
                    else -> {
                        Log.e("UsuarioFormFragment", "Código de error desconocido: $errorCode")
                    }
                }
            }
        }

        viewModel.isNetworkAvailable.observe(viewLifecycleOwner) { isNetworkAvailable ->
            if (!isNetworkAvailable) {
                deshabilitarFormulario()
            }
        }

        viewModel.mensaje.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { mensaje ->
//                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
                Snackbar.make(requireView(), mensaje, Snackbar.LENGTH_LONG).show()
            }
        }

        // Observa el estado de carga
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isAdded && context != null) { // Verifica que el fragmento siga activo
                if (isLoading) {
                    LoadingDialogUtil.showLoading(requireContext(), "")
                } else {
                    LoadingDialogUtil.hideLoading(lifecycleScope, 500L)
                }
            }
        }

        viewModel.users.observe(viewLifecycleOwner) { usuarios ->
            if (!usuarios.isNullOrEmpty()) {
                actualizarUiConDatosDeUsuario(usuarios[0])
                usuarios[0].id?.let { userId = it } // Asignación segura si el ID no es nulo
            }
        }
    }

    private fun addTextWatcher(textInputLayout: TextInputLayout, errorMessage: String) {
        textInputLayout.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (textInputLayout.isErrorEnabled) {
                    if (s.isNullOrEmpty()) {
                        textInputLayout.error = errorMessage
                    } else {
                        textInputLayout.isErrorEnabled = false
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
}
