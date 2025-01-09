package com.example.adminobr.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.adminobr.R
import com.example.adminobr.data.VersionInfo
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.OutputStream
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.adminobr.application.MyApplication

class DownloadWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun doWork(): Result {
        val apkUrl = inputData.getString("apkUrl") ?: return Result.failure()
        val versionName = inputData.getString("versionName") ?: "1.0.0"
        val releaseNotes = inputData.getString("releaseNotes") ?: "Notas no disponibles"
        val versionCode = inputData.getInt("versionCode", 0)

        val versionInfo = VersionInfo(versionCode, versionName, releaseNotes, apkUrl)

        return try {
            startForegroundService() // Inicia el servicio antes de cualquier operación pesada.

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                downloadToMediaStore(apkUrl)
            } else {
                downloadToLegacyFileSystem(apkUrl)
            }

            if (uri != null) {
                if (isAppInForeground()) {
                    // Llama a las funciones del companion object correctamente
                    installApk(this.context, uri) // Pasa el contexto explícito
                    clearPendingInstallation(this.context) // Pasa el contexto explícito
                    removeNotification(this.context) // Pasa el contexto explícito
                } else {
                    Log.d("DownloadWorker", "App en segundo plano, marcando como pendiente de instalación.")
                    markPendingInstallation(uri)
                    showInstallNotification(versionInfo)
                }
                Result.success()
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e("DownloadWorker", "Error durante la descarga: ${e.message}", e)
            Result.retry()
        } finally {
            stopForegroundService() // Detiene el servicio al finalizar.
        }

    }

    private fun markPendingInstallation(uri: Uri) {
        val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putString("pending_installation_uri", uri.toString())
            apply()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startForegroundService() {
        val intent = Intent(context, DownloadForegroundService::class.java)
        ContextCompat.startForegroundService(context, intent)

        // Asegúrate de que se envía la notificación inmediatamente
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "download_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Descargas",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Descargando actualización")
            .setContentText("Por favor, espere mientras se completa la descarga")
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(true)
            .build()

        notificationManager.notify(DownloadForegroundService.NOTIFICATION_ID, notification)
    }

    private fun stopForegroundService() {
        val intent = Intent(context, DownloadForegroundService::class.java)
        context.stopService(intent)
        Log.d("DownloadWorker", "Foreground service detenido correctamente.")
    }

    // Modificación en la función para MediaStore (Android Q+)
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun downloadToMediaStore(apkUrl: String): Uri? {
        val resolver = context.contentResolver

        // Validación y eliminación previa
        resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
            arrayOf("${APK_FILE_NAME.removeSuffix(".apk")}%"),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                val uri = Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString())
                val deleted = resolver.delete(uri, null, null)
                if (deleted > 0) {
                    Log.d("DownloadWorker", "Archivo eliminado: $uri")
                } else {
                    Log.e("DownloadWorker", "No se pudo eliminar: $uri")
                }
            }
        }

        // Crear archivo para la descarga
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, APK_FILE_NAME)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                if (!downloadFile(apkUrl, outputStream)) {
                    resolver.delete(it, null, null) // Eliminar si falla la descarga
                    Log.e("DownloadWorker", "Error durante la descarga. Archivo eliminado.")
                }
            }
        }
        return uri
    }

    // Función para eliminar archivos existentes y duplicados
    private fun deleteExistingApkFiles(downloadsDir: File) {
        val files = downloadsDir.listFiles { file ->
            file.name.startsWith(APK_FILE_NAME.removeSuffix(".apk")) && file.name.endsWith(".apk")
        }

        files?.forEach { file ->
            if (file.delete()) {
                Log.d("DownloadWorker", "Archivo eliminado: ${file.name}")
            } else {
                Log.e("DownloadWorker", "No se pudo eliminar el archivo: ${file.name}")
            }
        }
    }

    // Modificación en la función para el sistema de archivos legado
    private fun downloadToLegacyFileSystem(apkUrl: String): Uri? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        // Eliminar archivos previos
        deleteExistingApkFiles(downloadsDir)

        val apkFile = File(downloadsDir, APK_FILE_NAME)

        return try {
            apkFile.outputStream().use { outputStream ->
                if (downloadFile(apkUrl, outputStream)) {
                    Uri.fromFile(apkFile)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("DownloadWorker", "Error al manejar el archivo: ${e.message}", e)
            null
        }
    }

    private fun downloadFile(apkUrl: String, outputStream: OutputStream): Boolean {
        return try {
            val client = OkHttpClient()
            val request = Request.Builder().url(apkUrl).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                response.body?.byteStream()?.use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
                true
            } else {
                Log.e("DownloadWorker", "Error en la descarga: Código HTTP ${response.code}")
                false
            }
        } catch (e: Exception) {
            Log.e("DownloadWorker", "Error al descargar archivo: ${e.message}", e)
            false
        }
    }

    private fun showInstallNotification(versionInfo: VersionInfo) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "update_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Actualización de la App",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para actualizaciones"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(
                "Versión: ${versionInfo.versionName}\n" +
                        "Notas: ${versionInfo.releaseNotes}\n\n" +
                        "La descarga de la actualización se ha completado."
            )
            .setBigContentTitle("Descarga completada")

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_mini_logo) // Usa un ícono apropiado
            .setContentTitle("Actualización descargada")
            .setContentText("La descarga de la actualización se ha completado.")
            .setStyle(bigTextStyle)
            .setAutoCancel(true) // La notificación se elimina al pulsarla
            .build()

        notificationManager.notify(2, notification)
    }

    private fun isAppInForeground(): Boolean {
        val app = context.applicationContext as? MyApplication
        return app?.isAppInForeground == true
    }

    companion object {

        private const val APK_FILE_NAME = "adminobr.apk"

        fun installApk(context: Context, uri: Uri) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        }

        fun clearPendingInstallation(context: Context) {
            val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
            with(prefs.edit()) {
                remove("pending_installation_uri")
                apply()
            }
        }

        fun removeNotification(context: Context) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancelAll()
        }
    }

}
