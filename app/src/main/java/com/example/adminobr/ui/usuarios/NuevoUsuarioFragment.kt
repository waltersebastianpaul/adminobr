package com.example.adminobr.ui.usuarios

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.adminobr.R
import com.example.adminobr.data.Usuario
import com.example.adminobr.databinding.FragmentNuevoUsuarioBinding
import com.example.adminobr.ui.common.ProgressDialogFragment
import com.example.adminobr.utils.SessionManager
import com.example.adminobr.viewmodel.NuevoUsuarioViewModel
import com.example.adminobr.viewmodel.NuevoUsuarioViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout

class NuevoUsuarioFragment : Fragment() {
    private val viewModel: NuevoUsuarioViewModel by viewModels {
        NuevoUsuarioViewModelFactory(requireActivity().application) // Se pasa la aplicación
    }

    private var _binding: FragmentNuevoUsuarioBinding? = null
    private val binding get() = _binding!!

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
        _binding = FragmentNuevoUsuarioBinding.inflate(inflater, container, false)

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

        // Configurar el FloatingActionButton
        setupFab()

        // Observar los cambios en los datos del ViewModel
        observeViewModels()

        // Configurar TextWatchers.
        setupTextWatchers()

        // Configurar listeners
        setupListeners()

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
                guardarNuevoUsuario()
            }
        }
    }

    private fun setupFab() {
        // Obtener referencia al FAB y configurar su OnClickListener
        val fab: FloatingActionButton? = activity?.findViewById(R.id.fab)
        fab?.setOnClickListener {
            limpiarFormulario()
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

    private fun guardarNuevoUsuario() {
        // Extensión para convertir String a Editable
        fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)


        //val userId = requireActivity().intent.getIntExtra("id", -1)
        val userId = sessionManager.getUserId()
        if (validarCampos()) {

            val nuevoUsuario = Usuario(
                id = null,
                legajo = legajoEditText.text.toString(),
                email = emailEditText.text.toString(),
                dni = dniEditText.text.toString(),
                password = passwordEditText.text.toString(),
                nombre = nombreEditText.text.toString(),
                apellido = apellidoEditText.text.toString(),
                telefono = telefonoEditText.text.toString(),
                userCreated = userId, // Reemplazar con el ID del user actual
                estadoId = 1 // 1 = activo
            )

            viewModel.guardarNuevoUsuario(nuevoUsuario) { success, nuevoId ->
                if (success) {
                    deshabilitarFormulario()
                    val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
                    fab.visibility = View.VISIBLE

                    nuevoId?.let {
                        binding.nuevoUsuarioIdTextView.text = it.toString().toEditable()
                    }

                } else {
                    Toast.makeText(
                        requireContext(),
                        "Error al guardar el parte diario",
                        Toast.LENGTH_SHORT
                    ).show()
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
        // Deshabilita el botón
        guardarButton.isEnabled = false
    }

    private fun cerrarTeclado() {
        // Cerrar el teclado
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
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
