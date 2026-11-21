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
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

data class CapturedLocationResult(
  val label: String = "",
  val lat: Double = 0.0,
  val lng: Double = 0.0,
  val status: String = "pending",
)

object AuditDateLocationHelper {
  private val DOB_REGEX = Regex("""^(\d{4})-(\d{2})-(\d{2})$""")

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
    val fmt = SimpleDateFormat("dd/MM hh:mm a", Locale("hi", "IN"))
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

  @SuppressLint("MissingPermission")
  suspend fun captureBestCoarseLocation(context: Context): CapturedLocationResult {
    if (!hasLocationPermission(context)) {
      return CapturedLocationResult(
        label = "लोकेशन अनुमति अस्वीकृत",
        lat = 0.0,
        lng = 0.0,
        status = "denied",
      )
    }
    val lm = context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
      ?: return CapturedLocationResult(
        label = "लोकेशन सेवा उपलब्ध नहीं",
        lat = 0.0,
        lng = 0.0,
        status = "unavailable",
      )

    val location: Location? = withContext(Dispatchers.Main) {
      val cached = listOf(
        LocationManager.NETWORK_PROVIDER,
        LocationManager.GPS_PROVIDER,
        LocationManager.PASSIVE_PROVIDER,
      ).mapNotNull { provider ->
        runCatching {
          if (lm.isProviderEnabled(provider) || provider == LocationManager.PASSIVE_PROVIDER) {
            lm.getLastKnownLocation(provider)
          } else {
            null
          }
        }.getOrNull()
      }.maxByOrNull { it.time }

      if (cached != null && (System.currentTimeMillis() - cached.time) <= 10 * 60 * 1000L) {
        return@withContext cached
      }

      val fresh = requestSingleFreshLocation(context, lm)
      fresh ?: cached
    }

    if (location == null) {
      return CapturedLocationResult(
        label = "GPS / नेटवर्क लोकेशन प्राप्त नहीं हो सकी",
        lat = 0.0,
        lng = 0.0,
        status = "unavailable",
      )
    }

    val safeLat = location.latitude.coerceIn(-90.0, 90.0)
    val safeLng = location.longitude.coerceIn(-180.0, 180.0)
    val label = reverseGeocodeLabel(context, safeLat, safeLng)
      .ifBlank { String.format(Locale.US, "%.4f, %.4f", safeLat, safeLng) }
      .take(240)

    return CapturedLocationResult(
      label = label,
      lat = safeLat,
      lng = safeLng,
      status = "captured",
    )
  }

  @SuppressLint("MissingPermission")
  private suspend fun requestSingleFreshLocation(
    context: Context,
    lm: LocationManager,
  ): Location? {
    val provider = when {
      runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false) ->
        LocationManager.NETWORK_PROVIDER
      runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false) ->
        LocationManager.GPS_PROVIDER
      else -> return null
    }

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
    val geocoder = Geocoder(context.applicationContext, Locale("hi", "IN"))
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

    val addr = addresses.firstOrNull() ?: return@withContext ""
    val parts = listOfNotNull(
      addr.subLocality?.takeIf { it.isNotBlank() },
      addr.locality?.takeIf { it.isNotBlank() } ?: addr.subAdminArea?.takeIf { it.isNotBlank() },
      addr.adminArea?.takeIf { it.isNotBlank() },
      addr.countryName?.takeIf { it.isNotBlank() },
    ).distinct()
    parts.joinToString(", ").take(240)
  }
}
