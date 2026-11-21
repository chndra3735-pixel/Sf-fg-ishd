package com.example.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.abs

data class CapturedLocationResult(
  val label: String = "",
  val lat: Double = 0.0,
  val lng: Double = 0.0,
  val status: String = "pending",
) {
  val isValidCaptured: Boolean
    get() = status == "captured" &&
      label.isNotBlank() &&
      !(abs(lat) < 0.0001 && abs(lng) < 0.0001)
}

object AuditDateLocationHelper {
  private val DOB_REGEX = Regex("""^(\d{4})-(\d{2})-(\d{2})$""")
  private const val MAX_RECENT_FIX_AGE_MS = 15 * 60 * 1000L

  fun calculateAgeYears(
    dobIso: String,
    nowMs: Long = System.currentTimeMillis(),
  ): Int {
    val match = DOB_REGEX.matchEntire(dobIso.trim()) ?: return 0
    val year = match.groupValues[1].toIntOrNull() ?: return 0
    val month = match.groupValues[2].toIntOrNull() ?: return 0
    val day = match.groupValues[3].toIntOrNull() ?: return 0
    if (month !in 1..12 || day !in 1..31) return 0

    val birthCal = Calendar.getInstance().apply {
      isLenient = false
      clear()
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, month - 1)
      set(Calendar.DAY_OF_MONTH, day)
    }
    val birthTime = runCatching { birthCal.timeInMillis }.getOrNull() ?: return 0
    if (birthTime > nowMs) return 0

    val nowCal = Calendar.getInstance().apply { timeInMillis = nowMs }
    var age = nowCal.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
    val nowMonth = nowCal.get(Calendar.MONTH)
    val birthMonth = birthCal.get(Calendar.MONTH)
    if (nowMonth < birthMonth ||
      (nowMonth == birthMonth && nowCal.get(Calendar.DAY_OF_MONTH) < birthCal.get(Calendar.DAY_OF_MONTH))
    ) {
      age--
    }
    return age.coerceIn(0, 130)
  }

  fun dobIsoFromAgeYears(
    ageYears: Int,
    nowMs: Long = System.currentTimeMillis(),
  ): String {
    val safeAge = ageYears.coerceIn(1, 120)
    val nowCal = Calendar.getInstance().apply { timeInMillis = nowMs }
    val birthYear = (nowCal.get(Calendar.YEAR) - safeAge).coerceAtLeast(1900)
    return String.format(Locale.US, "%04d-01-01", birthYear)
  }

  fun formatDobDisplay(dobIso: String): String {
    val match = DOB_REGEX.matchEntire(dobIso.trim()) ?: return dobIso.trim()
    val year = match.groupValues[1]
    val month = match.groupValues[2]
    val day = match.groupValues[3]
    return "$day/$month/$year"
  }

  fun dobIsoFromMillis(utcPickerMillis: Long): String {
    val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
      timeInMillis = utcPickerMillis
    }
    val year = utcCal.get(Calendar.YEAR)
    val month = utcCal.get(Calendar.MONTH) + 1
    val day = utcCal.get(Calendar.DAY_OF_MONTH)
    return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
  }

  fun dateKeyFromMillis(millis: Long = System.currentTimeMillis()): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return fmt.format(Date(millis))
  }

  fun formatDateDisplayFromKey(dateKey: String): String {
    val match = DOB_REGEX.matchEntire(dateKey.trim()) ?: return dateKey
    return "${match.groupValues[3]}/${match.groupValues[2]}/${match.groupValues[1]}"
  }

  fun formatTimeDisplay(millis: Long): String {
    if (millis <= 0L) return "--:--"
    val fmt = SimpleDateFormat("dd/MM hh:mm a", Locale.forLanguageTag("hi-IN"))
    return fmt.format(Date(millis))
  }

  fun sevenMonthsExpiryTimestamp(nowMs: Long = System.currentTimeMillis()): Timestamp {
    val cal = Calendar.getInstance().apply {
      timeInMillis = nowMs
      add(Calendar.MONTH, 7)
    }
    return Timestamp(cal.time)
  }

  fun isWithinSevenMonths(
    expiresAt: Timestamp?,
    createdAt: Timestamp?,
    nowMs: Long = System.currentTimeMillis(),
  ): Boolean {
    if (expiresAt != null) {
      return expiresAt.toDate().time > nowMs
    }
    val createdMs = createdAt?.toDate()?.time ?: return true
    val cal = Calendar.getInstance().apply {
      timeInMillis = createdMs
      add(Calendar.MONTH, 7)
    }
    return cal.timeInMillis > nowMs
  }

  fun formatDurationHindi(durationSeconds: Long): String {
    val safeSec = durationSeconds.coerceAtLeast(0L)
    val hours = safeSec / 3600L
    val mins = (safeSec % 3600L) / 60L
    val secs = safeSec % 60L
    return when {
      hours > 0L && mins > 0L -> "${hours} घंटा ${mins} मिनट"
      hours > 0L -> "${hours} घंटा"
      mins > 0L && secs > 0L -> "${mins} मिनट ${secs} सेकंड"
      mins > 0L -> "${mins} मिनट"
      else -> "${secs} सेकंड"
    }
  }

  fun hasLocationPermission(context: Context): Boolean {
    val coarse = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    val fine = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    return coarse || fine
  }

  fun isSystemLocationEnabled(context: Context): Boolean {
    val lm = context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
      ?: return false
    return runCatching { LocationManagerCompat.isLocationEnabled(lm) }.getOrDefault(false)
  }

  @SuppressLint("MissingPermission")
  suspend fun captureBestCoarseLocation(context: Context): CapturedLocationResult {
    val appCtx = context.applicationContext
    if (!hasLocationPermission(appCtx)) {
      return CapturedLocationResult(
        label = "लोकेशन अनुमति अस्वीकृत — आगे बढ़ने के लिए अनुमति दें",
        lat = 0.0,
        lng = 0.0,
        status = "denied",
      )
    }

    val lm = appCtx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (lm == null || !isSystemLocationEnabled(appCtx)) {
      return CapturedLocationResult(
        label = "मोबाइल की Location (GPS) बंद है — कृपया चालू करें",
        lat = 0.0,
        lng = 0.0,
        status = "gps_disabled",
      )
    }

    // Tier 1: Google Play Services FusedLocationProviderClient (fastest & most reliable indoors on 5G/Wi-Fi)
    val fusedLoc = captureViaFusedClient(appCtx)
    // Tier 2: Android LocationManager fallback (FUSED / NETWORK / GPS / PASSIVE)
    val hardwareLoc = fusedLoc ?: captureViaLocationManager(appCtx, lm)

    if (hardwareLoc != null && isUsableCoordinate(hardwareLoc.latitude, hardwareLoc.longitude)) {
      val safeLat = hardwareLoc.latitude.coerceIn(-90.0, 90.0)
      val safeLng = hardwareLoc.longitude.coerceIn(-180.0, 180.0)
      val label = reverseGeocodeLabel(appCtx, safeLat, safeLng)
        .ifBlank { String.format(Locale.US, "%.4f, %.4f", safeLat, safeLng) }
        .take(240)
      return CapturedLocationResult(
        label = label,
        lat = safeLat,
        lng = safeLng,
        status = "captured",
      )
    }

    // Tier 3: Approximate network city lookup (only when permission & device location are already ON)
    val networkResult = captureViaNetworkGeoFallback()
    if (networkResult != null && networkResult.isValidCaptured) {
      return networkResult
    }

    return CapturedLocationResult(
      label = "GPS / नेटवर्क लोकेशन प्राप्त नहीं हो सकी — कृपया खुले स्थान या Wi-Fi से पुनः प्रयास करें",
      lat = 0.0,
      lng = 0.0,
      status = "unavailable",
    )
  }

  private fun isUsableCoordinate(lat: Double, lng: Double): Boolean {
    if (lat.isNaN() || lng.isNaN()) return false
    if (lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0) return false
    return !(abs(lat) < 0.0001 && abs(lng) < 0.0001)
  }

  @SuppressLint("MissingPermission")
  private suspend fun captureViaFusedClient(appCtx: Context): Location? {
    val fusedClient = runCatching { LocationServices.getFusedLocationProviderClient(appCtx) }.getOrNull()
      ?: return null

    val lastKnown = withTimeoutOrNull(2_000L) {
      runCatching { fusedClient.lastLocation.awaitNullable() }.getOrNull()
    }
    if (lastKnown != null &&
      isUsableCoordinate(lastKnown.latitude, lastKnown.longitude) &&
      (System.currentTimeMillis() - lastKnown.time) <= MAX_RECENT_FIX_AGE_MS
    ) {
      return lastKnown
    }

    val cts = CancellationTokenSource()
    val fresh = withTimeoutOrNull(6_500L) {
      runCatching {
        fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
          .awaitNullable()
      }.getOrNull()
    } ?: run {
      cts.cancel()
      null
    }
    if (fresh != null && isUsableCoordinate(fresh.latitude, fresh.longitude)) {
      return fresh
    }

    return lastKnown?.takeIf { isUsableCoordinate(it.latitude, it.longitude) }
  }

  @SuppressLint("MissingPermission")
  private suspend fun captureViaLocationManager(
    appCtx: Context,
    lm: LocationManager,
  ): Location? = withContext(Dispatchers.Main) {
    val providers = buildList {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        add(LocationManager.FUSED_PROVIDER)
      }
      add(LocationManager.NETWORK_PROVIDER)
      add(LocationManager.GPS_PROVIDER)
      add(LocationManager.PASSIVE_PROVIDER)
    }

    val cached = providers.mapNotNull { provider ->
      runCatching {
        if (provider == LocationManager.PASSIVE_PROVIDER || lm.isProviderEnabled(provider)) {
          lm.getLastKnownLocation(provider)
        } else {
          null
        }
      }.getOrNull()
    }.filter { isUsableCoordinate(it.latitude, it.longitude) }
      .maxByOrNull { it.time }

    if (cached != null && (System.currentTimeMillis() - cached.time) <= MAX_RECENT_FIX_AGE_MS) {
      return@withContext cached
    }

    val fresh = requestSingleFreshLocation(appCtx, lm)
      ?.takeIf { isUsableCoordinate(it.latitude, it.longitude) }
    fresh ?: cached
  }

  @SuppressLint("MissingPermission")
  private suspend fun requestSingleFreshLocation(
    context: Context,
    lm: LocationManager,
  ): Location? {
    val provider = buildList {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        add(LocationManager.FUSED_PROVIDER)
      }
      add(LocationManager.NETWORK_PROVIDER)
      add(LocationManager.GPS_PROVIDER)
    }.firstOrNull { candidate ->
      runCatching { lm.isProviderEnabled(candidate) }.getOrDefault(false)
    } ?: return null

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      return withTimeoutOrNull(4_500L) {
        suspendCancellableCoroutine { cont ->
          val signal = CancellationSignal()
          cont.invokeOnCancellation { signal.cancel() }
          runCatching {
            lm.getCurrentLocation(
              provider,
              signal,
              ContextCompat.getMainExecutor(context.applicationContext),
            ) { loc ->
              if (cont.isActive) cont.resume(loc)
            }
          }.onFailure {
            if (cont.isActive) cont.resume(null)
          }
        }
      }
    }

    @Suppress("DEPRECATION")
    return withTimeoutOrNull(4_500L) {
      suspendCancellableCoroutine { cont ->
        val listener = object : LocationListener {
          override fun onLocationChanged(loc: Location) {
            runCatching { lm.removeUpdates(this) }
            if (cont.isActive) cont.resume(loc)
          }

          @Deprecated("Deprecated in Android API 29")
          override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
          override fun onProviderEnabled(p0: String) {}
          override fun onProviderDisabled(p0: String) {}
        }
        cont.invokeOnCancellation { runCatching { lm.removeUpdates(listener) } }
        runCatching {
          lm.requestSingleUpdate(provider, listener, android.os.Looper.getMainLooper())
        }.onFailure {
          if (cont.isActive) cont.resume(null)
        }
      }
    }
  }

  private suspend fun reverseGeocodeLabel(
    context: Context,
    lat: Double,
    lng: Double,
  ): String = withContext(Dispatchers.IO) {
    if (!Geocoder.isPresent()) return@withContext ""
    val hiResult = queryGeocoderForLocale(context, lat, lng, Locale.forLanguageTag("hi-IN"))
    if (hiResult.isNotBlank()) return@withContext hiResult
    queryGeocoderForLocale(context, lat, lng, Locale.ENGLISH)
  }

  private suspend fun queryGeocoderForLocale(
    context: Context,
    lat: Double,
    lng: Double,
    locale: Locale,
  ): String {
    val geocoder = Geocoder(context.applicationContext, locale)
    val addresses: List<Address> = runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        withTimeoutOrNull(3_000L) {
          suspendCancellableCoroutine { cont ->
            geocoder.getFromLocation(lat, lng, 1) { list ->
              if (cont.isActive) cont.resume(list)
            }
          }
        }.orEmpty()
      } else {
        @Suppress("DEPRECATION")
        geocoder.getFromLocation(lat, lng, 1).orEmpty()
      }
    }.getOrDefault(emptyList())

    val addr = addresses.firstOrNull() ?: return ""
    val parts = listOfNotNull(
      addr.subLocality?.takeIf { it.isNotBlank() },
      addr.locality?.takeIf { it.isNotBlank() } ?: addr.subAdminArea?.takeIf { it.isNotBlank() },
      addr.adminArea?.takeIf { it.isNotBlank() },
      addr.countryName?.takeIf { it.isNotBlank() },
    ).distinct()
    return parts.joinToString(", ").take(240)
  }

  private suspend fun captureViaNetworkGeoFallback(): CapturedLocationResult? = withContext(Dispatchers.IO) {
    val endpoints = listOf(
      "https://ipwho.is/",
      "https://ipapi.co/json/",
    )
    withTimeoutOrNull(6_000L) {
      for (endpoint in endpoints) {
        val parsed = runCatching {
          val request = Request.Builder()
            .url(endpoint)
            .get()
            .header("Accept", "application/json")
            .build()
          SupabaseNetworkClient.sharedOkHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null
            val body = response.body?.string().orEmpty()
            parseIpGeoJson(body)
          }
        }.getOrNull()
        if (parsed != null && parsed.isValidCaptured) {
          return@withTimeoutOrNull parsed
        }
      }
      null
    }
  }

  internal fun parseIpGeoJson(rawJson: String): CapturedLocationResult? {
    if (rawJson.isBlank()) return null
    return runCatching {
      val json = JSONObject(rawJson)
      if (json.has("success") && !json.optBoolean("success", true)) {
        return@runCatching null
      }
      val lat = json.optDouble("latitude", Double.NaN)
      val lng = json.optDouble("longitude", Double.NaN)
      if (!isUsableCoordinate(lat, lng)) return@runCatching null
      val city = json.optString("city", "").trim()
      val region = json.optString("region", "").trim()
      val country = json.optString("country_name", json.optString("country", "")).trim()
      val label = listOf(city, region, country)
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(", ")
        .ifBlank { String.format(Locale.US, "%.4f, %.4f", lat, lng) }
        .take(240)
      CapturedLocationResult(
        label = label,
        lat = lat.coerceIn(-90.0, 90.0),
        lng = lng.coerceIn(-180.0, 180.0),
        status = "captured",
      )
    }.getOrNull()
  }

  private suspend fun <T> Task<T>.awaitNullable(): T? = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
      if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener {
      if (cont.isActive) cont.resume(null)
    }
    addOnCanceledListener {
      if (cont.isActive) cont.resume(null)
    }
  }
}
