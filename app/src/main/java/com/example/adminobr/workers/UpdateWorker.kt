//package com.example.adminobr.workers
//
//import android.app.ActivityManager
//import android.content.Context
//import android.util.Log
//import androidx.work.CoroutineWorker
//import androidx.work.WorkerParameters
//import com.example.adminobr.data.VersionInfo
//import com.example.adminobr.update.UpdateManager
//import com.example.adminobr.update.UpdateService
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//
//class UpdateWorker(appContext: Context, workerParams: WorkerParameters) :
//    CoroutineWorker(appContext, workerParams) {
//
//    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
//        Log.d("UpdateWorker", "El Worker se ha iniciado correctamente")
//
//        try {
//
//
//            val updateManager = UpdateManager(applicationContext)
//            if (updateManager.checkForUpdates()) {
//                // Obtener la información de la última versión
//                val updateService = createUpdateService()
//                val latestVersion: VersionInfo = updateService.getLatestVersion()
//
//
//                // Iniciar la descarga
//                val apkUri = updateManager.downloadUpdate(latestVersion.apkUrl)
//
//
//                if (apkUri != null) {
//                    updateManager.handleUpdate(apkUri)
//                    return@withContext Result.success()
//                } else {
//                    return@withContext Result.retry()
//                }
//            } else {
//                // No hay actualizaciones disponibles
//                return@withContext Result.success()
//            }
//
//        } catch (e: Exception) {
//            Log.e("UpdateWorker", "Error al manejar la actualización: ${e.message}", e)
//            return@withContext Result.failure()
//        }
//
//
//
//
//    }
//
//    private fun createUpdateService(): UpdateService {
//        return Retrofit.Builder()
//            .baseUrl("http://adminobr.site/updates/apk/")
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//            .create(UpdateService::class.java)
//    }
//
//    private fun isAppInForeground(): Boolean {
//        val activityManager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
//        val appProcesses = activityManager.runningAppProcesses
//        if (appProcesses != null) {
//            val packageName = applicationContext.packageName
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
