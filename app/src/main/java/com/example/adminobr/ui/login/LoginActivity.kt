package com.example.adminobr.ui.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
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
import com.example.adminobr.utils.AppUtils
import com.example.adminobr.utils.NetworkErrorCallback
import com.example.adminobr.utils.NetworkStatusHelper
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

/**
 * Módulo LoginActivity
 *
 * Esta actividad gestiona el flujo de inicio de sesión en la aplicación, verificando la conectividad de red,
 * recuperando datos almacenados de usuario y empresa, y permitiendo al usuario autenticarse con sus credenciales.
 * Incluye funcionalidades para manejar errores de red, validar campos de entrada y gestionar la persistencia de
 * datos con `SessionManager`.
 */

class LoginActivity : AppCompatActivity(), NetworkErrorCallback {

    // Servicio de API para realizar solicitudes de login
    private val apiService: ApiService by lazy { ApiUtils.getApiService() }

    // Helper para verificar el estado de la conexión de red
    private lateinit var networkHelper: NetworkStatusHelper

    // Administrador de autocompletado para cargar empresas
    private lateinit var autocompleteManager: AutocompleteManager

    // ViewModel para gestionar los datos de la aplicación
    private lateinit var appDataViewModel: AppDataViewModel

    // Binding para acceder a las vistas de la interfaz de usuario
    private lateinit var binding: ActivityLoginBinding

    // Layout para mostrar errores de conexión de red
    private lateinit var networkErrorLayout: View

    // Variable para almacenar la empresa seleccionada en el campo de autocompletado
    private var selectedEmpresa: Empresa? = null

    // Manager para la gestión de sesiones, carga solo cuando se accede a él
    private val sessionManager by lazy { SessionManager(this) }

    // Variables para verificar si los datos de usuario y empresa ya están cargados
    var isEmpresaPreloaded = false
    var isUserPreloaded = false

    /**
     * Método que se ejecuta al crear la actividad.
     * Se inicializan los elementos y se cargan los datos necesarios como el estado de la red,
     * la interfaz de usuario y los datos del usuario y la empresa.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa NetworkStatusHelper después de que la actividad ha sido creada
        networkHelper = NetworkStatusHelper(this)

//        if (networkHelper.isWifiConnected()) {
//            Snackbar.make(binding.root, "Conectado por Wi-Fi", Snackbar.LENGTH_SHORT).show()
//        } else {
//            Snackbar.make(binding.root, "Conectado por Datos Moviles", Snackbar.LENGTH_SHORT).show()
//        }

        // Inicializar networkErrorLayout
        networkErrorLayout = findViewById(R.id.networkErrorLayout)

        // Capturar el layout incluido
        val networkErrorLayout = findViewById<ConstraintLayout>(R.id.networkErrorLayout)

        // Capturar el botón retry_button dentro de networkErrorLayout
        val retryButton = networkErrorLayout.findViewById<Button>(R.id.retry_button)

        // Asegúrate de que binding se haya inicializado antes de acceder a networkErrorView
        networkErrorLayout.visibility = View.GONE // O View.VISIBLE si quieres que se muestre inicialmente

        retryButton.setOnClickListener {
            manageNetworkErrorLayout()
        }

        // Verificar si ya existen datos guardados del usuario
        val userDetails = sessionManager.getUserDetails()
        val empresa = sessionManager.getEmpresaData() // Obtener los datos de la empresa

        if (!userDetails["legajo"].isNullOrEmpty() && !userDetails["nombre"].isNullOrEmpty()) {
            isUserPreloaded = true
            // Mostrar mensaje de bienvenida
            binding.tvWelcomeMessage.text = "¡Hola ${userDetails["nombre"]?.uppercase(Locale.ROOT)}!"
            binding.tvWelcomeMessage.visibility = View.VISIBLE

            // Mostrar el enlace de "No soy yo"
            binding.tvNotMeLink.visibility = View.VISIBLE

            // Ocultar los campos de empresa y usuario
            binding.empresaTextInputLayout.visibility = View.GONE
            binding.usuarioTextInputLayout.visibility = View.GONE

            // Si los datos de la empresa existen, mostrar el nombre de la empresa en el campo de autocomplete
            if (empresa != null && empresa.nombre.isNotEmpty() && !empresa.db_name.isNullOrEmpty()) {
                isEmpresaPreloaded = true
                Log.d("LoginActivity", "Empresa recuperada: ${empresa.nombre}")
                binding.empresaAutocomplete.setText(empresa.nombre) // Asignar el nombre de la empresa
            } else {
                Log.d("LoginActivity", "No se encontraron datos de empresa en SharedPreferences.")
                Toast.makeText(this, "No se encontraron datos de empresa en SharedPreferences.", Toast.LENGTH_SHORT).show()
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

        binding.tvNotMeLink.setOnClickListener {
            // Limpiar el campo de empresa
            binding.empresaAutocomplete.setText("")
            // Limpiar el campo de usuario
            binding.usuarioEditText.setText("")

            // Borrar los datos del usuario almacenados en SessionManager
            sessionManager.clearUserDetails()

            // Mostrar los campos de empresa y usuario nuevamente
            binding.empresaTextInputLayout.visibility = View.VISIBLE
            binding.usuarioTextInputLayout.visibility = View.VISIBLE

            // Ocultar el mensaje de bienvenida y el enlace de "No soy yo"
            binding.tvWelcomeMessage.visibility = View.GONE
            binding.tvNotMeLink.visibility = View.GONE

            // Aquí puedes restablecer cualquier otro estado necesario
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
            // Verificar si hay conexión a internet
            if (!networkHelper.isNetworkAvailable()) {
                Toast.makeText(this, "No hay conexión a internet", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val usuario = binding.usuarioEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            // Cerrar el teclado usando AppUtils
            AppUtils.closeKeyboard(this)

            // Usar la empresa almacenada en SharedPreferences si existe
            val empresa = sessionManager.getEmpresaData()

            if (validarCampos()) {

                val progressDialog = ProgressDialogFragment.show(supportFragmentManager)
                lifecycleScope.launch {
                    try {

                        // Usar la empresa guardada si está disponible
                        val dbName = empresa?.db_name ?: selectedEmpresa?.db_name

                        if (dbName.isNullOrEmpty()) {
                            Log.e("LoginActivity", "DB Name es nulo o vacío")
                            Toast.makeText(this@LoginActivity, "DB Name es nulo o vacío", Toast.LENGTH_SHORT).show()

                            return@launch
                        }

                        val loginRequest = LoginRequest(usuario, password, dbName)
                        val requestBody = Gson().toJson(loginRequest)
                            .toRequestBody("application/json".toMediaTypeOrNull())

                        Log.d("LoginActivity", "Datos a enviar: Usuario = $usuario, Password = $password, DB Name = $dbName")
                        Log.d("LoginActivity", "Enviando datos al servidor: $requestBody")

                        // Realizar la solicitud de login
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
                // Mostrar un mensaje de error si los campos no son válidos
                Toast.makeText(this, "Por favor, complete todos los campos requeridos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Método que se ejecuta al iniciar la actividad, registrando los callbacks de red.
     */
    override fun onStart() {
        super.onStart()
        networkHelper.registerNetworkCallback()  // Registrar cuando la actividad esté visible
        // Verificar el estado de la red, para mostrar el layout de errores
        manageNetworkErrorLayout()
    }

    /**
     * Método que se ejecuta al detener la actividad, desregistrando los callbacks de red.
     */
    override fun onStop() {
        super.onStop()
        networkHelper.unregisterNetworkCallback()  // Desregistrar cuando la actividad deje de ser visible
    }

    /**
     * Método que se ejecuta al destruir la actividad. Desregistra los callbacks para evitar fugas de memoria.
     */
    override fun onDestroy() {
        super.onDestroy()
        // Unregister the network callback to prevent leaks
        networkHelper.unregisterNetworkCallback()
    }

    /**
     * Función que gestiona el layout de errores de red y recarga componentes si la red está disponible.
     */
    override fun manageNetworkErrorLayout() {
        if (networkHelper.isNetworkAvailable()) {
            networkErrorLayout.visibility = View.GONE
            reloadComponents() // Recargar componentes que dependen de la red
            binding.loginButton.isEnabled = true
        } else {
            // Verificar si el layout de error está visible
            if (networkErrorLayout.visibility == View.VISIBLE) {
                // Cambiar el texto del TextView en el layout de error
                val textViewError = networkErrorLayout.findViewById<TextView>(R.id.textViewError)
                textViewError?.text = "Aún no hay conexión a internet" // Cambiar el texto
            }

            networkErrorLayout.visibility = View.VISIBLE
            binding.loginButton.isEnabled = false
        }
    }

    /**
     * Función que recarga los componentes que dependen de la conexión de red.
     */
    private fun reloadComponents() {
        // Si hay conexión, recargar las empresas en el AutoCompleteTextView
        autocompleteManager.loadEmpresas(binding.empresaAutocomplete, this) { empresa ->
            selectedEmpresa = empresa
        }
    }

    /**
     * Función que agrega un TextWatcher a un campo de entrada para validar su contenido.
     */
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

    /**
     * Función que convierte el texto ingresado en un AutoCompleteTextView a mayúsculas.
     */
    private fun setEditTextToUppercase(editText: AutoCompleteTextView) {
        editText.filters = arrayOf(InputFilter.AllCaps())
    }

    /**
     * Función que maneja la respuesta de la solicitud de login.
     */
    private fun handleLoginResponse(response: Response<LoginResponse>) {
        if (response.isSuccessful) {
            val loginResponse = response.body()
            val empresa = selectedEmpresa ?: sessionManager.getEmpresaData()

            if (loginResponse?.success == true && empresa != null) {
                // Guardar la empresa seleccionada
                sessionManager.saveEmpresaData(empresa)

                // Guardar los datos del usuario en SharedPreferences
                loginResponse.user.let { user ->
                    sessionManager.saveUserDetails(
                        user.id ?: -1,
                        user.legajo,
                        user.nombre,
                        user.apellido,
                        user.roles,
                        user.principalRole
                    )
                    Log.d("LoginActivity", "Datos de usuario guardados en SharedPreferences.")
                }

                // Iniciar la actividad principal
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                val errorMessage = loginResponse?.message ?: "Error de autenticación desconocido"
                Log.e("LoginActivity", "Error de autenticación: $errorMessage")
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        } else {
            val errorMessage = parseErrorMessage(response.errorBody()?.string())
            Log.e("LoginActivity", "Error en la respuesta del servidor: ${response.code()} - $errorMessage")
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Función que parsea el mensaje de error del servidor.
     */
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

    /**
     * Función para validar los campos de entrada del formulario de login.
     */
    private fun validarCampos(): Boolean {
        var camposValidos = true

        if (!isUserPreloaded && binding.usuarioEditText.text.isNullOrEmpty()) {
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

        if (!isEmpresaPreloaded && binding.empresaAutocomplete.text.isEmpty()) {
            binding.empresaTextInputLayout.error = "Campo requerido"
            binding.empresaTextInputLayout.isErrorEnabled = true
            camposValidos = false
        } else if (selectedEmpresa == null && !isEmpresaPreloaded) {
            val empresaName = binding.empresaAutocomplete.text.toString()
            selectedEmpresa = autocompleteManager.getEmpresaByName(empresaName)

            if (selectedEmpresa == null) {
                binding.empresaTextInputLayout.error = "Seleccione una empresa válida"
                camposValidos = false
            } else {
                binding.empresaTextInputLayout.isErrorEnabled = false
            }
        }

        if (!camposValidos) {
            Toast.makeText(this, "Por favor, complete todos los campos requeridos", Toast.LENGTH_SHORT).show()
        }

        return camposValidos
    }
}