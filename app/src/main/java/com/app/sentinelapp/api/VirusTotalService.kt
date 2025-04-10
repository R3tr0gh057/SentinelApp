package com.app.sentinelapp.api

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.File
import java.io.IOException

class VirusTotalService {
    private val client = OkHttpClient()
    private val apiKey = "04c033888124dd8289bbaded4ff55d662b5ebfc4d128400e56df5ab5007a0173"
    private val baseUrl = "https://www.virustotal.com/api/v3"

    fun scanUrl(url: String, callback: (Result<String>) -> Unit) {
        val requestBody = FormBody.Builder()
            .add("url", url)
            .build()

        val request = Request.Builder()
            .url("$baseUrl/urls")
            .post(requestBody)
            .addHeader("accept", "application/json")
            .addHeader("x-apikey", apiKey)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val jsonResponse = JSONObject(response.body?.string() ?: "")
                    val analysisId = jsonResponse.getJSONObject("data")
                        .getJSONObject("links")
                        .getString("self")
                        .substringAfterLast("/")
                    
                    // Get analysis results
                    getAnalysisResults(analysisId, callback)
                } catch (e: Exception) {
                    callback(Result.failure(e))
                }
            }
        })
    }

    fun scanFile(file: File, callback: (Result<String>) -> Unit) {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                RequestBody.create("application/octet-stream".toMediaTypeOrNull(), file)
            )
            .build()

        val request = Request.Builder()
            .url("$baseUrl/files")
            .post(requestBody)
            .addHeader("accept", "application/json")
            .addHeader("x-apikey", apiKey)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val jsonResponse = JSONObject(response.body?.string() ?: "")
                    val analysisId = jsonResponse.getJSONObject("data")
                        .getJSONObject("links")
                        .getString("self")
                        .substringAfterLast("/")
                    
                    // Get analysis results
                    getAnalysisResults(analysisId, callback)
                } catch (e: Exception) {
                    callback(Result.failure(e))
                }
            }
        })
    }

    private fun getAnalysisResults(analysisId: String, callback: (Result<String>) -> Unit) {
        val request = Request.Builder()
            .url("$baseUrl/analyses/$analysisId")
            .get()
            .addHeader("accept", "application/json")
            .addHeader("x-apikey", apiKey)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { 
                    callback(Result.success(it))
                } ?: callback(Result.failure(Exception("Empty response")))
            }
        })
    }
} 