package com.example.adminobr.update

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.adminobr.R

class DownloadForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "download_foreground_channel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // El servicio no necesita realizar acciones específicas.
        // La lógica de descarga se maneja en el `DownloadWorker`.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
    }

    override fun onBind(intent: Intent?): IBinder? {
        // No se usa para servicios en primer plano sin comunicación directa
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Descarga en Progreso",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Descargando actualización")
            .setContentText("Por favor, espere mientras se completa la descarga")
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(true) // Impide que el usuario la elimine manualmente
            .build()
    }
}
