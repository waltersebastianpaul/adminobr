package com.example.adminobr.update

import android.app.Notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.adminobr.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException


class DownloadService : Service() {


    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var downloadJob: Job? = null


    companion object {
        private const val CHANNEL_ID = "download_channel"
        private const val NOTIFICATION_ID = 1
    }


    override fun onBind(intent: Intent?): IBinder? = null


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val apkUrl = intent?.getStringExtra("apkUrl")
        if (apkUrl != null) {
            createNotificationChannel()


            val notification = createNotification()

            // Utilizar startForeground() con dos parámetros
            startForeground(NOTIFICATION_ID, notification)

            downloadJob = serviceScope.launch {
                try {
                    val apkUri = downloadApk(apkUrl)
                    if (apkUri != null) {
                        // Descarga completada, mostrar notificación o instalar
                        val updateManager = UpdateManager(this@DownloadService)
                        updateManager.handleUpdate(apkUri)
                    } else {
                        // Error en la descarga
                        Log.e("DownloadService", "Error al descargar la actualización")
                    }
                } catch (e: IOException) {
                    Log.e("DownloadService", "Error de descarga: ${e.message}", e)
                } finally {
                    // Detener el servicio y eliminar la notificación después de la descarga
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        } else {
            stopSelf()
        }
        return START_NOT_STICKY
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Download Channel"
            val descriptionText = "Channel for download notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH //IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Descargando actualización")
            .setContentText("Descarga en progreso")
            .setSmallIcon(R.drawable.ic_download)
            .build()
    }


    private suspend fun downloadApk(apkUrl: String): Uri? {
        return withContext(Dispatchers.IO) {
            val updateManager = UpdateManager(this@DownloadService)
            updateManager.downloadUpdate(apkUrl)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        downloadJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}