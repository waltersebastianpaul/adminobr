package com.example.adminobr.ui.login

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.adminobr.utils.AutocompleteManager
import com.example.adminobr.utils.SessionManager
import com.example.adminobr.MainActivity
import com.example.adminobr.R
import com.example.adminobr.api.ApiService
import com.example.adminobr.databinding.ActivityLoginBinding
import com.example.adminobr.api.ApiUtils
import com.example.adminobr.data.Empresa
import com.example.adminobr.data.ErrorResponse
import com.example.adminobr.data.LoginRequest
import com.example.adminobr.data.LoginResponse
import com.example.adminobr.ui.common.ProgressDialogFragment
import com.example.adminobr.viewmodel.AppDataViewModel
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class LoginActivity : AppCompatActivity() {

    private val apiService: ApiService by lazy { ApiUtils.getApiService() }

    // Declaración de variables para cargar los datos
    private lateinit var autocompleteManager: AutocompleteManager
    private lateinit var appDataViewModel: AppDataViewModel
    private lateinit var binding: ActivityLoginBinding

    // Variable para almacenar la empresa seleccionada
    private var selectedEmpresa: Empresa? = null

    // Instancia de SessionManager solo cuando se acceda a ella
    private val sessionManager by lazy { SessionManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Asignar valores por defecto en modo Debug
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        // Guardar el valor de isDebuggable en SessionManager
        sessionManager.saveDebuggable(isDebuggable)
        Log.d("LoginActivity", "Debuggable: $isDebuggable")

        if (isDebuggable) {
            binding.usuarioEditText.setText("0001")
            binding.passwordEditText.setText("123456")
        }

        // Cambia el color de la "O" de AdminObr a otro color
        val text = SpannableString("AdminObr")
        val oColor = ContextCompat.getColor(this, R.color.colorLogo)
        val oSpan = ForegroundColorSpan(oColor)
        text.setSpan(oSpan, 5, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.tituloLogoTextView.text = text

        // Obtener el ViewModel
        appDataViewModel = ViewModelProvider(this).get(AppDataViewModel::class.java)

        // Crear una instancia de AutocompleteManager
        autocompleteManager = AutocompleteManager(this, appDataViewModel)

        // Cargar empresas y capturar el objeto Empresa seleccionado
        autocompleteManager.loadEmpresas(
            binding.empresaAutocomplete,
            this
        ) { empresa ->
            Log.d("LoginActivity", "Empresa selecionada: $empresa")

            selectedEmpresa = empresa // Guardar empresa seleccionada
        }

        // Llamar a la función para convertir el texto a mayúsculas
        setEditTextToUppercase(binding.empresaAutocomplete)

        addTextWatcher(binding.empresaTextInputLayout, "Campo requerido")

        // Listener para el botón de login
        binding.loginButton.setOnClickListener {
            val usuario = binding.usuarioEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (validarCampos() && selectedEmpresa != null) {

                cerrarTeclado()

                val progressDialog = ProgressDialogFragment.show(supportFragmentManager)
                lifecycleScope.launch {
                    try {
                        Log.e("LoginActivity", "DB Name: ${selectedEmpresa?.db_name}")

                        val loginRequest = LoginRequest(usuario, password, selectedEmpresa!!.db_name)
                        val requestBody = Gson().toJson(loginRequest)
                            .toRequestBody("application/json".toMediaTypeOrNull())
                        val response = apiService.login(requestBody)

                        Log.d("LoginActivity", "Enviando datos al servidor: $requestBody")

                        handleLoginResponse(response)

                    } catch (e: HttpException) {
                        val errorBody = e.response()?.errorBody()?.string()
                        val errorMessage = parseErrorMessage(errorBody)
                        Log.e("LoginActivity", "Error HTTP: ${e.code()} - $errorMessage")
                        Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    } catch (e: UnknownHostException) {
                        Log.e("LoginActivity", "Error de conexión a internet: ${e.message}")
                        Toast.makeText(
                            this@LoginActivity,
                            "No hay conexión a internet",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: SocketTimeoutException) {
                        Log.e("LoginActivity", "Timeout de la petición: ${e.message}")
                        Toast.makeText(
                            this@LoginActivity,
                            "Timeout de la petición",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Log.e("LoginActivity", "Error en la autenticación: ${e.message}", e)
                        Toast.makeText(
                            this@LoginActivity,
                            "Error en la autenticación",
                            Toast.LENGTH_SHORT
                        ).show()
                    } finally {
                        progressDialog.dismiss()
                    }
                }
            } else {
                Toast.makeText(this, "Seleccione una empresa válida", Toast.LENGTH_SHORT).show()
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

    private fun setEditTextToUppercase(editText: AutoCompleteTextView) {
        editText.filters = arrayOf(InputFilter.AllCaps())
    }

    private fun cerrarTeclado() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    private fun handleLoginResponse(response: Response<LoginResponse>) {
        if (response.isSuccessful) {
            val loginResponse = response.body()
            Log.d("LoginActivity", "Respuesta del servidor: $loginResponse")

            if (loginResponse != null && loginResponse.success) {
                Log.d("LoginActivity", "Inicio de sesión exitoso")

                // Guardar la empresa seleccionada
                sessionManager.saveEmpresaData(selectedEmpresa!!)

                // Guardar los datos del usuario
                sessionManager.saveUserData(
                    loginResponse.user.id ?: -1,
                    loginResponse.user.nombre,
                    loginResponse.user.apellido,
                    loginResponse.user.email,
                    loginResponse.user.rol ?: emptyList(),
                    loginResponse.user.permisos ?: emptyList()
                )

                // Iniciar la actividad principal
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                val errorMessage = loginResponse?.message ?: "Error de autenticación desconocido"
                Log.e("LoginActivity", "Error de autenticación: $errorMessage")
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        } else {
            val errorBody = response.errorBody()?.string()
            val errorMessage = parseErrorMessage(errorBody)
            Log.e("LoginActivity", "Error en la respuesta del servidor: ${response.code()} - $errorMessage")
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }
//    private fun handleLoginResponse(response: Response<LoginResponse>) {
//         if (response.isSuccessful) {
//             val loginResponse = response.body()
//             Log.d("LoginActivity", "Respuesta del servidor: $loginResponse")
//
//             if (loginResponse != null && loginResponse.success) {
//                 Log.d("LoginActivity", "Inicio de sesión exitoso")
//
//                 // Guardar empresaDbName en SessionManager
//                 val sessionManager = SessionManager(this)
//                 sessionManager.saveEmpresaDbName(empresaDbName!!)
//
//                 // Inicia la actividad principal
//                 val intent = Intent(this, MainActivity::class.java).apply {
//                     putExtra("id", loginResponse.user.id)
//                     putExtra("nombre", loginResponse.user.nombre)
//                     putExtra("apellido", loginResponse.user.apellido)
//                     putExtra("email", loginResponse.user.email)
//                     putStringArrayListExtra("rol", ArrayList(loginResponse.user.rol))
//                 }
//                 startActivity(intent)
//                 finish()
//             } else {
//                 val errorMessage = loginResponse?.message ?: "Error de autenticación desconocido"
//                 Log.e("LoginActivity", "Error de autenticación: $errorMessage")
//                 Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
//             }
//         } else {
//             val errorBody = response.errorBody()?.string()
//             val errorMessage = when (response.code()) {
//                 400 -> parseErrorMessage(errorBody)
//                 401 -> "User o contraseña incorrectos"
//                 404 -> "Página no encontrada"
//                 500 -> "Error interno del servidor"
//                 else -> "Error en la respuesta del servidor: ${response.code()}"
//             }
//             Log.e("LoginActivity", "Error en la respuesta del servidor: ${response.code()} - $errorMessage")
//             Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
//         }
//     }

    private fun parseErrorMessage(errorBody: String?): String {
        return if (errorBody.isNullOrEmpty()) {
            "Error en la respuesta del servidor"
        } else {
            try {
                val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                errorResponse.message ?: "Error en la respuesta del servidor"
            } catch (e: Exception) {
                "Error en la respuesta del servidor"
            }
        }
    }

    private fun validarCampos(): Boolean {
        var camposValidos = true

        if (binding.usuarioEditText.text.isNullOrEmpty()) {
            binding.usuarioTextInputLayout.error = "Campo requerido"
            binding.usuarioTextInputLayout.isErrorEnabled = true
            camposValidos = false
        } else {
            binding.usuarioTextInputLayout.isErrorEnabled = false
        }

        if (binding.passwordEditText.text.isNullOrEmpty()) {
            binding.passwordTextInputLayout.error = "Campo requerido"
            binding.passwordTextInputLayout.isErrorEnabled = true
            camposValidos = false
        } else {
            binding.passwordTextInputLayout.isErrorEnabled = false
        }

        if (binding.empresaAutocomplete.text.isNullOrEmpty()) {
            binding.empresaTextInputLayout.error = "Campo requerido"
            binding.empresaTextInputLayout.isErrorEnabled = true
            camposValidos = false
        } else {
            binding.empresaTextInputLayout.isErrorEnabled = false
        }

        if (!camposValidos) {
            Toast.makeText(this, "Por favor, complete todos los campos requeridos", Toast.LENGTH_SHORT).show()
        }

        return camposValidos
    }
}