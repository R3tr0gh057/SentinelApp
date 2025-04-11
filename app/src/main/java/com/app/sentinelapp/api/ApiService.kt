package com.app.sentinelapp.api

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object ApiService {
    private const val BASE_URL = "https://www.virustotal.com/api/v3"
    private const val API_KEY = "04c033888124dd8289bbaded4ff55d662b5ebfc4d128400e56df5ab5007a0173"
    private val client = OkHttpClient()

    suspend fun scanFile(file: File): ScanResponse {
        // First request to upload file
        val uploadResponse = uploadFile(file)
        val analysisUrl = uploadResponse.getString("data")
            .let { JSONObject(it) }
            .getJSONObject("links")
            .getString("self")

        // Poll for results
        return pollForResults(analysisUrl)
    }

    suspend fun scanUrl(url: String): ScanResponse {
        // First request to submit URL
        val submitResponse = submitUrl(url)
        val analysisUrl = submitResponse.getString("data")
            .let { JSONObject(it) }
            .getJSONObject("links")
            .getString("self")

        // Poll for results
        return pollForResults(analysisUrl)
    }

    suspend fun getFileScanResult(analysisUrl: String): ScanResponse {
        val request = Request.Builder()
            .url(analysisUrl)
            .addHeader("accept", "application/json")
            .addHeader("x-apikey", API_KEY)
            .get()
            .build()

        val response = makeRequest(request)
        return parseScanResponse(response)
    }

    suspend fun getUrlScanResult(analysisUrl: String): ScanResponse {
        val request = Request.Builder()
            .url(analysisUrl)
            .addHeader("accept", "application/json")
            .addHeader("x-apikey", API_KEY)
            .get()
            .build()

        val response = makeRequest(request)
        return parseScanResponse(response)
    }

    private suspend fun uploadFile(file: File): JSONObject {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody("application/octet-stream".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/files")
            .addHeader("accept", "application/json")
            .addHeader("x-apikey", API_KEY)
            .post(requestBody)
            .build()

        return makeRequest(request)
    }

    private suspend fun submitUrl(url: String): JSONObject {
        val requestBody = "url=$url".toRequestBody("application/x-www-form-urlencoded".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/urls")
            .addHeader("accept", "application/json")
            .addHeader("content-type", "application/x-www-form-urlencoded")
            .addHeader("x-apikey", API_KEY)
            .post(requestBody)
            .build()

        return makeRequest(request)
    }

    private suspend fun pollForResults(analysisUrl: String): ScanResponse {
        val request = Request.Builder()
            .url(analysisUrl)
            .addHeader("accept", "application/json")
            .addHeader("x-apikey", API_KEY)
            .get()
            .build()

        val response = makeRequest(request)
        return parseScanResponse(response)
    }

    private suspend fun makeRequest(request: Request): JSONObject = suspendCoroutine { continuation ->
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        continuation.resumeWithException(
                            IOException("Unexpected response ${response.code}")
                        )
                        return
                    }

                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        continuation.resume(JSONObject(responseBody))
                    } else {
                        continuation.resumeWithException(
                            IOException("Empty response body")
                        )
                    }
                }
            }
        })
    }

    private fun parseScanResponse(json: JSONObject): ScanResponse {
        val data = json.getJSONObject("data")
        val attributes = data.getJSONObject("attributes")
        val status = attributes.getString("status")
        val results = attributes.getJSONObject("results")
        val stats = attributes.getJSONObject("stats")
        val meta = json.getJSONObject("meta")

        val scanResults = mutableListOf<ScanResult>()
        results.keys().forEach { key ->
            val result = results.getJSONObject(key)
            scanResults.add(
                ScanResult(
                    engineName = result.getString("engine_name"),
                    category = result.getString("category"),
                    result = result.optString("result", "N/A"),
                    isThreat = result.getString("category") in listOf("malicious", "suspicious")
                )
            )
        }

        val scanStats = ScanStats(
            malicious = stats.optInt("malicious", 0),
            suspicious = stats.optInt("suspicious", 0),
            undetected = stats.optInt("undetected", 0),
            harmless = stats.optInt("harmless", 0),
            timeout = stats.optInt("timeout", 0),
            failure = stats.optInt("failure", 0),
            typeUnsupported = stats.optInt("type-unsupported", 0)
        )

        val fileInfo = meta.optJSONObject("file_info")?.let {
            FileInfo(
                sha256 = it.getString("sha256"),
                md5 = it.optString("md5", ""),
                sha1 = it.optString("sha1", ""),
                size = it.optLong("size", 0)
            )
        }

        val urlInfo = meta.optJSONObject("url_info")?.let {
            UrlInfo(
                id = it.getString("id"),
                url = it.getString("url")
            )
        }

        return ScanResponse(
            status = status,
            results = scanResults,
            stats = scanStats,
            fileInfo = fileInfo,
            urlInfo = urlInfo
        )
    }
}

data class ScanResponse(
    val status: String,
    val results: List<ScanResult>,
    val stats: ScanStats,
    val fileInfo: FileInfo? = null,
    val urlInfo: UrlInfo? = null
)

data class ScanResult(
    val engineName: String,
    val category: String,
    val result: String,
    val isThreat: Boolean
)

data class ScanStats(
    val malicious: Int,
    val suspicious: Int,
    val undetected: Int,
    val harmless: Int,
    val timeout: Int,
    val failure: Int = 0,
    val typeUnsupported: Int = 0
)

data class FileInfo(
    val sha256: String,
    val md5: String,
    val sha1: String,
    val size: Long
)

data class UrlInfo(
    val id: String,
    val url: String
) 