//package com.example.adminobr.update
//
//import android.app.*
//import android.content.*
//import android.content.pm.PackageManager
//import android.net.Uri
//import android.os.Build
//import android.os.Environment
//import android.provider.MediaStore
//import android.util.Log
//import android.widget.Toast
//import android.Manifest
//import androidx.appcompat.app.AlertDialog
//import androidx.core.app.NotificationCompat
//import androidx.core.app.NotificationManagerCompat
//import com.example.adminobr.R
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import okhttp3.OkHttpClient
//import okhttp3.Request
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//import com.example.adminobr.data.VersionInfo
//import com.example.adminobr.utils.Constants
//import com.example.adminobr.utils.SessionManager
//
//class UpdateManager(private val context: Context) {
//
//    private val sessionManager = SessionManager(context)
//    private val isDebuggable = sessionManager.isDebuggable()
//
//    //
//    private var updateUrl = Constants.Update.RELEASE_DIR
//
//    suspend fun checkForUpdates(): Boolean {
//        if(isDebuggable){
//            updateUrl = Constants.Update.DEBUG_DIR
//        }
//        Log.d("UpdateManager", "Iniciando checkForUpdates()")
//        Log.d("UpdateManager", "updateUrl $updateUrl")
//        Log.d("UpdateManager", "isDebuggable $isDebuggable")
//        return try {
//            val updateService = Retrofit.Builder()
//                .baseUrl(updateUrl)
//                .addConverterFactory(GsonConverterFactory.create())
//                .build()
//                .create(UpdateService::class.java)
//
//            val latestVersion = updateService.getLatestVersion()
//            val currentVersionCode = getCurrentVersionCode()
//            Log.d("UpdateManager", "latestVersion.versionCode ${latestVersion.versionCode}")
//            Log.d("UpdateManager", "currentVersionCode $currentVersionCode")
//
//            latestVersion.versionCode > currentVersionCode
//        } catch (e: Exception) {
//            Log.e("UpdateManager", "Error al comprobar actualizaciones: ${e.message}", e)
//            Toast.makeText(context, "Error al comprobar actualizaciones", Toast.LENGTH_SHORT).show()
//            false
//        }
//    }
//
//    suspend fun downloadUpdate(apkUrl: String): Uri? {
//        return withContext(Dispatchers.IO) {
//            var uri: Uri? = null
//            try {
//                val resolver = context.applicationContext.contentResolver
//                val contentValues = ContentValues().apply {
//                    put(MediaStore.MediaColumns.DISPLAY_NAME, "app-release.apk")
//                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
//                    }
//                }
//
//                uri = resolver.insert(getDownloadDirectoryUri()!!, contentValues)
//                uri?.let {
//                    resolver.openOutputStream(it)?.use { outputStream ->
//                        val client = OkHttpClient()
//                        val request = Request.Builder().url(apkUrl).build()
//                        val response = client.newCall(request).execute()
//                        if (response.isSuccessful) {
//                            response.body?.byteStream()?.use { inputStream ->
//                                inputStream.copyTo(outputStream)
//                                Log.d("UpdateManager", "Descarga de APK - Completada")
//                            }
//                        } else {
//                            Log.e("UpdateManager", "Error en la descarga: ${response.code} - ${response.message}")
//                        }
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e("UpdateManager", "Error general en downloadAndInstallUpdate: ${e.message}", e)
//            }
//            uri
//        }
//    }
//
//    suspend fun handleUpdate(apkUri: Uri) {
//        if (isAppInForeground()) {
//            installApk(apkUri)
//        } else if (areNotificationsEnabled()) {
//            showInstallNotification(apkUri)
//        } else {
//            showUpdateDialog(apkUri)
//        }
//    }
//
//    suspend fun getLatestVersion(): VersionInfo {
//        if(isDebuggable){
//            updateUrl = Constants.Update.DEBUG_DIR
//        }
//        val updateService = Retrofit.Builder()
//            .baseUrl(updateUrl)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//            .create(UpdateService::class.java)
//
//        return updateService.getLatestVersion()
//    }
//
//    private fun areNotificationsEnabled(): Boolean {
//        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            notificationManager.areNotificationsEnabled()
//        } else {
//            NotificationManagerCompat.from(context).areNotificationsEnabled()
//        }
//    }
//
//    private suspend fun showUpdateDialog(apkUri: Uri) {
//        withContext(Dispatchers.Main) {
//            AlertDialog.Builder(context)
//                .setTitle("Actualización disponible")
//                .setMessage("Hay una nueva actualización disponible. ¿Desea instalarla ahora?")
//                .setPositiveButton("Sí") { _, _ ->
//                    installApk(apkUri)
//                }
//                .setNegativeButton("Instalar Más Tarde") { _, _ ->
//                    showInstallLaterNotification(apkUri)
//                }
//                .show()
//        }
//    }
//
//    private fun showInstallNotification(apkUri: Uri) {
//        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        val channelId = "update_channel"
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                channelId,
//                "Actualizaciones",
//                NotificationManager.IMPORTANCE_HIGH
//            )
//            notificationManager.createNotificationChannel(channel)
//        }
//
//        val installIntent = Intent(Intent.ACTION_VIEW).apply {
//            setDataAndType(apkUri, "application/vnd.android.package-archive")
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
//        }
//        val pendingIntent = PendingIntent.getActivity(
//            context,
//            0,
//            installIntent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val notification = NotificationCompat.Builder(context, channelId)
//            .setContentTitle("Actualización descargada")
//            .setContentText("Pulse para instalar")
//            .setSmallIcon(R.drawable.ic_download_done)
//            .setContentIntent(pendingIntent)
//            .setAutoCancel(true)
//            .build()
//
//        notificationManager.notify(2, notification)
//    }
//
////    private fun showInstallLaterNotification(apkUri: Uri) {
////        val retryIntent = Intent(context, RetryInstallReceiver::class.java).apply {
////            putExtra("apk_uri", apkUri.toString())
////        }
////
////        val retryPendingIntent = PendingIntent.getBroadcast(
////            context,
////            0,
////            retryIntent,
////            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
////        )
////
////        val builder = NotificationCompat.Builder(context, "update_channel")
////            .setSmallIcon(R.drawable.ic_download_done)
////            .setContentTitle("Instalación pendiente")
////            .setContentText("Toca para instalar la actualización.")
////            .setPriority(NotificationCompat.PRIORITY_HIGH)
////            .addAction(R.drawable.ic_update, "Instalar Ahora", retryPendingIntent)
////            .setAutoCancel(true)
////
////        val notificationManager = NotificationManagerCompat.from(context)
////
////        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
////            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
////                notificationManager.notify(3, builder.build())
////            } else {
////                // Manejo cuando no se tienen permisos. Ejemplo: mostrar un Toast o solicitar permisos.
////                Toast.makeText(context, "Permiso necesario para mostrar notificaciones", Toast.LENGTH_LONG).show()
////                // Si estás en una actividad, podrías solicitar el permiso aquí.
////            }
////        } else {
////            notificationManager.notify(3, builder.build()) // No se requiere permiso explícito en versiones anteriores
////        }
////    }
//
////    fun installApk(uri: Uri) {
////        val intent = Intent(Intent.ACTION_VIEW).apply {
////            setDataAndType(uri, "application/vnd.android.package-archive")
////            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
////        }
////        context.startActivity(intent)
////    }
//
//    private fun showInstallLaterNotification(apkUri: Uri) {
//        val retryIntent = Intent(context, RetryInstallReceiver::class.java).apply {
//            putExtra("apk_uri", apkUri.toString())
//        }
//
//        val retryPendingIntent = PendingIntent.getBroadcast(
//            context,
//            0,
//            retryIntent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val builder = NotificationCompat.Builder(context, "update_channel")
//            .setSmallIcon(R.drawable.ic_download_done)
//            .setContentTitle("Instalación pendiente")
//            .setContentText("Toca para instalar la actualización.")
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .addAction(R.drawable.ic_update, "Instalar Ahora", retryPendingIntent) // Aquí agregas la acción
//            .setAutoCancel(true)
//
//        val notificationManager = NotificationManagerCompat.from(context)
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
//                notificationManager.notify(3, builder.build())
//            } else {
//                // Manejo cuando no se tienen permisos. Ejemplo: mostrar un Toast o solicitar permisos.
//                Toast.makeText(context, "Permiso necesario para mostrar notificaciones", Toast.LENGTH_LONG).show()
//                // Si estás en una actividad, podrías solicitar el permiso aquí.
//            }
//        } else {
//            notificationManager.notify(3, builder.build()) // No se requiere permiso explícito en versiones anteriores
//        }
//
////        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
////            notificationManager.notify(3, builder.build())
////        } else {
////            // Solicitar permiso para notificaciones si no está concedido
////        }
//    }
//
//
//    fun installApk(uri: Uri) {
//        try {
//            val intent = Intent(Intent.ACTION_VIEW).apply {
//                setDataAndType(uri, "application/vnd.android.package-archive")
//                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
//            }
//            context.startActivity(intent)
//        } catch (e: SecurityException) {
//            // Maneja el conflicto de paquetes aquí
//            Log.e("UpdateManager", "Error de seguridad durante la instalación: ${e.message}", e)
//            showInstallFailureDialog(uri)
//        } catch (e: Exception) {
//            Log.e("UpdateManager", "Error general durante la instalación: ${e.message}", e)
//        }
//    }
//
//    private fun showInstallFailureDialog(apkUri: Uri) {
//        AlertDialog.Builder(context)
//            .setTitle("Error de instalación")
//            .setMessage("Parece que esta actualización no se puede instalar automáticamente debido a un conflicto con la versión actual. Te recomendamos desinstalar la versión actual manualmente e intentar instalar de nuevo.")
//            .setPositiveButton("Desinstalar") { _, _ ->
//                // Iniciar la desinstalación de la app actual
//                val packageUri = Uri.parse("package:${context.packageName}")
//                val uninstallIntent = Intent(Intent.ACTION_DELETE, packageUri).apply {
//                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                }
//                context.startActivity(uninstallIntent)
//
//                // Aquí podrías también lanzar una notificación para recordar al usuario que instale la nueva versión
//                showInstallLaterNotification(apkUri)
//            }
//            .setNegativeButton("Cancelar") { _, _ ->
//                // Si el usuario cancela, también puedes lanzar la notificación
//                showInstallLaterNotification(apkUri)
//            }
//            .show()
//    }
//
//
//    private fun getCurrentVersionCode(): Long {
//        val packageManager = context.packageManager
//        val packageName = context.packageName
//        return try {
//            val packageInfo = packageManager.getPackageInfo(packageName, 0)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//                packageInfo.longVersionCode
//            } else {
//                @Suppress("DEPRECATION")
//                packageInfo.versionCode.toLong()
//            }
//        } catch (e: PackageManager.NameNotFoundException) {
//            Log.e("UpdateManager", "Error al obtener la versión actual: ${e.message}", e)
//            -1L
//        }
//    }
//
//
//    private fun getDownloadDirectoryUri(): Uri? {
//        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            MediaStore.Downloads.EXTERNAL_CONTENT_URI
//        } else {
//            Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
//        }
//    }
//
//    private fun isAppInForeground(): Boolean {
//        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
//        val appProcesses = activityManager.runningAppProcesses
//        if (appProcesses != null) {
//            val packageName = context.packageName
//            for (appProcess in appProcesses) {
//                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
//                    && appProcess.processName == packageName) {
//                    return true
//                }
//            }
//        }
//        return false
//    }
//}
//
//// BroadcastReceiver para manejar la instalación desde la notificación
//class RetryInstallReceiver : BroadcastReceiver() {
//    override fun onReceive(context: Context, intent: Intent) {
//        val uriString = intent.getStringExtra("apk_uri")
//        val uri = Uri.parse(uriString)
//        val updateManager = UpdateManager(context)
//        updateManager.installApk(uri)
//    }
//}



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

import android.app.*
import android.content.*
import com.example.adminobr.utils.Constants
import com.example.adminobr.utils.SessionManager

class UpdateManager(private val context: Context) {

    private val downloadScope = CoroutineScope(Dispatchers.IO + Job())
    private val sessionManager = SessionManager(context)
    private val isDebuggable = sessionManager.getDebuggable()


    private var updateUrl = if (isDebuggable) {
        Constants.Update.DEBUG_DIR
    } else {
        Constants.Update.RELEASE_DIR
    }

    suspend fun checkForUpdates(): Boolean {
//        if(isDebuggable){
//            updateUrl = Constants.Update.DEBUG_DIR
//        } else {
//            updateUrl = Constants.Update.RELEASE_DIR
//        }
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
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "app-release.apk")
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
//        if(isDebuggable){
//            updateUrl = Constants.Update.DEBUG_DIR
//        } else {
//            updateUrl = Constants.Update.RELEASE_DIR
//        }
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
//    fun getCurrentVersionCode(): Int {
//        val packageManager = context.packageManager
//        val packageName = context.packageName
//        return try {
//            val packageInfo = packageManager.getPackageInfo(packageName, 0)
//            packageInfo.versionCode
//        } catch (e: PackageManager.NameNotFoundException) {
//            Log.e("UpdateManager", "Error al obtener la versión actual: ${e.message}", e)
//            -1 // O cualquier otro valor que indique un error
//        }
//    }

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
