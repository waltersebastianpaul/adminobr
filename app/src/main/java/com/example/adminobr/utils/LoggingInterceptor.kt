//package com.example.adminobr.utils
//
//import okhttp3.Interceptor
//import okhttp3.Response
//import android.util.Log
//import okio.Buffer // Asegúrate de importar la clase Buffer correcta
//import java.io.IOException
//
//class LoggingInterceptor : Interceptor {
//
//    @Throws(IOException::class)
//    override fun intercept(chain: Interceptor.Chain): Response {
//        val request = chain.request()
//        val requestBody = request.body
//
//        val t1 = System.nanoTime()
//        Log.d("API Request", String.format("Sending request %s on %s%n%s",
//            request.url, chain.connection(), request.headers))
//
//        if (requestBody != null) {
//            val buffer = Buffer()
//            requestBody.writeTo(buffer)
//
//            val contentType = requestBody.contentType()
//            if (contentType != null) {
//                Log.d("API Request", "Content-Type: " + contentType)
//            }
//
//            Log.d("API Request", "Content-Length: " + requestBody.contentLength())
//            Log.d("API Request", buffer.readUtf8())
//        }
//
//        val response = chain.proceed(request)
//        val t2 = System.nanoTime()
//
//        val responseBody = response.body
//        val contentLength = responseBody?.contentLength()
//
//        Log.d("API Response", String.format("Received response for %s in %.1fms%n%s",
//            response.request.url, (t2 - t1) / 1e6, response.headers))
//
//        if (responseBody != null && contentLength != 0L) {
//            Log.d("API Response", responseBody.string())
//        }
//
//        return response
//    }
//}