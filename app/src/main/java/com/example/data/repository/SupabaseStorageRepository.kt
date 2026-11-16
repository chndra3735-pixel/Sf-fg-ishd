package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URI
import java.net.UnknownHostException
import java.util.UUID

/**
 * Handles media uploads (User DP, Room Covers, and Moment Videos/Photos) to Supabase Storage.
 * Metadata & real-time state stay in Firebase Firestore; binary media files live in Supabase Storage.
 */
class SupabaseStorageRepository(
  private val appContext: Context,
  private val httpClient: OkHttpClient = SupabaseNetworkClient.sharedOkHttpClient,
) {
  private val supabaseUrl: String
    get() = BuildConfig.SUPABASE_URL.trim().trimEnd('/')

  private val supabaseAnonKey: String
    get() {
      val raw = BuildConfig.SUPABASE_ANON_KEY.trim()
      return if (raw.startsWith("Sb_publishable_")) {
        "sb_publishable_" + raw.removePrefix("Sb_publishable_")
      } else {
        raw
      }
    }

  private val bucketName: String
    get() = BuildConfig.SUPABASE_BUCKET.trim().ifBlank { "swarroom-media" }

  fun isConfigured(): Boolean {
    return supabaseUrl.startsWith("https://") &&
      !supabaseUrl.contains("your-project-id") &&
      supabaseAnonKey.isNotBlank() &&
      supabaseAnonKey != "your-supabase-anon-key"
  }

  suspend fun uploadMediaFromUri(
    uri: Uri,
    folder: String,
    uid: String,
    isVideo: Boolean,
  ): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
      if (!isConfigured()) {
        throw IllegalStateException(
          "Supabase Storage कॉन्फ़िगर नहीं है। AI Studio Secrets या GitHub Secrets में SUPABASE_URL और SUPABASE_ANON_KEY जोड़ें।"
        )
      }

      val prepared = MediaCompressor.prepareForUpload(
        context = appContext,
        uri = uri,
        folder = folder,
        isVideo = isVideo,
      )

      try {
        val objectPath =
          "$folder/$uid/${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.${prepared.extension}"
        val uploadEndpoint = "$supabaseUrl/storage/v1/object/$bucketName/$objectPath"

        val requestBody = prepared.file.asRequestBody(prepared.mimeType.toMediaTypeOrNull())
        val requestBuilder = Request.Builder()
          .url(uploadEndpoint)
          .post(requestBody)
          .addHeader("apikey", supabaseAnonKey)
          .addHeader("Content-Type", prepared.mimeType)

        // Legacy anon/service JWT keys (eyJ...) require Authorization: Bearer, while newer
        // sb_publishable_* keys must only be sent in the apikey header.
        if (supabaseAnonKey.startsWith("eyJ")) {
          requestBuilder.addHeader("Authorization", "Bearer $supabaseAnonKey")
        }

        try {
          httpClient.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
              val errorBody = response.body?.string().orEmpty()
              Log.e(TAG, "Supabase upload failed (${response.code}): $errorBody")
              val serverMessage = extractSupabaseErrorMessage(errorBody)
              val friendlyReason = when {
                serverMessage.contains("Bucket not found", ignoreCase = true) ->
                  "बकेट '$bucketName' Supabase में नहीं मिला। Supabase Storage में '$bucketName' नाम का Public बकेट बनाएं।"
                serverMessage.contains("row-level security", ignoreCase = true) ||
                  serverMessage.contains("Unauthorized", ignoreCase = true) ->
                  "Supabase बकेट '$bucketName' पर अपलोड (INSERT) की RLS पॉलिसी चालू नहीं है।"
                serverMessage.contains("Invalid", ignoreCase = true) ||
                  serverMessage.contains("JWT", ignoreCase = true) ->
                  "Supabase API Key अमान्य है ($serverMessage)। सही Publishable/Anon Key जांचें।"
                serverMessage.isNotBlank() ->
                  "Supabase अपलोड विफल (${response.code}): $serverMessage"
                else ->
                  "Supabase अपलोड विफल (${response.code}): बकेट '$bucketName' व इसकी पब्लिक अपलोड पॉलिसी जांचें।"
              }
              throw IllegalStateException(friendlyReason)
            }
          }
        } catch (e: UnknownHostException) {
          val host = runCatching { URI(supabaseUrl).host.orEmpty() }.getOrDefault("")
          if (host.isNotEmpty() && SupabaseNetworkClient.wasHostNxDomain(host)) {
            throw IllegalStateException(
              "Supabase URL '$supabaseUrl' इंटरनेट पर नहीं मिला (गलत Project URL या प्रोजेक्ट बंद हो सकता है)। Supabase Dashboard से सही URL जांचें।",
              e,
            )
          }
          throw IllegalStateException(
            "Supabase सर्वर ($host) से DNS कनेक्शन नहीं हो सका। इंटरनेट या Private DNS जांचें।",
            e,
          )
        }

        "$supabaseUrl/storage/v1/object/public/$bucketName/$objectPath"
      } finally {
        runCatching { prepared.file.delete() }
      }
    }
  }

  suspend fun deleteMediaByPublicUrl(publicUrl: String): Result<Boolean> {
    return deleteMediaByPublicUrls(listOf(publicUrl)).map { it > 0 }
  }

  suspend fun deleteMediaByPublicUrls(publicUrls: Collection<String>): Result<Int> =
    withContext(Dispatchers.IO) {
      runCatching {
        if (!isConfigured()) return@runCatching 0
        val urlToPath = publicUrls
          .map { it.trim() }
          .filter { it.isNotBlank() }
          .distinct()
          .mapNotNull { url -> extractObjectPathFromPublicUrl(url)?.let { path -> url to path } }
        if (urlToPath.isEmpty()) return@runCatching 0

        val objectPaths = urlToPath.map { it.second }.distinct()
        val deleteEndpoint = "$supabaseUrl/storage/v1/object/$bucketName"
        val jsonBody = org.json.JSONObject()
          .put("prefixes", org.json.JSONArray(objectPaths))
          .toString()
        val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val requestBuilder = Request.Builder()
          .url(deleteEndpoint)
          .delete(requestBody)
          .addHeader("apikey", supabaseAnonKey)
          .addHeader("Content-Type", "application/json")

        if (supabaseAnonKey.startsWith("eyJ")) {
          requestBuilder.addHeader("Authorization", "Bearer $supabaseAnonKey")
        }

        var deletedCount = 0
        try {
          httpClient.newCall(requestBuilder.build()).execute().use { response ->
            val respBody = response.body?.string().orEmpty()
            if (response.code == 404) {
              deletedCount = objectPaths.size
            } else if (response.isSuccessful) {
              val arrLen = runCatching { org.json.JSONArray(respBody).length() }.getOrDefault(-1)
              if (arrLen >= objectPaths.size) {
                deletedCount = arrLen
              } else {
                // Batch DELETE under Postgres RLS returns 200 [] when DELETE policy is missing,
                // or may delete partially. Fall back to single-object DELETE to verify exact status.
                deletedCount = deleteObjectsIndividually(objectPaths)
                if (deletedCount == 0 && urlToPath.any { stillExistsAtPublicUrl(it.first) }) {
                  throw IllegalStateException(
                    "Supabase बकेट '$bucketName' पर डिलीट (DELETE) की RLS पॉलिसी चालू नहीं है।"
                  )
                }
                if (deletedCount == 0) {
                  deletedCount = objectPaths.size
                }
              }
            } else {
              Log.e(TAG, "Supabase batch delete failed (${response.code}): $respBody")
              val fallbackCount = deleteObjectsIndividually(objectPaths)
              if (fallbackCount > 0) {
                deletedCount = fallbackCount
              } else {
                val serverMessage = extractSupabaseErrorMessage(respBody)
                val friendlyReason = when {
                  serverMessage.contains("row-level security", ignoreCase = true) ||
                    serverMessage.contains("Unauthorized", ignoreCase = true) ||
                    response.code == 401 || response.code == 403 ->
                    "Supabase बकेट '$bucketName' पर डिलीट (DELETE) की RLS पॉलिसी चालू नहीं है।"
                  serverMessage.isNotBlank() ->
                    "Supabase डिलीट विफल (${response.code}): $serverMessage"
                  else ->
                    "Supabase डिलीट विफल (${response.code}): बकेट '$bucketName' की DELETE पॉलिसी जांचें।"
                }
                throw IllegalStateException(friendlyReason)
              }
            }
          }
        } catch (e: UnknownHostException) {
          val host = runCatching { URI(supabaseUrl).host.orEmpty() }.getOrDefault("")
          throw IllegalStateException(
            "Supabase सर्वर ($host) से कनेक्शन नहीं हो सका; पुरानी फ़ाइल हटाई नहीं जा सकी।",
            e,
          )
        }

        deletedCount
      }
    }

  private fun deleteObjectsIndividually(objectPaths: List<String>): Int {
    var deleted = 0
    for (path in objectPaths) {
      val singleUrl = "$supabaseUrl/storage/v1/object/$bucketName/$path"
      val req = Request.Builder()
        .url(singleUrl)
        .delete()
        .addHeader("apikey", supabaseAnonKey)
      if (supabaseAnonKey.startsWith("eyJ")) {
        req.addHeader("Authorization", "Bearer $supabaseAnonKey")
      }
      runCatching {
        httpClient.newCall(req.build()).execute().use { resp ->
          val body = resp.body?.string().orEmpty()
          if (resp.isSuccessful) {
            if (body.isBlank() || !body.trim().startsWith("[") ||
              runCatching { org.json.JSONArray(body).length() }.getOrDefault(1) > 0
            ) {
              deleted++
            }
          } else if (resp.code == 404) {
            deleted++
          } else {
            Log.w(TAG, "Single delete failed (${resp.code}) for $path: $body")
          }
        }
      }
    }
    return deleted
  }

  private fun stillExistsAtPublicUrl(publicUrl: String): Boolean {
    return runCatching {
      val headReq = Request.Builder().url(publicUrl).head().build()
      httpClient.newCall(headReq).execute().use { resp -> resp.isSuccessful }
    }.getOrDefault(false)
  }

  private fun extractObjectPathFromPublicUrl(publicUrl: String): String? {
    val cleanUrl = publicUrl.trim()
    if (cleanUrl.isBlank() || !isConfigured()) return null
    val marker = "/storage/v1/object/public/$bucketName/"
    val markerIdx = cleanUrl.indexOf(marker)
    if (markerIdx < 0) return null
    val basePart = cleanUrl.substring(0, markerIdx).trimEnd('/')
    val configuredHost = runCatching { URI(supabaseUrl).host.orEmpty() }.getOrDefault("")
    val urlHost = runCatching { URI(basePart).host.orEmpty() }.getOrDefault("")
    if (!basePart.equals(supabaseUrl, ignoreCase = true) &&
      (configuredHost.isBlank() || !urlHost.equals(configuredHost, ignoreCase = true))
    ) {
      return null
    }
    val objectPath = cleanUrl.substring(markerIdx + marker.length)
      .substringBefore('?')
      .substringBefore('#')
      .trimStart('/')
    return objectPath.takeIf { it.isNotBlank() }
  }

  private fun extractSupabaseErrorMessage(errorBody: String): String {
    if (errorBody.isBlank()) return ""
    return runCatching {
      val json = org.json.JSONObject(errorBody)
      val msg = json.optString("message").trim()
      val err = json.optString("error").trim()
      when {
        msg.isNotEmpty() -> msg
        err.isNotEmpty() -> err
        else -> errorBody.take(160)
      }
    }.getOrDefault(errorBody.take(160))
  }

  private companion object {
    const val TAG = "SupabaseStorage"
  }
}
