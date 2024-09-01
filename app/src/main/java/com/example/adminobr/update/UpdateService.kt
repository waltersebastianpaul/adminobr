package com.example.adminobr.update


import com.example.adminobr.data.VersionInfo
import retrofit2.http.GET

interface UpdateService {
    @GET("version.json")
    suspend fun getLatestVersion(): VersionInfo
}