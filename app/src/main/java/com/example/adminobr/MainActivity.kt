package com.example.adminobr

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.appcompat.app.AppCompatActivity
import com.example.adminobr.ui.login.LoginActivity
import com.example.adminobr.databinding.ActivityMainBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.lifecycle.lifecycleScope
import com.example.adminobr.update.UpdateManager
import kotlinx.coroutines.launch
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.example.adminobr.data.ResultData
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.adminobr.ui.usuarios.EditType
import com.example.adminobr.utils.AppUtils
import com.example.adminobr.utils.Constants
import com.example.adminobr.utils.SessionManager
import com.example.adminobr.utils.NetworkStatusHelper
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.adminobr.update.DownloadWorker
import com.example.adminobr.viewmodel.LoginViewModel
import com.google.android.material.snackbar.Snackbar

import kotlinx.coroutines.delay

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var fab: FloatingActionButton

    private lateinit var sessionManager: SessionManager
    private val loginViewModel: LoginViewModel by viewModels()

    // Layout para mostrar errores de conexión de red
    private lateinit var networkErrorLayout: View
    private var isNetworkCheckEnabled = Constants.getNetworkStatusHelper()
    private var isNetworkErrorLayoutEnabled = Constants.getNetworkErrorLayout()

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bloquear en orientación vertical
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Primero verifica el permiso de notificaciones
        checkNotificationPermission {
            // Luego verifica si es la primera ejecución
            if (isFirstRun() && !hasInstallPermission()) {
                requestInstallPermission()
            }
        }

        // Verificar si se debe comprobar actualizaciones automáticamente
        val checkUpdatesOnStartup = intent.getBooleanExtra("CHECK_UPDATES_ON_STARTUP", true)
        if (checkUpdatesOnStartup && shouldCheckForUpdate()) {
            lifecycleScope.launch {
                delay(5000) // Retrasa por 5 segundos
                checkUpdatesOnStartup(false)
                updateLastCheckTime()
            }
        }

        // Se puede eliminar, solo para ver los datos de las shared preferences
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val allEntries = sharedPrefs.all
        for (entry in allEntries.entries) {
            Log.d("SharedPreferences", "Key: ${entry.key}, Value: ${entry.value}")
        }

        // Inicializar SessionManager
        sessionManager = SessionManager(applicationContext)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)
        fab = binding.appBarMain.fab

        val navView: NavigationView = binding.navView
        val headerView = navView.getHeaderView(0)

        val empresa = sessionManager.getEmpresaData()

        // Usa SessionManager para obtener los datos del usuario
        val userName = sessionManager.getUserNombre() ?: "No hay usuario disponible"
        val userLastName = sessionManager.getUserApellido() ?: "No hay apellido disponible"
        val empresaName = empresa["empresaName"] ?: "No hay empresa disponible" // Convertir a minúsculas

        val userTextView: TextView = headerView.findViewById(R.id.userTextView)
        val fullName = "$userLastName $userName"
        userTextView.text = fullName.split(" ").joinToString(" ") {
            it.replaceFirstChar { char -> char.uppercase() }
        }

        val empresaTextView: TextView = headerView.findViewById(R.id.empresaTextView)
        empresaTextView.text = empresaName

        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        val versionTextView: TextView = headerView.findViewById(R.id.versionTextView)

        if (BuildConfig.DEBUG) {
            versionTextView.text = "v $versionName (Debug)"
        } else {
            versionTextView.text = "v $versionName"
        }

        // Referencia al layout de error
        networkErrorLayout = findViewById(R.id.networkErrorLayout)

        // Observa el estado de la red y actualiza el layout de error
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                NetworkStatusHelper.networkAvailable
//                    .debounce(3000) // Evita fluctuaciones rápidas en la red
                    .collect { isConnected ->
                        if (isNetworkCheckEnabled) {
                            if (isConnected) {
                                if (isNetworkErrorLayoutEnabled) hideNetworkErrorLayout()
                                if (NetworkStatusHelper.isWifiConnected()) {
                                    // Si la conexión es Wi-Fi, verifica descargas pendientes
                                    val updateManager = UpdateManager(this@MainActivity)
                                    updateManager.handlePendingDownload()
                                }
                            } else {
                                if (isNetworkErrorLayoutEnabled) showNetworkErrorLayout()
                            }
                        }
                    }
            }
        }

//        if (NetworkStatusHelper.isWifiConnected()) {
//            Snackbar.make(binding.root, "Conectado por Wi-Fi", Snackbar.LENGTH_SHORT).show()
//        } else {
//            Snackbar.make(binding.root, "Conectado por Datos Moviles", Snackbar.LENGTH_SHORT).show()
//        }

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

    override fun onStart() {
        super.onStart()
        val token = sessionManager.getAuthToken()
        if (token.isNullOrEmpty()) {
            logout()
        }
    }

    private fun shouldCheckForUpdate(): Boolean {
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val lastCheckTime = sharedPrefs.getLong("last_check_time", 0L)
        val currentTime = System.currentTimeMillis()

        // Verificar si han pasado más de 24 horas (en milisegundos)
        val timeElapsed = currentTime - lastCheckTime
        return timeElapsed >= 24 * 60 * 60 * 1000 // 24 horas en milisegundos
    }

    private fun updateLastCheckTime() {
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putLong("last_check_time", System.currentTimeMillis()).apply()
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

        // Restablecer el enfoque al DrawerLayout
        drawerLayout.requestFocus()
    }

    private fun logout() {
        lifecycleScope.launch {
            val token = sessionManager.getAuthToken()
            if (!token.isNullOrEmpty()) {
                val result = loginViewModel.logout(token) // Llama al método logout en LoginViewModel
                when (result) {
                    is ResultData.Success -> {
                        // Si el logout fue exitoso
                        handleLogoutSuccess()
                    }
                    is ResultData.Error -> {
                        // Muestra el error al usuario
//                        Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_SHORT).show()
                        Snackbar.make(binding.root, result.message?: "Error al cerrar sesión", Snackbar.LENGTH_LONG).show()
                    }
                }
            } else {
                // Si no hay token, cierra la sesión localmente
                handleLogoutSuccess()
            }
        }
    }

    // Maneja el cierre de sesión exitoso, sea con o sin token
    private fun handleLogoutSuccess() {
        sessionManager.logout() // Limpia todos los datos de sesión
        val intent = Intent(this@MainActivity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Finaliza la actividad actual
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
//                openSettings()
                true
            }
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showEmpresaStep() {
        sessionManager.logout() // Limpia todos los datos de sesión
        sessionManager.clearEmpresaData()
        sessionManager.clearObraData()
        val intent = Intent(this@MainActivity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Finaliza la actividad actual
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home), binding.drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)


        // Obtener roles del usuario
        val userRoles = sessionManager.getUserRol()

        // Obtener el menú de la NavigationView
        val navMenu = binding.navView.menu

        // Obtener el elemento del menú específico
        val gestionUsuariosItem = navMenu.findItem(R.id.nav_gestion_usuarios)

        // Controlar la visibilidad del elemento según los roles del usuario
        if (userRoles?.contains("supervisor") == true || userRoles?.contains("administrador") == true) {
            gestionUsuariosItem.isVisible = true
        }

        // Marca el ítem de "Inicio" como seleccionado
        binding.navView.setCheckedItem(R.id.nav_home)

        // Manejar eventos de clic en los elementos del menú
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Acción para nav_gestion_usuarios
                    findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_home) // Reemplaza R.id.nav_partediario con el ID del fragmento destino
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_cerrar_sesion -> {
                    logout()
                    false
                }
                R.id.nav_partediario -> {
                    // Acción para nav_partesdiarios
                    val bundle = bundleOf(
                        "editMode" to false
                    )
                    findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_parteDiarioFormFragment, bundle) // Reemplaza R.id.nav_partediario con el ID del fragmento destino
                    binding.drawerLayout.closeDrawers()
                    false
                }
                R.id.nav_check_update -> {
                    // Acción para nav_check_updates
                    setupCheckUpdateManualOption()
                    binding.drawerLayout.closeDrawers()
                    false
                }
                R.id.nav_gestion_usuarios -> {
                    // Acción para nav_gestion_usuarios
                    findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_gestion_usuarios) // Reemplaza R.id.nav_partediario con el ID del fragmento destino
                    binding.drawerLayout.closeDrawers()
                    false
                }
                R.id.nav_change_password -> {
                    // Acción para nav_change_password
                    val userId = sessionManager.getUserId()
                    val bundle = bundleOf(
                        "userId" to userId,
                        "editMode" to true,
                        "editType" to EditType.CHANGE_PASSWORD.name
                    )
                    findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_userFormFragment, bundle)
                    binding.drawerLayout.closeDrawers()
                    false
                }
                R.id.nav_edit_profile -> {
                    // Acción para nav_edit_profile
                    val userId = sessionManager.getUserId()
                    val bundle = bundleOf(
                        "userId" to userId,
                        "editMode" to true,
                        "editType" to EditType.EDIT_PROFILE.name
                    )
                    findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_userFormFragment, bundle)
                    binding.drawerLayout.closeDrawers()
                    false
                }
                // Agregar más casos según sea necesario
                else -> {
                    // Manejar otros elementos del menú si es necesario
                    false
                }
            }
        }
    }

    // Configura la opción manual para comprobar actualizaciones.
    private fun setupCheckUpdateManualOption() {
        lifecycleScope.launch {
            val updateManager = UpdateManager(this@MainActivity)
            updateManager.checkForUpdates(true)
        }
    }

    // Comprueba si hay actualizaciones disponibles.
    // @param showToast Indica si se deben mostrar mensajes Toast.
    private fun checkUpdatesOnStartup(showToast: Boolean) {
        lifecycleScope.launch {
            val updateManager = UpdateManager(this@MainActivity)
            updateManager.checkForUpdates(showToast)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

//        // Modificar visibilidad directamente aquí
//        menu.findItem(R.id.action_settings)?.isVisible = true
//        menu.findItem(R.id.action_logout)?.isVisible = true
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    fun showFab() {
        fab.show()
    }

    fun hideFab() {
        fab.hide()
    }

    fun setFabClickListener(listener: View.OnClickListener) {
        fab.setOnClickListener(listener)
    }

    fun setFabIcon(resourceId: Int) {
        fab.setImageResource(resourceId)
    }

    override fun onResume() {
        super.onResume()
        // Refrescar el estado de red explícitamente
        NetworkStatusHelper.refreshNetworkStatus()

        // Verificar actualizaciones pendientes de instalación
        checkPendingInstallation()
    }

    private fun checkPendingInstallation() {
        val prefs = getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        val pendingUriString = prefs.getString("pending_installation_uri", null)

        if (!pendingUriString.isNullOrEmpty()) {
            val pendingUri = Uri.parse(pendingUriString)

            // Mostrar un Snackbar al usuario
            Snackbar.make(binding.root, "Actualización lista para instalar", Snackbar.LENGTH_INDEFINITE)
                .setAction("Instalar") {
                    DownloadWorker.installApk(this, pendingUri) // Llamada corregida
                    DownloadWorker.clearPendingInstallation(this) // Llamada corregida
                    DownloadWorker.removeNotification(this) // Llamada corregida
                }
                .setBackgroundTint(ContextCompat.getColor(this, R.color.colorAccent))
                .setTextColor(ContextCompat.getColor(this, R.color.colorWhite))
                .setActionTextColor(ContextCompat.getColor(this, R.color.colorWhite))
                .show()
        }
    }

    // Funciones para manejar la primera ejecución
    private fun isFirstRun(): Boolean {
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isFirstRun = sharedPrefs.getBoolean("first_run", true)

        if (isFirstRun) {
            sharedPrefs.edit().putBoolean("first_run", false).apply()
        }

        return isFirstRun
    }

    // Funciones para manejar el permiso de instalación
    private fun hasInstallPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            packageManager.canRequestPackageInstalls()
        } else {
            true // En versiones anteriores a Android O no se necesita este permiso
        }
    }

    // Función para solicitar el permiso de instalar aplicaciones desconocidas
    private fun requestInstallPermission() {
        AlertDialog.Builder(this)
            .setTitle("Permiso necesario")
            .setMessage("Para instalar actualizaciones, la app necesita permiso para instalar aplicaciones desconocidas. ¿Desea otorgar el permiso ahora?")
            .setPositiveButton("Sí") { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:$packageName")
                }
                installPermissionLauncher.launch(intent)
            }
            .setNegativeButton("No") { _, _ ->
                Snackbar.make(binding.root, "No se otorgaron los permisos. La app no podrá gestionar actualizaciones.", Snackbar.LENGTH_LONG).show()
//                Toast.makeText(this, "No se otorgaron los permisos. La app no podrá gestionar actualizaciones.", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    private val installPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Verifica nuevamente si el permiso fue concedido
            if (hasInstallPermission()) {
                Snackbar.make(binding.root, "Permiso de instalación concedido", Snackbar.LENGTH_SHORT).show()
            } else {
                Snackbar.make(binding.root, "Permiso de instalación denegado", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    // Funcionalidad para solicitar permisos de notificaciones
    private fun checkNotificationPermission(onPermissionChecked: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                onPermissionChecked()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
            }
        } else {
            onPermissionChecked() // En versiones anteriores, continúa directamente
        }
    }

    // Sobrescribir onRequestPermissionsResult() para manejar el resultado
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(binding.root, "Permiso de notificaciones concedido", Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(binding.root, "Permiso de notificaciones denegado", Snackbar.LENGTH_SHORT).show()
                }

                // Continua con la lógica de permisos de instalación
                if (isFirstRun() && !hasInstallPermission()) {
                    requestInstallPermission()
                }
            }
        }
    }
}
