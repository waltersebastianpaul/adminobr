package com.example.adminobr

import android.content.Context
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
import java.util.Locale
import androidx.lifecycle.lifecycleScope
import com.example.adminobr.update.UpdateManager
import kotlinx.coroutines.launch
import android.app.AlertDialog
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.adminobr.utils.SessionManager
import com.example.adminobr.update.DownloadService

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var fab: FloatingActionButton

    private lateinit var sessionManager: SessionManager

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

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
        val empresaName = empresa?.nombre ?: "No hay email disponible"

        val empresaTextView: TextView = headerView.findViewById(R.id.empresaTextView)
        empresaTextView.text = empresaName.uppercase(Locale.ROOT)

        val userTextView: TextView = headerView.findViewById(R.id.userTextView)
        val fullName = "$userLastName $userName"
        userTextView.text = fullName.split(" ").joinToString(" ") {
            it.replaceFirstChar { char -> char.uppercase() }
        }

        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        val versionTextView: TextView = headerView.findViewById(R.id.versionTextView)
        versionTextView.text = "v $versionName"

        // Comprobar y solicitar el permiso de notificaciones
        checkNotificationPermission()

        // Iniciar el servicio de actualización dentro de lifecycleScope.launch
        lifecycleScope.launch {
            checkUpdates(isAutomatic = true) // false para NO iniciar automaticamente
        }
    }

    // Funcionalidad para solicitar permisos de notificaciones
    private fun checkNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            // Permiso concedido, puedes mostrar notificaciones
        } else {
            // Permiso no concedido, solicitar permiso
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun cerrarSesion() {
        // Borrar las credenciales del user
        val sharedPreferences = getSharedPreferences("mis_preferencias", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        // Limpiar la sesión
        sessionManager.clearSession()

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
        if (userRoles?.contains("supervisor"()) == true || userRoles?.contains("administrador") == true) {
            gestionUsuariosItem.isVisible = true
        }

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_cerrar_sesion -> {
                    cerrarSesion()
                    true
                }
                R.id.nav_partediario -> {
                    // Acción para nav_partesdiarios
                    findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_partediario) // Reemplaza R.id.nav_partediario con el ID del fragmento destino
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_check_update -> {
                    // Acción para nav_check_updates
                    checkUpdates()
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_gestion_usuarios -> {
                    // Acción para nav_gestion_usuarios
                    findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_gestion_usuarios) // Reemplaza R.id.nav_partediario con el ID del fragmento destino
                    binding.drawerLayout.closeDrawers()
                    true
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
        lifecycleScope.launch {
            val updateManager = UpdateManager(this@MainActivity)
            val hasUpdate = updateManager.checkForUpdates()
            if (hasUpdate) {
                val latestVersion = updateManager.getLatestVersion()
                showUpdateDialog(latestVersion.apkUrl, latestVersion.versionName)
            } else if (!isAutomatic) {
                // Mostrar mensaje de "no hay actualizaciones disponibles" solo si la llamada es manual
                Toast.makeText(this@MainActivity, "No hay actualizaciones disponibles", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showUpdateDialog(apkUrl: String, versionName: String) {
        AlertDialog.Builder(this)
            .setTitle("Actualización disponible")
            .setMessage("Se ha encontrado una nueva versión ($versionName). ¿Desea descargar e instalar la actualización?")
            .setPositiveButton("Sí") { _, _ ->
                val intent = Intent(this, DownloadService::class.java)
                intent.putExtra("apkUrl", apkUrl)
                startService(intent)
            }
            .setNegativeButton("No", null)
            .show()
    }

}
