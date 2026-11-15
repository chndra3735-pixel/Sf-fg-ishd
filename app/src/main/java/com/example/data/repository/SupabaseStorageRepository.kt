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
