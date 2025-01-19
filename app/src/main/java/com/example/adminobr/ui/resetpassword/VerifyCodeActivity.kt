package com.example.adminobr.ui.resetpassword

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.adminobr.R
import com.example.adminobr.data.ResultData
import com.example.adminobr.databinding.ActivityVerifyCodeBinding
import com.example.adminobr.repository.ResetPasswordRepository
import com.example.adminobr.ui.login.LoginActivity
import com.example.adminobr.utils.AppUtils
import com.example.adminobr.utils.NetworkStatusHelper
import com.example.adminobr.utils.SessionManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class VerifyCodeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerifyCodeBinding
    private var email: String? = null
    private val sessionManager by lazy { SessionManager(this) }
    private var empresaDbName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyCodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTextWatchers()
        setupListeners()

        // Configurar Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Muestra el ícono predeterminado de retroceso
        supportActionBar?.title = null // Si no deseas un título en el Toolbar

        // Configurar acción del botón de retroceso
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed() // Manejar retroceso
        }

        email = intent.getStringExtra("email")
        empresaDbName = sessionManager.getEmpresaData()["empresaDbName"]

    }

    private fun setupListeners() {
        binding.verifyCodeButton.setOnClickListener {
            AppUtils.closeKeyboard(this)
            if (NetworkStatusHelper.isConnected()) {
                if (validarCodigo()) {
                    val code = binding.codeEditText.text.toString().trim()
                    verifyCode(code)
                }
            } else {
                Snackbar.make(binding.root, "No hay conexión a internet, intenta mas tardes.", Snackbar.LENGTH_LONG)
//                    .setBackgroundTint(ContextCompat.getColor(this, R.color.colorDanger))
                    .setTextColor(ContextCompat.getColor(this, R.color.danger_400))
                    .show()
                return@setOnClickListener
            }
        }

        binding.changePasswordButton.setOnClickListener {
            AppUtils.closeKeyboard(this)
            if (NetworkStatusHelper.isConnected()) {
                if (validarNuevaContrasena()) {
                    val newPassword = binding.newPasswordEditText.text.toString()
                    changePassword(newPassword)
                }
            } else {
                Snackbar.make(binding.root, "No hay conexión a internet, intenta mas tardes.", Snackbar.LENGTH_LONG)
//                    .setBackgroundTint(ContextCompat.getColor(this, R.color.colorDanger))
                    .setTextColor(ContextCompat.getColor(this, R.color.danger_400))
                    .show()
                return@setOnClickListener
            }
        }
    }

    private fun validarCodigo(): Boolean {
        val code = binding.codeEditText.text.toString().trim()
        return if (code.isEmpty()) {
            binding.codeTextInputLayout.error = "Ingresa el código recibido"
            false
        } else {
            binding.codeTextInputLayout.isErrorEnabled = false
            true
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun verifyCode(code: String) {
        lifecycleScope.launch {
            val result = ResetPasswordRepository.verifyCode(email.orEmpty(), code, empresaDbName.orEmpty())
            when (result) {
                is ResultData.Success -> {
                    Snackbar.make(binding.root, "Código verificado correctamente.", Snackbar.LENGTH_SHORT).show()
                    showPasswordFields()
                }
                is ResultData.Error -> {
                    Snackbar.make(binding.root, "Error: ${result.message}", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun changePassword(newPassword: String) {
        lifecycleScope.launch {
            val result = ResetPasswordRepository.changePassword(email.orEmpty(), newPassword, empresaDbName.orEmpty())
            when (result) {
                is ResultData.Success -> {
                    Snackbar.make(binding.root, "Contraseña cambiada exitosamente.", Snackbar.LENGTH_SHORT).show()

                    // Redirigir al LoginActivity
                    val intent = Intent(this@VerifyCodeActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)

                }
                is ResultData.Error -> {
                    Snackbar.make(binding.root, "Error: ${result.message}", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showPasswordFields() {
        binding.newPasswordTextInputLayout.visibility = View.VISIBLE
        binding.confirmPasswordTextInputLayout.visibility = View.VISIBLE
        binding.changePasswordButton.visibility = View.VISIBLE

        binding.codeTextInputLayout.visibility = View.GONE
        binding.verifyCodeButton.visibility = View.GONE
    }

    private fun validarNuevaContrasena(): Boolean {
        val newPassword = binding.newPasswordEditText.text.toString()
        val confirmPassword = binding.confirmPasswordEditText.text.toString()

        var isValid = true

        // Función auxiliar para validar un campo y establecer el error
        fun validateField(text: String?, textInputLayout: com.google.android.material.textfield.TextInputLayout, errorMessage: String): Boolean {
            return if (text.isNullOrEmpty()) {
                textInputLayout.error = errorMessage
                false
            } else {
                textInputLayout.isErrorEnabled = false
                true
            }
        }

        // Validar campos individuales
        val isNewPasswordValid = validateField(newPassword, binding.newPasswordTextInputLayout, "Campo requerido")
        val isConfirmPasswordValid = validateField(confirmPassword, binding.confirmPasswordTextInputLayout, "Campo requerido")

        isValid = isValid && isNewPasswordValid && isConfirmPasswordValid

        // Validar que las contraseñas coincidan si ambos campos están llenos
        if (newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
            binding.newPasswordTextInputLayout.error = "Las contraseñas no coinciden"
            binding.confirmPasswordTextInputLayout.error = "Las contraseñas no coinciden"
            isValid = false
        } else if (newPassword.isNotEmpty() && confirmPassword.isNotEmpty()) {
            binding.newPasswordTextInputLayout.isErrorEnabled = false
            binding.confirmPasswordTextInputLayout.isErrorEnabled = false
        }

        return isValid
    }

    private fun addTextWatcher(textInputLayout: com.google.android.material.textfield.TextInputLayout, errorMessage: String) {
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

    private fun setupTextWatchers() {
        addTextWatcher(binding.newPasswordTextInputLayout, "Campo requerido")
        addTextWatcher(binding.confirmPasswordTextInputLayout, "Campo requerido")
        addTextWatcher(binding.codeTextInputLayout, "Ingresa el código recibido")
    }

}
