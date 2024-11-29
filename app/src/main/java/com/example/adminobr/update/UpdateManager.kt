package com.example.adminobr.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.work.*
import com.example.adminobr.R
import com.example.adminobr.data.VersionInfo
import com.example.adminobr.utils.Constants
import com.example.adminobr.utils.NetworkStatusHelper
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class UpdateManager(private val context: Context) {

    private var updateUrl = Constants.Update.UPDATE_DIR

    /**
     * Comprueba si hay actualizaciones disponibles.
     * @param showToast Indica si se deben mostrar mensajes Toast.
     */
    suspend fun checkForUpdates(showToast: Boolean) {
        try {
            val updateService = Retrofit.Builder()
                .baseUrl(updateUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(UpdateService::class.java)

            val latestVersion = updateService.getLatestVersion()
            val currentVersionCode = getCurrentVersionCode()

            if (latestVersion.versionCode > currentVersionCode) {
                // Hay una nueva versión, muestra el diálogo al usuario
                showUpdateDialog(latestVersion)
            } else if (showToast) {
                Toast.makeText(context, "Tu aplicación está actualizada", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("UpdateManager", "Error al comprobar actualizaciones: ${e.message}")
            if (showToast) {
                Toast.makeText(context, "Error al comprobar actualizaciones", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Obtiene el código de versión actual de la aplicación.
     */
    private fun getCurrentVersionCode(): Long {
        val packageManager = context.packageManager
        val packageName = context.packageName
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("UpdateManager", "Error al obtener la versión actual: ${e.message}")
            -1L
        }
    }

    /**
     * Muestra un diálogo de actualización al usuario.
     */
    private fun showUpdateDialog(versionInfo: VersionInfo) {
        AlertDialog.Builder(context)
            .setTitle("Nueva actualización disponible")
            .setMessage(
                "Versión: ${versionInfo.versionName}\n" +
                        "Notas: ${versionInfo.releaseNotes}\n\n" +
                        "¿Deseas descargar e instalar la actualización?"
            )
            .setPositiveButton("Descargar") { _, _ ->
                if (NetworkStatusHelper.isWifiConnected()) {
                    // Si está conectado a Wi-Fi, iniciar la descarga
                    startUpdateDownload(versionInfo)
                } else {
                    // Si está en datos móviles, mostrar advertencia
                    showDataWarningDialog(versionInfo)
                }
            }
            .setNegativeButton(SpannableString("Más tarde").apply {
                setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorBlack)), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }, null)
            .show()
    }

    /**
     * Muestra un diálogo de advertencia si el usuario está conectado por datos móviles.
     */
    private fun showDataWarningDialog(versionInfo: VersionInfo) {
        AlertDialog.Builder(context)
            .setTitle("Advertencia: Conexión por Datos Móviles")
            .setMessage(
                "Actualmente estás conectado a datos móviles. La descarga podría generar costos adicionales. " +
                        "¿Deseas continuar o programar la descarga para cuando haya conexión Wi-Fi?"
            )
            .setPositiveButton("Continuar") { _, _ ->
                // Continuar con la descarga
                startUpdateDownload(versionInfo)
            }
            .setNegativeButton(SpannableString("Programar para Wi-Fi").apply {
                setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorBlack)), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }) { _, _ ->
                // Guardar el estado de la descarga pendiente
                scheduleDownloadOnWifi(versionInfo)
            }
            .show()
    }

    /**
     * Inicia la descarga de la actualización.
     */
    fun startUpdateDownload(versionInfo: VersionInfo) {
        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                workDataOf(
                    "apkUrl" to versionInfo.apkUrl,
                    "versionName" to versionInfo.versionName,
                    "releaseNotes" to versionInfo.releaseNotes
                )
            )
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)

        // Informar al usuario que la descarga ha comenzado
        Toast.makeText(context, "Descargando actualización...", Toast.LENGTH_SHORT).show()
    }

    /**
     * Programa la descarga para cuando haya conexión Wi-Fi.
     */
    private fun scheduleDownloadOnWifi(versionInfo: VersionInfo) {
        val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putString("apkUrl", versionInfo.apkUrl)
            putString("versionName", versionInfo.versionName)
            putString("releaseNotes", versionInfo.releaseNotes)
            putInt("versionCode", versionInfo.versionCode)
            apply()
        }

        Toast.makeText(context, "La descarga iniciará al conectarse a Wi-Fi", Toast.LENGTH_SHORT).show()
    }

    /**
     * Maneja la programación de descarga pendiente.
     */
    fun handlePendingDownload() {
        val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        val apkUrl = prefs.getString("apkUrl", null)
        if (apkUrl != null) {
            val versionInfo = VersionInfo(
                versionCode = prefs.getInt("versionCode", 0),
                versionName = prefs.getString("versionName", "1.0.0") ?: "1.0.0",
                releaseNotes = prefs.getString("releaseNotes", "Notas no disponibles")
                    ?: "Notas no disponibles",
                apkUrl = apkUrl
            )

            // Inicia la descarga
            startUpdateDownload(versionInfo)

            // Limpia la programación de descarga pendiente
            prefs.edit().clear().apply()
        }
    }
}
