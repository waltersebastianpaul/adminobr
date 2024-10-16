package com.example.adminobr.ui.usuarios

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.compose.ui.semantics.text
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.adminobr.R
import com.example.adminobr.data.Usuario
import com.example.adminobr.databinding.FragmentUserFormBinding
import com.example.adminobr.ui.common.ProgressDialogFragment
import com.example.adminobr.utils.SessionManager
import com.example.adminobr.viewmodel.UsuarioViewModel
import com.example.adminobr.viewmodel.UsuarioViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout

class UserFormFragment : Fragment() {
    private val viewModel: UsuarioViewModel by viewModels {
        UsuarioViewModelFactory(requireActivity().application) // Se pasa la aplicación
    }
    private lateinit var _binding: FragmentUserFormBinding
    private val binding get() = _binding

    private var userId: Int? = null

    private var isEditMode = false

    private lateinit var legajoEditText: EditText
    private lateinit var nuevoUsuarioIdTextView: EditText
    private lateinit var emailEditText: EditText
    private lateinit var dniEditText: EditText
    private lateinit var nombreEditText: EditText
    private lateinit var apellidoEditText: EditText
    private lateinit var telefonoEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var password2EditText: EditText
    private lateinit var guardarButton: Button

    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserFormBinding.inflate(inflater, container, false)

        legajoEditText = binding.legajoEditText
        nuevoUsuarioIdTextView = binding.nuevoUsuarioIdTextView
        emailEditText = binding.emailEditText
        dniEditText = binding.dniEditText
        nombreEditText = binding.nombreEditText
        apellidoEditText = binding.apellidoEditText
        telefonoEditText = binding.telefonoEditText
        passwordEditText = binding.passwordEditText
        password2EditText = binding.password2EditText
        guardarButton = binding.guardarButton

        sessionManager = SessionManager(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Verificar si estamos en modo edición
        isEditMode = arguments?.getBoolean("isEditMode", false) ?: false
        var userId = sessionManager.getUserId()
        var userIdEdit = arguments?.getInt("userId")
        // Configurar el FloatingActionButton
        setupFab()

        // Observar los cambios en los datos del ViewModel
        observeViewModels()

        // Configurar TextWatchers.
        setupTextWatchers()

        // Configurar listeners
        setupListeners()

        // Obtener el ID del usuario y el modo de edición
//        arguments?.let {
//            userIdEdit = it.getInt("userId")
//            isEditMode = userIdEdit != null
//        }

        arguments?.let {
            isEditMode = it.getBoolean("isEditMode")
            userId = it.getInt("userId", -1) // -1 como valor por defecto si no se proporciona
        }

        // Configurar el título
        val titleTextView: TextView = view.findViewById(R.id.titleTextView)
        titleTextView.text = if (isEditMode) "Editar Usuario" else "Nuevo Usuario"

    }

    private fun cargarDatosUsuario(userId: Int?) {
        // Realizar una llamada a la API para obtener los datos del usuario
        // y completar los campos del formulario
    }

    private fun setupTextWatchers() {
        // TextWatcher para calcular horas trabajadas
        val horasTextWatcher = object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                //
            }
        }

        // Otros TextWatchers para los campos requeridos
        addTextWatcher(binding.legajoTextInputLayout, "Campo requerido")
        addTextWatcher(binding.emailTextInputLayout, "Campo requerido")
        addTextWatcher(binding.dniTextInputLayout, "Campo requerido")
        addTextWatcher(binding.nombreTextInputLayout, "Campo requerido")
        addTextWatcher(binding.apellidoTextInputLayout, "Campo requerido")
        addTextWatcher(binding.telefonoTextInputLayout, "Campo requerido")
        addTextWatcher(binding.passwordTextInputLayout, "Campo requerido")
        addTextWatcher(binding.password2TextInputLayout, "Campo requerido")
    }

    private fun setupListeners() {


        guardarButton.setOnClickListener {
            if (validarContrasenas()) {
                if (isEditMode) {
                    actualizarUsuario()
                } else {
                    guardarUsuario()
                }
            }
        }
    }

    private fun cargarDatosUsuario() {
        // Realizar una llamada a la API para obtener los datos del usuario
        // y completar los campos del formulario
    }

    private fun actualizarUsuario() {
        // Crear un objeto Usuario con los datos del formulario
        // Incluir el ID del usuario
        // Llamar al endpoint actualizarUsuario de la API
    }

    private fun setupFab() {
        // Obtener referencia al FAB y configurar su OnClickListener
        val fab: FloatingActionButton? = activity?.findViewById(R.id.fab)

        fab?.visibility = View.GONE
        fab?.setImageResource(R.drawable.ic_add)
        fab?.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
        fab?.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white)))

        fab?.setOnClickListener {
            limpiarFormulario()
            fab.visibility = View.GONE
        }

    }

    private fun limpiarFormulario() {
        legajoEditText.text?.clear()
        nuevoUsuarioIdTextView.text?.clear()
        emailEditText.text?.clear()
        dniEditText.text?.clear()
        nombreEditText.text?.clear()
        apellidoEditText.text?.clear()
        telefonoEditText.text?.clear()
        passwordEditText.text?.clear()
        password2EditText.text?.clear()

        habilitarFormulario()

    }

    private fun validarContrasenas(): Boolean {
        val password = passwordEditText.text.toString()
        val password2 = password2EditText.text.toString()
        return if (password != password2) {
            Toast.makeText(requireContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }

    private fun guardarUsuario() {
        // Extensión para convertir String a Editable
        fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)

        if (validarCampos()) {
            val usuario = Usuario(
                id = userId, // Si es modo edición, se usa el userId existente
                legajo = legajoEditText.text.toString(),
                email = emailEditText.text.toString(),
                dni = dniEditText.text.toString(),
                password = passwordEditText.text.toString(),
                nombre = nombreEditText.text.toString(),
                apellido = apellidoEditText.text.toString(),
                telefono = telefonoEditText.text.toString(),
                userCreated = sessionManager.getUserId(), // ID del usuario actual
                estadoId = 1 // 1 = activo
            )

            when (isEditMode) {
                true -> {
                    val newPassword = if (binding.passwordEditText.text?.isNotEmpty() == true) {
                        binding.passwordEditText.text.toString()
                    } else {
                        null // Si el campo de contraseña está vacío, no se actualiza la contraseña
                    }
                    viewModel.actualizarUsuario(usuario, newPassword) { success ->
                        if (success) {
                            deshabilitarFormulario()
                            val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
                            fab.visibility = View.VISIBLE
                            // Puedes navegar a otro fragmento o realizar otras acciones
                        } else {
                            Toast.makeText(requireContext(), "Error al actualizar el usuario", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                false -> viewModel.guardarNuevoUsuario(usuario) { success, nuevoId ->
                    if (success) {
                        deshabilitarFormulario()
                        val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
                        fab.visibility = View.VISIBLE

                        nuevoId?.let {
                            binding.nuevoUsuarioIdTextView.text = it.toString().toEditable()
                        }
                        // Puedes navegar a otro fragmento o realizar otras acciones
                    } else {
                        Toast.makeText(requireContext(), "Error al guardar el usuario", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun validarCampos(): Boolean {
        var camposValidos = true

        if (legajoEditText.text.isNullOrEmpty()) {
            binding.legajoTextInputLayout.error = "Campo requerido"
            binding.legajoTextInputLayout.isErrorEnabled = true
            camposValidos = false
        } else {
            binding.legajoTextInputLayout.isErrorEnabled = false
        }

        if (emailEditText.text.isNullOrEmpty()) {
            binding.emailTextInputLayout.error = "Campo requerido"
            binding.emailTextInputLayout.isErrorEnabled = true
            camposValidos = false
        } else {
            binding.emailTextInputLayout.isErrorEnabled = false
        }

        if (dniEditText.text.isNullOrEmpty()) {
            binding.dniTextInputLayout.error = "Campo requerido"
            binding.dniTextInputLayout.isErrorEnabled = true
            camposValidos = false
        } else {
            binding.dniTextInputLayout.isErrorEnabled = false
        }

        if (nombreEditText.text.isNullOrEmpty()) {
            binding.nombreTextInputLayout.error = "Campo requerido"
            binding.nombreTextInputLayout.isErrorEnabled = true
            camposValidos = false
        } else {
            binding.nombreTextInputLayout.isErrorEnabled = false
        }

        if (apellidoEditText.text.isNullOrEmpty()) {
            binding.apellidoTextInputLayout.error = "Campo requerido"
            binding.apellidoTextInputLayout.isErrorEnabled = true
            camposValidos = false
        } else {
            binding.apellidoTextInputLayout.isErrorEnabled = false
        }

        if (telefonoEditText.text.isNullOrEmpty()) {
            binding.telefonoTextInputLayout.error = "Campo requerido"
            binding.telefonoTextInputLayout.isErrorEnabled = true
            camposValidos = false
        } else {
            binding.telefonoTextInputLayout.isErrorEnabled = false
        }

        if (passwordEditText.text.isNullOrEmpty()) {
            binding.passwordTextInputLayout.error = "Campo requerido"
            binding.passwordTextInputLayout.isErrorEnabled = true
            camposValidos = false
        } else {
            binding.passwordTextInputLayout.isErrorEnabled = false
        }

        if (password2EditText.text.isNullOrEmpty()) {
            binding.password2TextInputLayout.error = "Campo requerido"
            binding.password2TextInputLayout.isErrorEnabled = true
            camposValidos = false
        } else {
            binding.password2TextInputLayout.isErrorEnabled = false
        }

        if (!camposValidos) {
            Toast.makeText(requireContext(), "Por favor, complete todos los campos requeridos", Toast.LENGTH_SHORT).show()
            // O puedes usar un AlertDialog para mostrar la advertencia
        }

        return camposValidos
    }

    private fun habilitarFormulario() {
        // Habilita los campos de edición
        legajoEditText.isEnabled = true
        emailEditText.isEnabled = true
        dniEditText.isEnabled = true
        nombreEditText.isEnabled = true
        apellidoEditText.isEnabled = true
        telefonoEditText.isEnabled = true
        passwordEditText.isEnabled = true
        password2EditText.isEnabled = true

        guardarButton.isEnabled = true
    }

    private fun deshabilitarFormulario() {
        // Deshabilita los campos de edición

        legajoEditText.isEnabled = false
        emailEditText.isEnabled = false
        dniEditText.isEnabled = false
        nombreEditText.isEnabled = false
        apellidoEditText.isEnabled = false
        telefonoEditText.isEnabled = false
        passwordEditText.isEnabled = false
        password2EditText.isEnabled = false

        // Deshabilitar el icono de la contraseña
        binding.passwordTextInputLayout.isEndIconVisible = false
        binding.password2TextInputLayout.isEndIconVisible = false

        // Deshabilita el botón
        guardarButton.isEnabled = false
    }

    private var progressDialog: ProgressDialogFragment? = null // Mostrar ProgressDialog

    private fun observeViewModels() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                if (progressDialog == null) {
                    progressDialog = ProgressDialogFragment()
                }
                if (!progressDialog!!.isAdded) {
                    progressDialog!!.show(childFragmentManager, "progress")
                }
            } else {
                progressDialog?.dismiss()
            }
        }

        // Observa el LiveData para mensajes
        viewModel.error.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { mensaje ->
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
            }
        }

        // Observa el LiveData para mensajes
        viewModel.mensaje.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { mensaje ->
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addTextWatcher(textInputLayout: TextInputLayout, errorMessage: String) {
        val editText = textInputLayout.editText
        editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Implementación vacía o tu código aquí
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Implementación vacía o tu código aquí
            }

            override fun afterTextChanged(s: Editable?) {
                if (textInputLayout.isErrorEnabled) {
                    if (s.isNullOrEmpty()) {
                        textInputLayout.error = errorMessage
                    } else {
                        textInputLayout.isErrorEnabled = false
                    }
                }
            }
        })
    }
}
