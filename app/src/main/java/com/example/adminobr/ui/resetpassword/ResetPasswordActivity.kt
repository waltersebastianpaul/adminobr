package com.example.adminobr.ui.resetpassword

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.adminobr.R
import com.example.adminobr.data.ResultData
import com.example.adminobr.databinding.ActivityResetPasswordBinding
import com.example.adminobr.repository.ResetPasswordRepository
import com.example.adminobr.utils.AppUtils
import com.example.adminobr.utils.NetworkStatusHelper
import com.example.adminobr.utils.SessionManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResetPasswordBinding
    private val sessionManager by lazy { SessionManager(this) }
    private var empresaDbName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
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

        empresaDbName = sessionManager.getEmpresaData()["empresaDbName"]
    }

    private fun setupListeners() {
        binding.sendCodeButton.setOnClickListener {
            AppUtils.closeKeyboard(this)
            if (NetworkStatusHelper.isConnected()) {
                if (validarEmail()) {
                    val email = binding.emailEditText.text.toString().trim()
                    sendRecoveryCode(email)
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

    private fun validarEmail(): Boolean {
        val email = binding.emailEditText.text.toString().trim()
        return when {
            email.isEmpty() -> {
                binding.emailTextInputLayout.error = "Email requerido"
                false
            }
            !email.isValidEmail() -> {
                binding.emailTextInputLayout.error = "Email inválido"
                false
            }
            else -> {
                binding.emailTextInputLayout.isErrorEnabled = false
                true
            }
        }
    }

    private fun setupTextWatchers() {
        addTextWatcher(binding.emailTextInputLayout, "Email requerido")
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

    private fun sendRecoveryCode(email: String) {
        lifecycleScope.launch {
            try {
                val result = ResetPasswordRepository.sendRecoveryCode(email, empresaDbName!!)
                when (result) {
                    is ResultData.Success -> {
//                        Snackbar.make(binding.root, "Código enviado exitosamente.", Snackbar.LENGTH_SHORT).show()
                        val intent = Intent(this@ResetPasswordActivity, VerifyCodeActivity::class.java)
                        intent.putExtra("email", email)
                        startActivity(intent)
                    }
                    is ResultData.Error -> {
                        // Aquí personalizas el mensaje de error devuelto por el servidor
                        Snackbar.make(binding.root, result.message ?: "Error desconocido", Snackbar.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.localizedMessage}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

}

fun String.isValidEmail(): Boolean = android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
