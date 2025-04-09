package com.app.sentinelapp.services

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class VirusTotalService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiKey = "YOUR_API_KEY" // Replace with your actual API key
    private val baseUrl = "https://www.virustotal.com/vtapi/v2"

    interface ScanCallback {
        fun onSuccess(response: String)
        fun onError(error: String)
    }

    fun scanFile(file: File, callback: ScanCallback) {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody("application/octet-stream".toMediaType())
            )
            .addFormDataPart("apikey", apiKey)
            .build()

        val request = Request.Builder()
            .url("$baseUrl/file/scan")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("VirusTotalService", "File scan failed", e)
                callback.onError("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    callback.onSuccess(responseBody)
                } else {
                    callback.onError("API error: ${response.code}")
                }
            }
        })
    }

    fun getFileReport(resource: String, callback: ScanCallback) {
        val formBody = FormBody.Builder()
            .add("resource", resource)
            .add("apikey", apiKey)
            .build()

        val request = Request.Builder()
            .url("$baseUrl/file/report")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("VirusTotalService", "File report failed", e)
                callback.onError("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    callback.onSuccess(responseBody)
                } else {
                    callback.onError("API error: ${response.code}")
                }
            }
        })
    }

    fun scanUrl(url: String, callback: ScanCallback) {
        val formBody = FormBody.Builder()
            .add("url", url)
            .add("apikey", apiKey)
            .build()

        val request = Request.Builder()
            .url("$baseUrl/url/scan")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("VirusTotalService", "URL scan failed", e)
                callback.onError("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    callback.onSuccess(responseBody)
                } else {
                    callback.onError("API error: ${response.code}")
                }
            }
        })
    }

    fun getUrlReport(resource: String, callback: ScanCallback) {
        val formBody = FormBody.Builder()
            .add("resource", resource)
            .add("apikey", apiKey)
            .build()

        val request = Request.Builder()
            .url("$baseUrl/url/report")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("VirusTotalService", "URL report failed", e)
                callback.onError("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    callback.onSuccess(responseBody)
                } else {
                    callback.onError("API error: ${response.code}")
                }
            }
        })
    }
} 