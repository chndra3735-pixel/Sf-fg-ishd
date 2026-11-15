package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.SwarCoral
import com.example.ui.theme.SwarCyan
import com.example.ui.theme.SwarGold
import com.example.ui.theme.SwarMint
import com.example.ui.theme.SwarViolet
import java.security.MessageDigest

data class UserLevelInfo(
  val level: Int,
  val title: String,
  val progress: Float,
  val badgeColor: Color,
  val nextLevelRemainingCoins: Long,
)

object SwarUserIdentity {

  /**
   * Generates a deterministic, unique 10-digit numeric ID (1000000000..9999999999)
   * from a user's Firebase UID so every user gets a distinct numeric ID across all screens.
   */
  fun numericIdForUid(uid: String): String {
    val clean = uid.trim().ifBlank { "swar_guest_default" }
    val digest = MessageDigest.getInstance("SHA-256")
      .digest(clean.toByteArray(Charsets.UTF_8))
    var raw = 0L
    for (i in 0 until 8) {
      raw = (raw shl 8) or (digest[i].toLong() and 0xFFL)
    }
    val positive = raw and Long.MAX_VALUE
    val tenDigit = 1_000_000_000L + (positive % 9_000_000_000L)
    return tenDigit.toString()
  }

  fun levelForCoins(coins: Long): UserLevelInfo {
    val safeCoins = coins.coerceAtLeast(0L)
    val level = ((safeCoins / 1000L).toInt() + 1).coerceIn(1, 99)
    val remainder = safeCoins % 1000L
    val progress = if (level >= 99) 1f else (remainder / 1000f).coerceIn(0f, 1f)
    val remaining = if (level >= 99) 0L else (1000L - remainder)
    val (title, color) = when {
      level >= 50 -> "रॉयल लीजेंड" to SwarCoral
      level >= 30 -> "डायमंड होस्ट" to SwarViolet
      level >= 15 -> "गोल्डन वॉइस" to SwarGold
      level >= 5 -> "सिल्वर स्टार" to SwarMint
      else -> "नया स्वर" to SwarCyan
    }
    return UserLevelInfo(
      level = level,
      title = title,
      progress = progress,
      badgeColor = color,
      nextLevelRemainingCoins = remaining,
    )
  }

  fun levelForProfile(profile: UserProfile?): UserLevelInfo {
    return levelForCoins(profile?.coins ?: 1500L)
  }
}
