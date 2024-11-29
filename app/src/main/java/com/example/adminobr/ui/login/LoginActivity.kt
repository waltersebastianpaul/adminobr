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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import android.graphics.Rect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.adminobr.utils.Constants
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

/**
 * Módulo LoginActivity
 *
 * Esta actividad gestiona el flujo de inicio de sesión en la aplicación, verificando la conectividad de red,
 * recuperando datos almacenados de usuario y empresa, y permitiendo al usuario autenticarse con sus credenciales.
 * Incluye funcionalidades para manejar errores de red, validar campos de entrada y gestionar la persistencia de
 * datos con `SessionManager`.
 */

class LoginActivity : AppCompatActivity() {

    // Servicio de API para realizar solicitudes de login
    private val apiService: ApiService by lazy { ApiUtils.getApiService() }

    // Administrador de autocompletado para cargar empresas
    private lateinit var autocompleteManager: AutocompleteManager

    // ViewModel para gestionar los datos de la aplicación
    private lateinit var appDataViewModel: AppDataViewModel

    // Binding para acceder a las vistas de la interfaz de usuario
    private lateinit var binding: ActivityLoginBinding

    // Variable para almacenar la empresa seleccionada en el campo de autocompletado
    private var selectedEmpresa: Empresa? = null

    // Manager para la gestión de sesiones, carga solo cuando se accede a él
    private val sessionManager by lazy { SessionManager(this) }

    // Layout para mostrar errores de conexión de red
    private lateinit var networkErrorLayout: View
    private var isNetworkCheckEnabled = Constants.getNetworkStatusHelper()

    // Variables para verificar si los datos de usuario y empresa ya están cargados
    var isEmpresaPreloaded = false
    var isUserPreloaded = false

    /**
     * Método que se ejecuta al crear la actividad.
     * Se inicializan los elementos y se cargan los datos necesarios como el estado de la red,
     * la interfaz de usuario y los datos del usuario y la empresa.
     */
    @OptIn(FlowPreview::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            binding.empresaAutocomplete, this) { empresa ->
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

            val usuario = binding.usuarioEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            // Cerrar el teclado usando AppUtils
            AppUtils.closeKeyboard(this)

            // Usar la empresa almacenada en SharedPreferences si existe
            val empresa = sessionManager.getEmpresaData()

            if (validarCampos()) {

                // Verificar si hay conexión a internet
                if (!NetworkStatusHelper.isNetworkAvailable()) {
                    // Opcionalmente, puedes quitar el foco de la vista actual
                    AppUtils.clearFocus(this)

                    Toast.makeText(this, "No hay conexión a internet", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

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
                        progressDialog.dismiss()
                        val errorBody = e.response()?.errorBody()?.string()
                        val errorMessage = parseErrorMessage(errorBody)
                        Log.e("LoginActivity", "Error HTTP: ${e.code()} - $errorMessage")
                        Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    } catch (e: UnknownHostException) {
                        progressDialog.dismiss()
                        Log.e("LoginActivity", "Error de conexión a internet: ${e.message}")
                        Toast.makeText(
                            this@LoginActivity,
                            "No hay conexión a internet",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: SocketTimeoutException) {
                        progressDialog.dismiss()
                        Log.e("LoginActivity", "Timeout de la petición: ${e.message}")
                        Toast.makeText(
                            this@LoginActivity,
                            "Timeout de la petición",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        progressDialog.dismiss()
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
            }
        }

        // Detecta el tamaño de la ventana y ajusta el formulario si el teclado está visible
        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            binding.root.getWindowVisibleDisplayFrame(rect)
            val screenHeight = binding.root.rootView.height

            val keypadHeight = screenHeight - rect.bottom
            if (keypadHeight > screenHeight * 0.15) { // Teclado visible
                binding.scrollView.scrollTo(0, binding.loginButton.bottom)
            } else {
                // Teclado oculto, opcionalmente puedes restablecer la vista aquí si es necesario
            }
        }

        networkErrorLayout = findViewById(R.id.networkErrorLayout) // Enlaza con el layout de error de red

        // Observa los cambios en la conectividad de red
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                NetworkStatusHelper.networkAvailable
//                .debounce(3000) // Evita fluctuaciones rápidas en la red
                    .collect { isConnected ->
                        if (isNetworkCheckEnabled) {
                            if (isConnected) {
                                hideNetworkErrorLayout()
                                val adapter = binding.empresaAutocomplete.adapter
                                if (adapter == null || adapter.count == 0) {
                                    // Si el adaptador está vacío, recargar empresas
                                    autocompleteManager.loadEmpresas(
                                        binding.empresaAutocomplete,
                                        this@LoginActivity
                                    ) { empresa ->
                                        Log.d(
                                            "LoginActivity",
                                            "Empresa cargada tras reconexión: $empresa"
                                        )
                                        selectedEmpresa = empresa
                                    }
                                }
                            } else {
                                showNetworkErrorLayout()
                            }
                        }
                }
            }
        }

        // Configuración del botón "Reintentar" dentro del layout de error
        val retryButton = findViewById<TextView>(R.id.retry_button)
        retryButton.setOnClickListener {
            // Intenta recargar si hay conexión
            if (NetworkStatusHelper.isNetworkAvailable()) {
                NetworkStatusHelper.refreshNetworkStatus()
            } else {
                val textViewError = networkErrorLayout.findViewById<TextView>(R.id.textViewError)
                textViewError?.text = "Sigue sin conexión a internet :("
            }
        }


        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            binding.root.getWindowVisibleDisplayFrame(rect)
            val screenHeight = binding.root.rootView.height
            val keypadHeight = screenHeight - rect.bottom

            if (keypadHeight > screenHeight * 0.15) { // El teclado está visible
                val focusedView = currentFocus
                if (focusedView != null) {
                    binding.scrollView.post {
                        binding.scrollView.smoothScrollTo(0, focusedView.bottom)
                    }
                }
            }
        }
    }

    /**
     * Función que gestiona el layout de errores de red y recarga componentes si la red está disponible.
     */
    private fun showNetworkErrorLayout() {
        val networkErrorLayout = findViewById<View>(R.id.networkErrorLayout)

        // Cerrar el teclado usando AppUtils
        AppUtils.closeKeyboard(this)

        if (networkErrorLayout.visibility != View.VISIBLE) {
            networkErrorLayout.animate()
                .alpha(1f)
                .setDuration(300)
                .withStartAction { networkErrorLayout.visibility = View.VISIBLE }
                .start()
        }
        networkErrorLayout.isClickable = true
        networkErrorLayout.isFocusable = true
    }

    private fun hideNetworkErrorLayout() {
        val networkErrorLayout = findViewById<View>(R.id.networkErrorLayout)
        val textViewError = networkErrorLayout.findViewById<TextView>(R.id.textViewError)
//        networkErrorLayout.visibility = View.GONE
        if (networkErrorLayout.visibility == View.VISIBLE) {
            networkErrorLayout.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction { networkErrorLayout.visibility = View.GONE }
                .start()
        }
        textViewError?.text="Se perdio la conexión a internet"
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

                // Iniciar la actividad principa
//                startActivity(Intent(this, MainActivity::class.java))
//                finish()

                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("CHECK_UPDATES_ON_STARTUP", true) // Indicar que se debe verificar actualizaciones
                startActivity(intent)
                finish() // Finaliza LoginActivity para limpiar la pila

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