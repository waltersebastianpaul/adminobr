package com.example.adminobr.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.adminobr.utils.AutocompleteManager
import com.example.adminobr.utils.SessionManager
import com.example.adminobr.MainActivity
import com.example.adminobr.api.ApiService
import com.example.adminobr.databinding.ActivityLoginBinding
import com.example.adminobr.api.ApiUtils
import com.example.adminobr.data.Empresa
import com.example.adminobr.data.ErrorResponse
import com.example.adminobr.data.LoginRequest
import com.example.adminobr.data.LoginResponse
import com.example.adminobr.ui.common.ProgressDialogFragment
import com.example.adminobr.utils.AppUtils
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
import java.util.Locale

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

        // Verificar si ya existen datos guardados del usuario
        val userDetails = sessionManager.getUserDetails()
        val empresa = sessionManager.getEmpresaData() // Obtener los datos de la empresa

        if (!userDetails["legajo"].isNullOrEmpty() && !userDetails["nombre"].isNullOrEmpty()) {
            // Log para verificar si encontramos datos en SharedPreferences
            Log.d("LoginActivity", "Datos de usuario encontrados en SharedPreferences:")
            Log.d("LoginActivity", "Legajo: ${userDetails["legajo"]}")
            Log.d("LoginActivity", "Nombre: ${userDetails["nombre"]}")
            Log.d("LoginActivity", "Apellido: ${userDetails["apellido"]}")
            Log.d("LoginActivity", "Roles: ${userDetails["roles"]}")

            // Mostrar mensaje de bienvenida
            binding.tvWelcomeMessage.text = "¡Hola ${userDetails["nombre"]?.uppercase(Locale.ROOT)}!"
//            binding.tvWelcomeMessage.text = "¡Hola ${userDetails["nombre"]?.uppercase(Locale.ROOT)} ${userDetails["apellido"]?.uppercase(Locale.ROOT)}!"
            binding.tvWelcomeMessage.visibility = View.VISIBLE

            // Mostrar el enlace de "No soy yo"
            binding.tvNotMeLink.visibility = View.VISIBLE

            // Ocultar los campos de empresa y usuario
            binding.empresaTextInputLayout.visibility = View.GONE
            binding.usuarioTextInputLayout.visibility = View.GONE

            // Si los datos de la empresa existen, mostrar el nombre de la empresa en el campo de autocomplete
            if (empresa != null && empresa.nombre.isNotEmpty()) {
                Log.d("LoginActivity", "Empresa recuperada: ${empresa.nombre}")
                binding.empresaAutocomplete.setText(empresa.nombre) // Asignar el nombre de la empresa
            }

            // Asignar el legajo al campo de usuario
            val legajo = userDetails["legajo"]
            if (!legajo.isNullOrEmpty()) {
                Log.d("LoginActivity", "Legajo recuperado: $legajo")
                binding.usuarioEditText.setText(legajo)  // Asignar el legajo al campo de usuario
            }
        } else {
            // Si no hay datos, mostrar los campos de login
            Log.d("LoginActivity", "No se encontraron datos de usuario en SharedPreferences.")
        }

        // Configurar el enlace de "No soy yo"
        binding.tvNotMeLink.setOnClickListener {
            binding.empresaAutocomplete.setText("") // Limpiar el campo de empresa
            binding.usuarioEditText.setText("")  // Limpiar el campo de usuario

            sessionManager.clearUserDetails() // Borrar los datos del usuario
            recreate() // Recargar la actividad para mostrar los campos de login nuevamente
        }

        // Asignar valores por defecto en modo Debug
        val isDebuggable = false // BuildConfig.DEBUG

        // Guardar el valor de isDebuggable en SessionManager
        sessionManager.saveDebuggable(isDebuggable)
        Log.d("LoginActivity", "Debuggable: $isDebuggable")


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

        // Configura el OnItemClickListener para equipoAutocomplete
//        binding.empresaAutocomplete.setOnItemClickListener { _, _, _, _ ->
//            // Si se selecciona un equipo, quita el foco del AutoCompleteTextView
//            binding.empresaAutocomplete.clearFocus()
//        }

        // Llamar a la función para convertir el texto a mayúsculas
        setEditTextToUppercase(binding.empresaAutocomplete)

        addTextWatcher(binding.empresaTextInputLayout, "Campo requerido")
        addTextWatcher(binding.usuarioTextInputLayout, "Campo requerido")
        addTextWatcher(binding.passwordTextInputLayout, "Campo requerido")

        binding.passwordEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.loginButton.performClick() // Simula un clic en el botón de login
                true // Indica que la acción se ha manejado
            } else {
                false // Indica que la acción no se ha manejado
            }
        }

        // Listener para el botón de login
        binding.loginButton.setOnClickListener {
            val usuario = binding.usuarioEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            // Cerrar el teclado usando AppUtils
            AppUtils.closeKeyboard(this, currentFocus)

            if (validarCampos()) {

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
                if (editText == binding.empresaAutocomplete) {
                    selectedEmpresa = null // Limpiar selectedEmpresa solo si es el AutoCompleteTextView de empresas
                }

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

    private fun handleLoginResponse(response: Response<LoginResponse>) {
        if (response.isSuccessful) {
            val loginResponse = response.body()


            Log.d("LoginActivity", "loginResponse.user: ${loginResponse?.user}")
            Log.d("LoginActivity", "Roles: ${loginResponse?.user?.roles?.joinToString()}")
            Log.d("LoginActivity", "Permisos: ${loginResponse?.user?.permisos?.joinToString()}")

            if (loginResponse != null && loginResponse.success) {
                Log.d("LoginActivity", "Inicio de sesión exitoso")

                // Guardar la empresa seleccionada
                sessionManager.saveEmpresaData(selectedEmpresa!!)

                // Guardar los datos del usuario en SharedPreferences
                sessionManager.saveUserDetails(
                    loginResponse.user.id ?: -1, // Usar -1 como valor predeterminado si es nulo
                    loginResponse.user.legajo,
                    loginResponse.user.nombre,
                    loginResponse.user.apellido,
                    loginResponse.user.roles,
                    loginResponse.user.principalRole
                )

                // Log después de guardar los datos
                Log.d("LoginActivity", "Datos de usuario guardados en SharedPreferences:")
                Log.d("LoginActivity", "Legajo: ${loginResponse.user.legajo}")
                Log.d("LoginActivity", "Nombre: ${loginResponse.user.nombre}")
                Log.d("LoginActivity", "Apellido: ${loginResponse.user.apellido}")
                Log.d("LoginActivity", "Roles: ${loginResponse.user.roles}")

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

        if (binding.empresaAutocomplete.text.isNotEmpty()) {
            if (selectedEmpresa == null) {
                val empresaName = binding.empresaAutocomplete.text.toString()
                selectedEmpresa = autocompleteManager.getEmpresaByName(empresaName)
                if (selectedEmpresa == null) {
                    binding.empresaTextInputLayout.error = "Seleccione una empresa válida"
                    binding.empresaTextInputLayout.isErrorEnabled = true
                    camposValidos = false
                } else {
                    binding.empresaTextInputLayout.isErrorEnabled = false
                }
            }
        } else {
            binding.empresaTextInputLayout.error = "Campo requerido"
            binding.empresaTextInputLayout.isErrorEnabled = true
            camposValidos = false
        }

        if (!camposValidos) {
            Toast.makeText(this, "Por favor, complete todos los campos requeridos", Toast.LENGTH_SHORT).show()
        }

        return camposValidos
    }
}