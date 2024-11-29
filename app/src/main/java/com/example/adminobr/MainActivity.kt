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
import android.content.pm.PackageManager
import android.os.Build
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

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var fab: FloatingActionButton

    private lateinit var sessionManager: SessionManager

    // Layout para mostrar errores de conexión de red
    private lateinit var networkErrorLayout: View
    private var isNetworkCheckEnabled = Constants.getNetworkStatusHelper()

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    @OptIn(FlowPreview::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        val empresaName = empresa?.nombre ?: "No hay empresa disponible" // Convertir a minúsculas

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

        // Comprobar y solicitar el permiso de notificaciones
        checkNotificationPermission()

        // Verificar si se debe comprobar actualizaciones automáticamente
        val checkUpdatesOnStartup = intent.getBooleanExtra("CHECK_UPDATES_ON_STARTUP", false)
        if (checkUpdatesOnStartup) {
            checkUpdatesOnStartup(false) // No mostrar mensajes Toast en modo automático
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
                                hideNetworkErrorLayout()
                                if (NetworkStatusHelper.isWifiConnected()) {
                                    // Si la conexión es Wi-Fi, verifica descargas pendientes
                                    val updateManager = UpdateManager(this@MainActivity)
                                    updateManager.handlePendingDownload()
                                }
                            } else {
                                showNetworkErrorLayout()
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

    /**
     * Función que gestiona el layout de errores de red y recarga componentes si la red está disponible.
     */
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

    // Funcionalidad para solicitar permisos de notificaciones
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Verificar la versión de Android
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, puedes mostrar notificaciones
            } else {
                // Permiso no concedido, solicitar permiso
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
            }
        } else {
            // En versiones anteriores a Android 13, no se necesita solicitar el permiso
            // Puedes mostrar un mensaje al usuario o simplemente omitir la solicitud
        }
    }

    private fun cerrarSesion() {
        // Crear un Intent para iniciar la actividad de Login
        val intent = Intent(this, LoginActivity::class.java)

        // Configurar las flags para crear una nueva tarea y limpiar la pila de actividades
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        // Iniciar la actividad de Login
        startActivity(intent)

        // Finalizar la actividad actual
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                cerrarSesion()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
                    cerrarSesion()
                    false
                }
                R.id.nav_partediario -> {
                    // Acción para nav_partesdiarios
                    val bundle = bundleOf("editMode" to false)
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
                    val bundle = bundleOf("userId" to userId, "editType" to EditType.CHANGE_PASSWORD.name)
                    findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_userFormFragment, bundle)
                    binding.drawerLayout.closeDrawers()
                    false
                }
                R.id.nav_edit_profile -> {
                    // Acción para nav_edit_profile
                    val userId = sessionManager.getUserId()
                    val bundle = bundleOf("userId" to userId, "editType" to EditType.EDIT_PROFILE.name)
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

    /**
     * Configura la opción manual para comprobar actualizaciones.
     */
    private fun setupCheckUpdateManualOption() {
        lifecycleScope.launch {
            val updateManager = UpdateManager(this@MainActivity)
            updateManager.checkForUpdates(true)
        }
    }

    /**
     * Comprueba si hay actualizaciones disponibles.
     * @param showToast Indica si se deben mostrar mensajes Toast.
     */
    private fun checkUpdatesOnStartup(showToast: Boolean) {
        lifecycleScope.launch {
            val updateManager = UpdateManager(this@MainActivity)
            updateManager.checkForUpdates(showToast)
        }
    }
//    private fun checkUpdatesOnStartup() {
//        lifecycleScope.launch {
//            val updateManager = UpdateManager(this@MainActivity)
//            updateManager.checkForUpdates()
//        }
//    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
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
    }






}
