package com.example.adminobr

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.appcompat.app.AppCompatActivity
import com.example.adminobr.ui.login.LoginActivity
import com.example.adminobr.databinding.ActivityMainBinding
import com.example.adminobr.viewmodel.AppDataViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.lifecycle.lifecycleScope
import com.example.adminobr.update.UpdateManager
import kotlinx.coroutines.launch
import android.app.AlertDialog
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import com.example.adminobr.ui.usuarios.EditMode
import com.example.adminobr.utils.SessionManager
import com.example.adminobr.update.DownloadService
import com.example.adminobr.utils.NetworkStatusHelper

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var fab: FloatingActionButton

    private lateinit var sessionManager: SessionManager

    private lateinit var networkHelper: NetworkStatusHelper

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar SessionManager
        sessionManager = SessionManager(applicationContext)

        // Inicializa NetworkStatusHelper después de que la actividad ha sido creada
        networkHelper = NetworkStatusHelper(this)

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
//        val empresaName = empresa?.nombre?.lowercase() ?: "No hay empresa disponible" // Convertir a minúsculas
        val empresaName = empresa?.nombre ?: "No hay empresa disponible" // Convertir a minúsculas

        val userTextView: TextView = headerView.findViewById(R.id.userTextView)
        val fullName = "$userLastName $userName"
        userTextView.text = fullName.split(" ").joinToString(" ") {
            it.replaceFirstChar { char -> char.uppercase() }
        }

        val empresaTextView: TextView = headerView.findViewById(R.id.empresaTextView)
        empresaTextView.text = empresaName
//        empresaTextView.text = empresaName.split(" ").joinToString(" ") {
//            it.replaceFirstChar { char -> char.uppercase() }
//        }

        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        val versionTextView: TextView = headerView.findViewById(R.id.versionTextView)

        if (BuildConfig.DEBUG) {
            versionTextView.text = "v $versionName (Debug)"
        } else {
            versionTextView.text = "v $versionName"
        }

        // Comprobar y solicitar el permiso de notificaciones
        checkNotificationPermission()

        // Iniciar el servicio de actualización dentro de lifecycleScope.launch
        lifecycleScope.launch {
            checkUpdates(isAutomatic = true) // false para NO iniciar automaticamente
        }
    }

    override fun onStart() {
        super.onStart()
        // Verificar si hay una actualización pendiente en Wi-Fi
        checkPendingUpdateOnWifi()
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
        // Borrar las credenciales del user
        //val sharedPreferences = getSharedPreferences("mis_preferencias", Context.MODE_PRIVATE)
        //val editor = sharedPreferences.edit()
        //editor.clear()
        //editor.apply()
        //editor.remove("user_legajo")  // Borrar solo el legajo del usuario
        //editor.remove("user_nombre")  // Borrar solo el nombre del usuario
        //editor.remove("user_apellido")  // Borrar solo el apellido del usuario

        // Limpiar la sesión
        //sessionManager.clearSession()

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
        val changePasswordItem = navMenu.findItem(R.id.nav_change_password)
        val editProfileItem = navMenu.findItem(R.id.nav_edit_profile)

        // Controlar la visibilidad del elemento según los roles del usuario
        if (userRoles?.contains("supervisor") == true || userRoles?.contains("administrador") == true) {
            gestionUsuariosItem.isVisible = true
        }

        // Sin restrinccion de rol, para su visivilidad
        changePasswordItem.isVisible = true
        editProfileItem.isVisible = true

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
                    findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_partediario) // Reemplaza R.id.nav_partediario con el ID del fragmento destino
                    binding.drawerLayout.closeDrawers()
                    false
                }
                R.id.nav_check_update -> {
                    // Acción para nav_check_updates
                    checkUpdates()
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
                    val bundle = bundleOf("userId" to userId, "editMode" to EditMode.CHANGE_PASSWORD.name)
                    findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_userFormFragment, bundle)
                    binding.drawerLayout.closeDrawers()
                    false
                }
                R.id.nav_edit_profile -> {
                    // Acción para nav_edit_profile
                    val userId = sessionManager.getUserId()
                    val bundle = bundleOf("userId" to userId, "editMode" to EditMode.EDIT_PROFILE.name)
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
        val appDataViewModel: AppDataViewModel by viewModels()
//        appDataViewModel.cargarDatos()
        Log.d("MainActivity", "onResume: Datos iniciales cargados")

    }

    private fun checkUpdates(isAutomatic: Boolean = false) {

        if (!checkPendingUpdateOnWifi()) {

            lifecycleScope.launch {
                val updateManager = UpdateManager(this@MainActivity)
                val hasUpdate = updateManager.checkForUpdates()
                if (hasUpdate) {
                    val latestVersion = updateManager.getLatestVersion()
                    showUpdateDialog(latestVersion.apkUrl, latestVersion.versionName)
                } else if (!isAutomatic) {
                    // Mostrar mensaje de "no hay actualizaciones disponibles" solo si la llamada es manual
                    Toast.makeText(
                        this@MainActivity,
                        "Ya tienes la última versión.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showUpdateDialog(apkUrl: String, versionName: String) {
        val builder = AlertDialog.Builder(this)
            .setTitle("Actualización disponible")
            .setMessage("La versión $versionName ya está disponible.\n¿Actualizar ahora?") // Salto de línea aquí
            .setPositiveButton("Actualizar") { _, _ ->
                if (networkHelper.isWifiConnected()) {
                    startDownloadService(apkUrl)
                } else {
                    showWifiWarningDialog(apkUrl)
                }
            }
            .setNegativeButton("Omitir", null)

        val dialog = builder.create() // Crear el diálogo aquí

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.black))
        }

        dialog.show()
    }

    private fun showWifiWarningDialog(apkUrl: String) {
        val builder = AlertDialog.Builder(this) // Cambiar a builder
            .setTitle("Conexión de datos móviles")
            .setMessage("Descargar ahora puede consumir tu plan de datos. ¿Desea continuar o descargar con Wi-Fi?")
            .setPositiveButton("Continuar") { _, _ ->
                // Continúa con la descarga usando datos móviles
                startDownloadService(apkUrl)
            }
            .setNegativeButton("Descargar con Wi-Fi") { _, _ ->
                // Guarda la URL para la próxima vez que se conecte a Wi-Fi
                sessionManager.savePendingUpdateUrl(apkUrl)
                Toast.makeText(this, "La actualización se descargará cuando estés conectado a Wi-Fi.", Toast.LENGTH_SHORT).show()
            }

        val dialog = builder.create() // Crear el diálogo aquí

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.black))
        }

        dialog.show()
    }

    private fun startDownloadService(apkUrl: String) {
        val intent = Intent(this, DownloadService::class.java)
        intent.putExtra("apkUrl", apkUrl)
        startService(intent)
    }

    private fun checkPendingUpdateOnWifi():Boolean {
        val pendingUpdateUrl = sessionManager.getPendingUpdateUrl()
        pendingUpdateUrl?.let {
            if (networkHelper.isWifiConnected()) {
                startDownloadService(it)
                sessionManager.clearPendingUpdateUrl()
            }
            return true
        }
        return false
    }

}
