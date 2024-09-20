package com.example.adminobr.update

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.adminobr.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.adminobr.data.VersionInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

import com.example.adminobr.utils.Constants
import com.example.adminobr.utils.SessionManager

class UpdateManager(private val context: Context) {

    private val downloadScope = CoroutineScope(Dispatchers.IO + Job())
    private val sessionManager = SessionManager(context)
    private val isDebuggable = sessionManager.getDebuggable()

    private var updateUrl = Constants.Update.UPDATE_DIR

    suspend fun checkForUpdates(): Boolean {

        Log.d("UpdateManager", "Iniciando checkForUpdates()")
        Log.d("UpdateManager", "updateUrl $updateUrl")
        Log.d("UpdateManager", "isDebuggable $isDebuggable")
        return try {
            val updateService = Retrofit.Builder()
                .baseUrl(updateUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(UpdateService::class.java)

            val latestVersion = updateService.getLatestVersion()
            val currentVersionCode = getCurrentVersionCode()
            Log.d("UpdateManager", "latestVersion.versionCode ${latestVersion.versionCode}")
            Log.d("UpdateManager", "currentVersionCode $currentVersionCode")

            latestVersion.versionCode > currentVersionCode
        } catch (e: Exception) {
            Log.e("UpdateManager", "Error al comprobar actualizaciones: ${e.message}", e)
            Toast.makeText(context, "Error al comprobar actualizaciones", Toast.LENGTH_SHORT).show()
            false
        }
    }

    suspend fun downloadUpdate(apkUrl: String): Uri? {
        return withContext(Dispatchers.IO) {
            var uri: Uri? = null
            try {
                val resolver = context.applicationContext.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, if (isDebuggable) "app-debug.apk" else "app-release.apk")
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                }

                uri = resolver.insert(getDownloadDirectoryUri()!!, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        val client = OkHttpClient()
                        val request = Request.Builder().url(apkUrl).build()
                        val response = client.newCall(request).execute()
                        if (response.isSuccessful) {
                            response.body?.byteStream()?.use { inputStream ->
                                inputStream.copyTo(outputStream)
                                Log.d("UpdateManager", "Descarga de APK - Completada")
                            }
                        } else {
                            Log.e("UpdateManager", "Error en la descarga: ${response.code} - ${response.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("UpdateManager", "Error general en downloadAndInstallUpdate: ${e.message}", e)
            }
            uri
        }
    }
    suspend fun handleUpdate(apkUri: Uri) {
        if (isAppInForeground()) {
            installApk(apkUri)
        } else if (areNotificationsEnabled()) {
            showInstallNotification(apkUri)
        } else {
            // Las notificaciones están desactivadas, mostrar un diálogo
            showUpdateDialog(apkUri)
        }
    }

    suspend fun getLatestVersion(): VersionInfo {

        val updateService = Retrofit.Builder()
            .baseUrl(updateUrl).addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UpdateService::class.java)

        return updateService.getLatestVersion()
    }

    private fun areNotificationsEnabled(): Boolean {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.areNotificationsEnabled()
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    private suspend fun showUpdateDialog(apkUri: Uri) {
        // Mostrar un diálogo informando al user que hay una actualización disponible
        // y ofreciendo la opción de instalarla
        withContext(Dispatchers.Main) {
            AlertDialog.Builder(context)
                .setTitle("Actualización disponible")
                .setMessage("Hay una nueva actualización disponible. ¿Desea instalarla ahora?")
                .setPositiveButton("Sí") { _, _ ->
                    installApk(apkUri)
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun showInstallNotification(apkUri: Uri) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "update_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Actualizaciones",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)}


        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Actualización descargada")
            .setContentText("Pulse para instalar")
            .setSmallIcon(R.drawable.ic_download_done)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2, notification) // Usamos un ID diferente para esta notificación
    }

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
            Log.e("UpdateManager", "Error al obtener la versión actual: ${e.message}", e)
            -1L
        }
    }

    private fun isAppInForeground(): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses
        if (appProcesses != null) {
            val packageName = context.packageName
            for (appProcess in appProcesses) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && appProcess.processName == packageName) {
                    return true
                }
            }
        }
        return false
    }

    fun installApk(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }

    private fun getDownloadDirectoryUri(): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
        }
    }
}
