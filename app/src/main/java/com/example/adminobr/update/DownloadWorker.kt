package com.example.adminobr.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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

class DownloadWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        val apkUrl = inputData.getString("apkUrl") ?: return Result.failure()
        val versionName = inputData.getString("versionName") ?: "1.0.0"
        val releaseNotes = inputData.getString("releaseNotes") ?: "Notas no disponibles"
        val versionCode = inputData.getInt("versionCode", 0)

        val versionInfo = VersionInfo(versionCode, versionName, releaseNotes, apkUrl)

        return try {
            startForegroundService()

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                downloadToMediaStore(apkUrl)
            } else {
                downloadToLegacyFileSystem(apkUrl)
            }

            if (uri != null) {
                showInstallNotification(uri, versionInfo)
                Result.success()
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e("DownloadWorker", "Error durante la descarga: ${e.message}", e)
            Result.retry()
        } finally {
            stopForegroundService()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "download_channel",
                "Descargas",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, "download_channel")
            .setContentTitle("Descargando actualización")
            .setContentText("La descarga está en progreso")
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(true)
            .build()

        val intent = Intent(context, DownloadForegroundService::class.java)
        context.startForegroundService(intent)
    }

    private fun stopForegroundService() {
        val intent = Intent(context, DownloadForegroundService::class.java)
        context.stopService(intent)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun downloadToMediaStore(apkUrl: String): Uri? {
        val resolver = context.contentResolver

        // Elimina archivos previos con el mismo nombre
        resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
            arrayOf("adminobr.apk"),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                resolver.delete(
                    Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString()),
                    null,
                    null
                )
            }
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "adminobr.apk")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                if (!downloadFile(apkUrl, outputStream)) {
                    resolver.delete(it, null, null) // Elimina el archivo si la descarga falla
                }
            }
        }
        return uri
    }

    private fun downloadToLegacyFileSystem(apkUrl: String): Uri? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val apkFile = File(downloadsDir, "adminobr.apk")

        if (apkFile.exists()) {
            apkFile.delete() // Elimina el archivo existente si ya existe
        }

        return try {
            apkFile.outputStream().use { outputStream ->
                if (downloadFile(apkUrl, outputStream)) {
                    Uri.fromFile(apkFile)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("DownloadWorker", "Error en la descarga (Legacy): ${e.message}", e)
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
                Log.e("DownloadWorker", "Error en la descarga: ${response.code}")
                false
            }
        } catch (e: Exception) {
            Log.e("DownloadWorker", "Error al descargar archivo: ${e.message}", e)
            false
        }
    }

    private fun showInstallNotification(apkUri: Uri, versionInfo: VersionInfo) {
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

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        val installPendingIntent = PendingIntent.getActivity(
            context,
            0,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(
                "Versión: ${versionInfo.versionName}\n" +
                        "Notas: ${versionInfo.releaseNotes}\n\n" +
                        "Pulse 'Instalar' para completar la actualización."
            )
            .setBigContentTitle("Actualización lista para instalar")

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_mini_logo)
            .setContentTitle("Actualización descargada")
            .setContentText("Pulse para instalar")
            .setStyle(bigTextStyle)
            .setContentIntent(installPendingIntent)
            .addAction(
                R.drawable.ic_update,
                "Instalar",
                installPendingIntent
            )
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2, notification)
    }
}