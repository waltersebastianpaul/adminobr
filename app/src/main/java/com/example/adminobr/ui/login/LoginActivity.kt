package com.example.adminobr.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.adminobr.BuildConfig
import com.example.adminobr.MainActivity
import com.example.adminobr.R
import com.example.adminobr.data.Obra
import com.example.adminobr.data.ResultData
import com.example.adminobr.databinding.ActivityLoginBinding
import com.example.adminobr.repository.LoginRepository
import com.example.adminobr.utils.AppUtils
import com.example.adminobr.utils.AutocompleteManager
import com.example.adminobr.utils.Constants
import com.example.adminobr.utils.LoadingDialogUtil
import com.example.adminobr.utils.NetworkStatusHelper
import com.example.adminobr.utils.SessionManager
import com.example.adminobr.viewmodel.AppDataViewModel
import com.example.adminobr.viewmodel.LoginViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val loginViewModel: LoginViewModel by viewModels()
    private val loginRepository = LoginRepository()

    // Manager para la gestión de sesiones, carga solo cuando se accede a él
    private val sessionManager by lazy { SessionManager(this) }

    // Layout para mostrar errores de conexión de red
    private lateinit var networkErrorLayout: View
    private var isNetworkCheckEnabled = Constants.getNetworkStatusHelper()
    private var isNetworkErrorLayoutEnabled = Constants.getNetworkErrorLayout()

    // Administrador de autocompletado para cargar empresas
    private lateinit var autocompleteManager: AutocompleteManager

    private lateinit var obraAutocomplete: AutoCompleteTextView
    private lateinit var obraLayout: TextInputLayout
    private lateinit var obraTextInputLayout: TextInputLayout
    private lateinit var empresaEditText: TextInputEditText
    private lateinit var empresaTextInputLayout: TextInputLayout
    private lateinit var usuarioEditText: TextInputEditText
    private lateinit var usuarioTextInputLayout: TextInputLayout
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var passwordTextInputLayout: TextInputLayout
    private lateinit var tvNotMeLink: TextView

    // ViewModel para gestionar los datos de la aplicación
    private val appDataViewModel: AppDataViewModel by viewModels() // Usamos el delegado by viewModels()

    private var empresaDbName: String? = null
    private lateinit var empresaData: Map<String, String?>
    private lateinit var obraData: Map<String, String?>
    private lateinit var userDetails: Map<String, String?>

    private var selectedObra: Obra? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar el ícono del menú
        val menuIcon = findViewById<ImageView>(R.id.menuIcon)
        menuIcon.setOnClickListener { showPopupMenu(it) }

        // Asignar valores por defecto en modo Debug
        val isDebuggable = BuildConfig.DEBUG

        // Guardar el valor de isDebuggable en SessionManager
        sessionManager.saveDebuggable(isDebuggable)
        Log.d("LoginActivity", "Debuggable: $isDebuggable")

        // Inicialización de autocompleteManager después de appDataViewModel
        autocompleteManager = AutocompleteManager(this, appDataViewModel, sessionManager)

        // Obtener y almacenar los datos de la empresa
        empresaData = sessionManager.getEmpresaData()

        // Obtener y almacenar los datos de la obra
        obraData = sessionManager.getObraData()

        obraAutocomplete = binding.obraAutocomplete
        obraTextInputLayout = binding.obraTextInputLayout
        empresaEditText = binding.empresaEditText
        empresaTextInputLayout = binding.empresaTextInputLayout
        usuarioEditText = binding.usuarioEditText
        usuarioTextInputLayout = binding.usuarioTextInputLayout
        passwordEditText = binding.passwordEditText
        passwordTextInputLayout = binding.passwordTextInputLayout
        tvNotMeLink = binding.tvNotMeLink


        // Versión actual de la app
        val currentVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: ""

        // Última versión almacenada
        val lastVersion = sessionManager.getLastVersion()

        if (lastVersion.isNullOrEmpty() || isVersionGreater(currentVersion, lastVersion)) {
            // Actualizamos la última versión almacenada
            sessionManager.saveLastVersion(currentVersion)

// Ejecutar acción única para una versión específica
            if (currentVersion == "1.0.33") {
                // Forzar revalidación del login
                sessionManager.clearAuthToken()
                sessionManager.clearEmpresaData()
                sessionManager.clearObraData()

                // Configurar empresa predeterminada
                empresaEditText.setText("JEPSA")

                Log.d("LoginActivity", "Acción única ejecutada para la versión $currentVersion")
            }
// Fin de la ejecución de la acción única

        } else {
            Log.d("LoginActivity", "Versión actual: $currentVersion, última guardada: $lastVersion")
        }

        val token = sessionManager.getAuthToken()
        if (!token.isNullOrEmpty()) {
            lifecycleScope.launch {
                val result = loginViewModel.validateToken(token) // Validar el token
                if (result is ResultData.Success) {
                    startMainActivity() // Redirigir al MainActivity si es válido
                    finish()
                }
            }
        }

        // Eliminar las claves específicas
        //sessionManager.removeKeys("last_version") // agregar una o varias key a eliminar

        initUI()
        setupObservers()
        setupListeners()

        // Detecta el tamaño de la ventana y ajusta el formulario si el teclado está visible
        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = android.graphics.Rect()
            binding.root.getWindowVisibleDisplayFrame(rect)
            val screenHeight = binding.root.rootView.height
            val keypadHeight = screenHeight - rect.bottom

            if (keypadHeight > screenHeight * 0.15) {
                val focusedView = currentFocus
                if (focusedView != null) {
                    binding.scrollView.post {
                        binding.scrollView.smoothScrollTo(0, focusedView.bottom)
                    }
                }
            }
        }

        // Configurar la validación de empresa
        binding.validateEmpresaButton.setOnClickListener {
            AppUtils.closeKeyboard(this)
            if (isNetworkCheckEnabled && NetworkStatusHelper.isConnected()) {
                validateEmpresa()
            } else {
//                Toast.makeText(this, "No hay conexión a internet, intenta mas tardes.", Toast.LENGTH_SHORT).show()
                Snackbar.make(binding.root, "No hay conexión a internet, intenta mas tardes.", Snackbar.LENGTH_LONG).show()
            }
        }

        binding.loginButton.setOnClickListener {
            AppUtils.closeKeyboard(this)
            if (isNetworkCheckEnabled) {
                if (NetworkStatusHelper.isConnected()) {
                    validateLogin()
                } else {
//                    Toast.makeText(this, "No hay conexión a internet, intenta mas tardes.", Toast.LENGTH_SHORT).show()
                    Snackbar.make(binding.root, "No hay conexión a internet, intenta mas tardes.", Snackbar.LENGTH_LONG)
                        .setBackgroundTint(ContextCompat.getColor(this, R.color.colorDanger))
                        .setActionTextColor(ContextCompat.getColor(this, R.color.colorWhite))
                        .show()
                }
            }
        }

        // Agregar listeners para los campos de texto
        addTextWatcher(empresaTextInputLayout, "Campo requerido")
        addTextWatcher(usuarioTextInputLayout, "Campo requerido")
        addTextWatcher(passwordTextInputLayout, "Campo requerido")
        addTextWatcher(obraTextInputLayout, "Campo requerido")

        // Llamar a la función para convertir el texto a mayúsculas
        setEditTextToUppercase(empresaEditText)
        setAutocompleteToUppercase(obraAutocomplete)

        // Referencia al layout de error
        networkErrorLayout = findViewById(R.id.networkErrorLayout)

        // Detecta cambios en la conectividad
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                NetworkStatusHelper.networkAvailable
//                .debounce(3000) // Evita fluctuaciones rápidas en la red
                .collect { isConnected ->
                    if (isNetworkCheckEnabled) {
                        if (isConnected) {
                            if (!empresaData["empresaDbName"].isNullOrEmpty()) loadObrasData()

                            if (isNetworkErrorLayoutEnabled) hideNetworkErrorLayout()
                        } else {
                            if (isNetworkErrorLayoutEnabled) showNetworkErrorLayout()
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

    }

    private fun isVersionGreater(version1: String, version2: String): Boolean {
        val v1Parts = version1.split(".").map { it.toIntOrNull() ?: 0 }
        val v2Parts = version2.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(v1Parts.size, v2Parts.size)) {
            val part1 = v1Parts.getOrElse(i) { 0 }
            val part2 = v2Parts.getOrElse(i) { 0 }
            if (part1 != part2) return part1 > part2
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    private fun showPopupMenu(view: View) {
        // Crear el PopupMenu
        val popupMenu = PopupMenu(this, view)
        popupMenu.inflate(R.menu.menu_login) // Inflar el menú desde XML

        // Configurar acciones de los ítems
        popupMenu.setOnMenuItemClickListener { item ->

            when (item.itemId) {
                R.id.action_change_company -> {
                    sessionManager.clearEmpresaData()
                    sessionManager.clearObraData()
                    showEmpresaStep()
                    true
                }
                R.id.action_exit_app -> {
                    finishAffinity() // Cierra la aplicación
                    true
                }
                else -> false
            }
        }

        // Mostrar el menú
        popupMenu.show()
    }

    private fun setupListeners() {
        empresaEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.validateEmpresaButton.performClick() // Simula un clic en el botón de login
                true // Indica que la acción se ha manejado
            } else {
                false // Indica que la acción no se ha manejado
            }
        }

        passwordEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.loginButton.performClick() // Simula un clic en el botón de login
                true // Indica que la acción se ha manejado
            } else {
                false // Indica que la acción no se ha manejado
            }
        }

        obraAutocomplete.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.loginButton.performClick() // Simula un clic en el botón de login
                true // Indica que la acción se ha manejado
            } else {
                false // Indica que la acción no se ha manejado
            }
        }
    }

    private fun setupObservers() {

        // Observador para errores al cargar obras
//        appDataViewModel.errorObras.observe(this) { event ->
//            event.getContentIfNotHandled()?.let { message ->
//                if (!empresaData["empresaDbName"].isNullOrEmpty()) {
//                    if (NetworkStatusHelper.isNetworkAvailable()) {
//                        if (event != null) {
//                            binding.loginButton.isEnabled = false
//                            // Mostrar un mensaje o gestionar la recarga
//                            Snackbar.make(binding.root, message, Snackbar.LENGTH_INDEFINITE)
//                                .setAction("Reintentar") {
//                                    loadObrasData()
//                                }
//                                .show()
//                        } else {
//                            binding.loginButton.isEnabled = true
//                        }
//                    }
//                }
//            }
//        }

        appDataViewModel.errorObras.observe(this) { event ->
            val message = event.getContentIfNotHandled()
            if (!empresaData["empresaDbName"].isNullOrEmpty()) {
                if (NetworkStatusHelper.isNetworkAvailable()) {
                    if (message != null) {
                        binding.loginButton.isEnabled = false
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_INDEFINITE)
                            .setAction("Reintentar") {
                                loadObrasData() // Llamada a recargar las obras
                            }
                            .show()
                    } else {
                        // Si no hay error, habilita el botón
                        binding.loginButton.isEnabled = true
                    }
                } else {
                    // Habilita el botón incluso sin conexión si no hay error
                    binding.loginButton.isEnabled = true
                }
            }
        }



        loginViewModel.successMessage.observe(this) { event ->
            event.getContentIfNotHandled()?.let { message ->
//                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
            }
        }

//        // Observador para mensajes de error usando getContentIfNotHandled()
//        loginViewModel.errorMessage.observe(this) { message ->
//            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
//        }

        loginViewModel.fieldErrorMessage.observe(this) { event ->
            event.getContentIfNotHandled()?.let { errorCode ->
                when (errorCode) {
                    "wrong_password" -> binding.passwordTextInputLayout.error = "Contraseña incorrecta."
                    "user_not_found" -> binding.usuarioTextInputLayout.error = "Usuario no encontrado."
                    "empresa_not_found" -> binding.empresaTextInputLayout.error = "Empresa no encontrada."
                    "wrong_empresa" -> binding.empresaTextInputLayout.error = "Empresa incorrecta."
//                    "missing_data" -> Toast.makeText(this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show()
                    "missing_data" -> Snackbar.make(binding.root, "Por favor, complete todos los campos.", Snackbar.LENGTH_LONG).show()
//                    "db_connection" -> Toast.makeText(this, "Error de conexión con la base de datos.", Toast.LENGTH_SHORT).show()
                    "db_connection" -> Snackbar.make(binding.root, "Error de conexión con la base de datos.", Snackbar.LENGTH_LONG).show()
                    else -> Log.e("LoginActivity", "Código de error desconocido: $errorCode")
                }
            }
        }

        loginViewModel.errorMessage.observe(this) { event ->
            event.getContentIfNotHandled()?.let { message ->
//                Toast.makeText(this,  message, Toast.LENGTH_SHORT).show()
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(ContextCompat.getColor(this, R.color.colorDanger))
                    .setActionTextColor(ContextCompat.getColor(this, R.color.colorWhite))
                    .show()
            }
        }

        // Observador para la validación de empresa
        loginViewModel.empresaValidationResult.observe(this) { result ->
            LoadingDialogUtil.hideLoading(lifecycleScope, 500L)

            when (result) {
                is ResultData.Success -> {
                    val empresa = result.data
                    Log.d("LoginActivity", "Validación exitosa. Empresa recibida: $empresa")

                    if (empresa.db_name.isNotEmpty()) {
                        Log.d("LoginActivity", "Guardando datos de empresa: ${empresa.code}, ${empresa.nombre}, ${empresa.db_name}")
                        sessionManager.saveEmpresaData(empresa.code, empresa.nombre, empresa.db_name)
                        empresaDbName = empresa.db_name
                        showLoginStep2()
                    } else {
                        Log.e("LoginActivity", "Error: db_name vacío en la empresa validada.")
                    }
                }
                is ResultData.Error -> {
                    Log.e("LoginActivity", "Error al validar empresa: ${result.message}")
                }
            }
        }

        // Observador para el login
        loginViewModel.loginResult.observe(this) { result ->
            LoadingDialogUtil.hideLoading(lifecycleScope, 500L)
            when (result) {
                is ResultData.Success -> {
                    val token = result.data.token // Obtener el token del resultado
                    if (!token.isNullOrEmpty()) {
                        sessionManager.saveAuthToken(token) // Guardar el token en SessionManager
                        Log.d("TokenDebug", "Token guardado: $token")
                    } else {
//                        Toast.makeText(this, "Error: No se recibió un token válido.", Toast.LENGTH_SHORT).show()
                        Snackbar.make(binding.root, "Error: No se recibió un token válido.", Snackbar.LENGTH_LONG).show()
                    }

                    val user = result.data.user
                    if (user != null) {
                        sessionManager.saveUserDetails(
                            user.id ?: -1,
                            user.legajo,
                            user.nombre,
                            user.apellido,
                            user.roles,
                            user.principalRole,
                            user.email,
                            user.telefono
                        )
                        startMainActivity()
                    } else {
//                        Toast.makeText(this, "Error: Usuario inválido.", Toast.LENGTH_SHORT).show()
                        Snackbar.make(binding.root, "Error: Usuario inválido.", Snackbar.LENGTH_LONG).show()

                    }
                }
                is ResultData.Error -> {
//                    Toast.makeText(this, "${result.message}", Toast.LENGTH_SHORT).show()
                    Snackbar.make(binding.root, "${result.message}", Snackbar.LENGTH_LONG).show()

                }
            }
        }
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun initUI() {

        empresaData = sessionManager.getEmpresaData()
        if (empresaData["empresaDbName"].isNullOrEmpty()) {
            showEmpresaStep()
        } else {
            empresaDbName = empresaData["empresaDbName"]
            showLoginStep2() // Si ya hay datos de empresa, ir directamente al paso 2
        }
    }

    private fun showEmpresaStep() {

        binding.empresaTextInputLayout.isErrorEnabled = false

        // Mostrar solo el campo de empresa y el botón de validar
        binding.empresaTextInputLayout.visibility = View.VISIBLE
        binding.validateEmpresaButton.visibility = View.VISIBLE

        // Ocultar otros elementos del paso 2
        binding.usuarioTextInputLayout.visibility = View.GONE
        binding.recuperarPasswordButton.visibility = View.GONE
        binding.passwordTextInputLayout.visibility = View.GONE
        binding.obraTextInputLayout.visibility = View.GONE
        binding.loginButton.visibility = View.GONE
        binding.menuIcon.visibility = View.GONE
        binding.obraLayout.visibility = View.GONE

        binding.tvWelcomeMessage.visibility = View.GONE
        binding.tvNotMeLink.visibility = View.GONE
    }

    private fun showLoginStep2() {

        // Limpia los errores de todos los TextInputLayout
        val inputLayouts = listOf(
            binding.usuarioTextInputLayout,
            binding.passwordTextInputLayout,
            binding.obraTextInputLayout
        )

        inputLayouts.forEach { it.isErrorEnabled = false }

        loadObrasData()

        empresaData = sessionManager.getEmpresaData()
        obraData = sessionManager.getObraData()

        // Ocultar los elementos del paso 1
        binding.empresaTextInputLayout.visibility = View.GONE
        binding.validateEmpresaButton.visibility = View.GONE

        // Mostrar los elementos del paso 2
        binding.menuIcon.visibility = View.VISIBLE

        binding.recuperarPasswordButton.visibility = View.VISIBLE
        binding.passwordTextInputLayout.visibility = View.VISIBLE
        binding.loginButton.visibility = View.VISIBLE

        if (obraData["obraNombre"].isNullOrEmpty() || binding.obraAutocomplete.text.isEmpty()) {
            binding.obraAutocomplete.setText("")
            binding.obraTextInputLayout.visibility = View.VISIBLE
            binding.obraLayout.visibility = View.GONE
            binding.obraTextView.text = ""
        } else {
            // Mostrar la obra seleccionada y permitir cambiarla
            binding.obraTextView.text = "Obra: ${obraData["obraNombre"]}"
            binding.obraLayout.visibility = View.VISIBLE
            binding.obraTextInputLayout.visibility = View.GONE

            binding.obraEditIcon.setOnClickListener {
                binding.obraLayout.visibility = View.GONE
                binding.obraTextInputLayout.visibility = View.VISIBLE
            }
        }

        // Obtener detalles de usuario guardados
        userDetails = sessionManager.getUserDetails()

        if (!userDetails["nombre"].isNullOrEmpty()) {
            // Oculta el texto del logo
            binding.logoTextImageView.visibility = View.GONE

            // Mostrar saludo inicial
            binding.apply {
                tvWelcomeMessage.text = "¡Hola ${userDetails["nombre"]?.uppercase()}!"
                tvWelcomeMessage.visibility = View.VISIBLE
                tvNotMeLink.visibility = View.VISIBLE
            }

            // Ocultar campo de usuario si hay datos válidos
            if (!userDetails["legajo"].isNullOrEmpty() || !userDetails["email"].isNullOrEmpty()) {
                usuarioEditText.setText(userDetails["legajo"] ?: userDetails["email"])
                usuarioTextInputLayout.visibility = View.GONE
            } else {
                usuarioEditText.setText("")
                usuarioTextInputLayout.visibility = View.VISIBLE
            }

            // Configurar "No soy yo" para borrar los datos de usuario
            tvNotMeLink.setOnClickListener {
                sessionManager.clearUserDetails()
                userDetails = sessionManager.getUserDetails()
                binding.apply {
                    // Oculta el texto del logo
                    logoTextImageView.visibility = View.VISIBLE
                    usuarioTextInputLayout.visibility = View.VISIBLE
                    usuarioEditText.setText("")
                    passwordEditText.setText("")
                    tvWelcomeMessage.visibility = View.GONE
                    tvNotMeLink.visibility = View.GONE
                }
            }

            // Asigna el OnClickListener al TextView de "Recuperar Contraseña"
            binding.recuperarPasswordButton.setOnClickListener {
                Snackbar.make(binding.root, "Comunicarse con el administrador de la app. \nNo disponible en esta versión.", Snackbar.LENGTH_LONG).show()
            }

            obraData.let { obra ->
                if (obra.isNullOrEmpty()) {
                    binding.obraAutocomplete.setText("")
                } else {
                    obraAutocomplete.setText("${obra["centroCosto"]} - ${obra["obraNombre"]}", false)
                    obraAutocomplete.dismissDropDown()
                }
            }

        } else {
            // Muestra el texto del logo
            binding.logoTextImageView.visibility = View.VISIBLE

            // Si no hay datos de usuario, mostrar el campo de usuario
            binding.usuarioTextInputLayout.visibility = View.VISIBLE
            binding.recuperarPasswordButton.visibility = View.GONE
            binding.tvWelcomeMessage.visibility = View.GONE
            binding.tvNotMeLink.visibility = View.GONE
        }
    }

    private fun loadObrasData() {
        // Cargar datos de obras
        loadDataIfEmpty(obraAutocomplete) {
            autocompleteManager.loadObras(
                autoCompleteTextView = obraAutocomplete,
                lifecycleOwner = this,
                empresaDbName = empresaDbName, // Pasamos el db_name explícitamente
                forceRefresh = true, // Forzamos la recarga desde la base externa
                filterEstado = true // Filtramos por estado
            ) { obra ->
                selectedObra = obra
                Log.d("ParteDiarioFormFragment", "Obra seleccionada: $obra")
                // Guardamos la obra seleccionada en SessionManager solo desde el LoginActivity
                sessionManager.saveObraData(obra.id, obra.nombre, obra.centroCosto)
            }
        }
    }

    private fun validateEmpresa() {
        val empresaCode = empresaEditText.text.toString().trim()

        Log.d("LoginActivity", "Validando empresaCode: $empresaCode")

        if (!validateEmpresaFields(empresaCode)) return

        LoadingDialogUtil.showLoading(this, "Validando empresa...")
        loginViewModel.validateEmpresa(empresaCode)
    }

    private fun validateEmpresaFields(empresaCode: String): Boolean {
        var isValid = true
        if (empresaCode.isEmpty()) {
            empresaTextInputLayout.error = "Codigo de empresa requerido"
            isValid = false
        }
        return isValid
    }

    private fun validateLogin() {
        val usuario = usuarioEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val empresaDbName = sessionManager.getEmpresaData()["empresaDbName"]

        obraData = sessionManager.getObraData()
        Log.d("LoginActivity", "Validando credenciales: $usuario, $password, $obraData")

        if (!validateLoginFields(usuario, password, obraData, )) return

        LoadingDialogUtil.showLoading(this, "Iniciando sesión...")
        loginViewModel.login(usuario, password, empresaDbName!!)
    }

    private fun validateLoginFields(usuario: String, password: String, obraData: Map<String, String?>): Boolean {
        var isValid = true

        if (usuario.isEmpty()) {
            binding.usuarioTextInputLayout.error = "Usuario requerido"
            isValid = false
        }
        if (password.isEmpty()) {
            binding.passwordTextInputLayout.error = "Contraseña requerida"
            isValid = false
        }

        if (obraAutocomplete.text.isNotEmpty()) {
            if (selectedObra == null) {
                val obraName = obraAutocomplete.text.toString()
                selectedObra = autocompleteManager.getObraByName(obraName)
                if (selectedObra == null) {
                    obraTextInputLayout.error = "Seleccione una obra válida"
                    obraTextInputLayout.isErrorEnabled = true
                    isValid = false
                } else {
                    obraTextInputLayout.isErrorEnabled = false
                }
            }
        } else {
            obraTextInputLayout.error = "Obra requerida"
            obraTextInputLayout.isErrorEnabled = true
            isValid = false
        }
        return isValid
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

    private fun setAutocompleteToUppercase(autocomplete: AutoCompleteTextView) {
        autocomplete.filters = arrayOf(InputFilter.AllCaps())
    }

    // Función que convierte el texto ingresado en un TextView a mayúsculas.
    private fun setEditTextToUppercase(editText: TextInputEditText) {
        editText.filters = arrayOf(InputFilter.AllCaps())
    }


    // Función que gestiona el layout de errores de red y recarga componentes si la red está disponible.
    private fun showNetworkErrorLayout() {
        val networkErrorLayout = findViewById<View>(R.id.networkErrorLayout)

        // Cerrar el teclado usando AppUtils
        AppUtils.closeKeyboard(this)

        networkErrorLayout.visibility = View.VISIBLE
        networkErrorLayout.isClickable = true
        networkErrorLayout.isFocusable = true
    }

    private fun hideNetworkErrorLayout() {
        val networkErrorLayout = findViewById<View>(R.id.networkErrorLayout)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val textViewError = networkErrorLayout.findViewById<TextView>(R.id.textViewError)
        networkErrorLayout.visibility = View.GONE
        textViewError?.text="Se perdio la conexión a internet"

    }

    // Función que agrega un TextWatcher a un campo de entrada para validar su contenido.
    private fun addTextWatcher(textInputLayout: TextInputLayout, errorMessage: String) {
        val editText = textInputLayout.editText
        editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (editText == obraAutocomplete) {
                    selectedObra = null
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

}
