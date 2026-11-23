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

data class CpLevelInfo(
  val level: Int,
  val title: String,
  val progress: Float,
  val xpIntoLevel: Long,
  val xpForNextLevel: Long,
  val remainingXp: Long,
  val ageDays: Long,
  val ageLabel: String,
  val auraTier: Int,
  val auraLabel: String,
  val primaryColor: Color,
  val secondaryColor: Color,
)

data class LevelTierSpec(
  val badgeLevel: Int,
  val minLevel: Int,
  val maxLevel: Int,
  val badgeLabel: String,
  val rangeLabel: String,
  val titleHindi: String,
  val capsuleTopColor: Color,
  val capsuleBottomColor: Color,
  val rimColor: Color,
  val wingHighlightColor: Color,
  val wingShadowColor: Color,
  val gemColor: Color,
  val textColor: Color,
  val glowColor: Color,
)

object SwarUserIdentity {

  val levelTiers: List<LevelTierSpec> = listOf(
    LevelTierSpec(
      badgeLevel = 1,
      minLevel = 1,
      maxLevel = 9,
      badgeLabel = "LV.1",
      rangeLabel = "LV.1~LV.9",
      titleHindi = "नया स्वर",
      capsuleTopColor = Color(0xFF7CA4DE),
      capsuleBottomColor = Color(0xFF39568C),
      rimColor = Color(0xFFE3ECFA),
      wingHighlightColor = Color(0xFFF5F8FF),
      wingShadowColor = Color(0xFF8392AC),
      gemColor = Color(0xFFB3E5FC),
      textColor = Color(0xFFF7FAFF),
      glowColor = Color(0xFF6B9BE8),
    ),
    LevelTierSpec(
      badgeLevel = 10,
      minLevel = 10,
      maxLevel = 19,
      badgeLabel = "LV.10",
      rangeLabel = "LV.10~LV.19",
      titleHindi = "सिल्वर स्टार",
      capsuleTopColor = Color(0xFF2FE6A3),
      capsuleBottomColor = Color(0xFF068663),
      rimColor = Color(0xFFFFD54F),
      wingHighlightColor = Color(0xFFFFF59D),
      wingShadowColor = Color(0xFF2CA874),
      gemColor = Color(0xFFFFEE58),
      textColor = Color(0xFFFFFDE7),
      glowColor = Color(0xFF1ED99A),
    ),
    LevelTierSpec(
      badgeLevel = 20,
      minLevel = 20,
      maxLevel = 29,
      badgeLabel = "LV.20",
      rangeLabel = "LV.20~LV.29",
      titleHindi = "गोल्डन वॉइस",
      capsuleTopColor = Color(0xFFFFBE3B),
      capsuleBottomColor = Color(0xFFC86900),
      rimColor = Color(0xFFFFE57F),
      wingHighlightColor = Color(0xFFFFF59D),
      wingShadowColor = Color(0xFFD68910),
      gemColor = Color(0xFFFF8F00),
      textColor = Color(0xFFFFF8E1),
      glowColor = Color(0xFFFFB300),
    ),
    LevelTierSpec(
      badgeLevel = 30,
      minLevel = 30,
      maxLevel = 39,
      badgeLabel = "LV.30",
      rangeLabel = "LV.30~LV.39",
      titleHindi = "डायमंड होस्ट",
      capsuleTopColor = Color(0xFFFF59B4),
      capsuleBottomColor = Color(0xFFAE0F66),
      rimColor = Color(0xFFFFDF7E),
      wingHighlightColor = Color(0xFFFFF3B0),
      wingShadowColor = Color(0xFFC88719),
      gemColor = Color(0xFFFFEB3B),
      textColor = Color(0xFFFFF8E7),
      glowColor = Color(0xFFFF409F),
    ),
    LevelTierSpec(
      badgeLevel = 40,
      minLevel = 40,
      maxLevel = 49,
      badgeLabel = "LV.40",
      rangeLabel = "LV.40~LV.49",
      titleHindi = "पर्पल क्राउन",
      capsuleTopColor = Color(0xFFAA4DFF),
      capsuleBottomColor = Color(0xFF530EC4),
      rimColor = Color(0xFFFFD86B),
      wingHighlightColor = Color(0xFFFFF59D),
      wingShadowColor = Color(0xFFB87609),
      gemColor = Color(0xFFFF5252),
      textColor = Color(0xFFFFF8E1),
      glowColor = Color(0xFF9C3CFF),
    ),
    LevelTierSpec(
      badgeLevel = 50,
      minLevel = 50,
      maxLevel = 59,
      badgeLabel = "LV.50",
      rangeLabel = "LV.50~LV.59",
      titleHindi = "एमराल्ड चैंपियन",
      capsuleTopColor = Color(0xFF28B91C),
      capsuleBottomColor = Color(0xFF0A5C0C),
      rimColor = Color(0xFFFFE082),
      wingHighlightColor = Color(0xFFFFF8DC),
      wingShadowColor = Color(0xFFD6A022),
      gemColor = Color(0xFFFFD54F),
      textColor = Color(0xFFFFFDE7),
      glowColor = Color(0xFF2ECC40),
    ),
    LevelTierSpec(
      badgeLevel = 60,
      minLevel = 60,
      maxLevel = 69,
      badgeLabel = "LV.60",
      rangeLabel = "LV.60~LV.69",
      titleHindi = "कोबाल्ट नाइट",
      capsuleTopColor = Color(0xFF2D68FF),
      capsuleBottomColor = Color(0xFF00149E),
      rimColor = Color(0xFFFFE57F),
      wingHighlightColor = Color(0xFFFFFDE7),
      wingShadowColor = Color(0xFFD4971A),
      gemColor = Color(0xFFFFCA28),
      textColor = Color(0xFFFFF9E6),
      glowColor = Color(0xFF2962FF),
    ),
    LevelTierSpec(
      badgeLevel = 70,
      minLevel = 70,
      maxLevel = 79,
      badgeLabel = "LV.70",
      rangeLabel = "LV.70~LV.79",
      titleHindi = "शाही अमेथिस्ट",
      capsuleTopColor = Color(0xFF9C1EFF),
      capsuleBottomColor = Color(0xFF3E0075),
      rimColor = Color(0xFFFFDE73),
      wingHighlightColor = Color(0xFFFFF8D6),
      wingShadowColor = Color(0xFFC48614),
      gemColor = Color(0xFFFFB300),
      textColor = Color(0xFFFFF8E1),
      glowColor = Color(0xFF8E24AA),
    ),
    LevelTierSpec(
      badgeLevel = 80,
      minLevel = 80,
      maxLevel = 89,
      badgeLabel = "LV.80",
      rangeLabel = "LV.80~LV.89",
      titleHindi = "फ्लेम एम्परर",
      capsuleTopColor = Color(0xFFFF7A18),
      capsuleBottomColor = Color(0xFFB72400),
      rimColor = Color(0xFFFFE57F),
      wingHighlightColor = Color(0xFFFFF3B0),
      wingShadowColor = Color(0xFFCC7700),
      gemColor = Color(0xFFFF5252),
      textColor = Color(0xFFFFF8E1),
      glowColor = Color(0xFFFF5722),
    ),
    LevelTierSpec(
      badgeLevel = 90,
      minLevel = 90,
      maxLevel = 99,
      badgeLabel = "LV.90",
      rangeLabel = "LV.90~LV.99",
      titleHindi = "रॉयल रूबी",
      capsuleTopColor = Color(0xFFE51C23),
      capsuleBottomColor = Color(0xFF6D0008),
      rimColor = Color(0xFFFFDF70),
      wingHighlightColor = Color(0xFFFFF8DC),
      wingShadowColor = Color(0xFFC58516),
      gemColor = Color(0xFFFFD54F),
      textColor = Color(0xFFFFF5C0),
      glowColor = Color(0xFFD50000),
    ),
    LevelTierSpec(
      badgeLevel = 100,
      minLevel = 100,
      maxLevel = 109,
      badgeLabel = "LV.100",
      rangeLabel = "LV.100~LV.109",
      titleHindi = "ब्लैक गोल्ड लीजेंड",
      capsuleTopColor = Color(0xFF3A342B),
      capsuleBottomColor = Color(0xFF100E0B),
      rimColor = Color(0xFFFFD75E),
      wingHighlightColor = Color(0xFFFFF3B0),
      wingShadowColor = Color(0xFFB87F14),
      gemColor = Color(0xFFFFCA28),
      textColor = Color(0xFFFFE082),
      glowColor = Color(0xFFFFC107),
    ),
    LevelTierSpec(
      badgeLevel = 110,
      minLevel = 110,
      maxLevel = 119,
      badgeLabel = "LV.110",
      rangeLabel = "LV.110~LV.119",
      titleHindi = "कॉस्मिक ऑर्किड",
      capsuleTopColor = Color(0xFFEA6BFF),
      capsuleBottomColor = Color(0xFF7B1FA2),
      rimColor = Color(0xFFF3E5F5),
      wingHighlightColor = Color(0xFFFFFFFF),
      wingShadowColor = Color(0xFFCE93D8),
      gemColor = Color(0xFFFFF59D),
      textColor = Color(0xFFFFF59D),
      glowColor = Color(0xFFE040FB),
    ),
    LevelTierSpec(
      badgeLevel = 120,
      minLevel = 120,
      maxLevel = 120,
      badgeLabel = "LV.120",
      rangeLabel = "LV.120",
      titleHindi = "सुप्रीम सेलेस्टियल",
      capsuleTopColor = Color(0xFF53C9FF),
      capsuleBottomColor = Color(0xFF1A58B8),
      rimColor = Color(0xFFFFE57F),
      wingHighlightColor = Color(0xFFE1F5FE),
      wingShadowColor = Color(0xFF4FC3F7),
      gemColor = Color(0xFFFFD54F),
      textColor = Color(0xFFFFF59D),
      glowColor = Color(0xFF40C4FF),
    ),
  )

  fun tierForLevel(level: Int): LevelTierSpec {
    val safeLevel = level.coerceIn(1, 120)
    return levelTiers.firstOrNull { safeLevel in it.minLevel..it.maxLevel } ?: levelTiers.first()
  }

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
    val level = ((safeCoins / 1000L).toInt() + 1).coerceIn(1, 120)
    val remainder = safeCoins % 1000L
    val progress = if (level >= 120) 1f else (remainder / 1000f).coerceIn(0f, 1f)
    val remaining = if (level >= 120) 0L else (1000L - remainder)
    val (title, color) = when {
      level >= 120 -> "सुप्रीम सेलेस्टियल" to SwarCyan
      level >= 100 -> "ब्लैक गोल्ड लीजेंड" to SwarGold
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

  fun unlockedTiersForLevel(level: Int): List<LevelTierSpec> {
    val safeLevel = level.coerceIn(1, 120)
    return levelTiers.filter { safeLevel >= it.minLevel }
  }

  fun highestUnlockedFrameUrl(
    level: Int,
    configs: Map<Int, LevelTierConfig>,
  ): String {
    val unlocked = unlockedTiersForLevel(level).asReversed()
    for (tier in unlocked) {
      val url = configs[tier.badgeLevel]?.avatarFrameUrl?.trim().orEmpty()
      if (url.isNotBlank()) return url
    }
    return ""
  }

  fun highestUnlockedEntryVideo(
    level: Int,
    configs: Map<Int, LevelTierConfig>,
  ): Pair<LevelTierSpec, String>? {
    val unlocked = unlockedTiersForLevel(level).asReversed()
    for (tier in unlocked) {
      val url = configs[tier.badgeLevel]?.entryVideoUrl?.trim().orEmpty()
      if (url.isNotBlank()) return tier to url
    }
    return null
  }

  fun highestUnlockedTypingBannerUrl(
    level: Int,
    configs: Map<Int, LevelTierConfig>,
  ): String {
    val unlocked = unlockedTiersForLevel(level).asReversed()
    for (tier in unlocked) {
      val url = configs[tier.badgeLevel]?.typingBannerUrl?.trim().orEmpty()
      if (url.isNotBlank()) return url
    }
    return ""
  }

  /**
   * Builds the canonical sorted pair ID matching firestore.rules (`cp_<uid0>_<uid1>`).
   */
  fun cpPairId(uidA: String, uidB: String): String {
    val sorted = listOf(uidA.trim(), uidB.trim()).sorted()
    return "cp_${sorted[0]}_${sorted[1]}"
  }

  fun findAcceptedCpForUser(
    uid: String,
    acceptedLinks: List<CpRelationship>,
  ): CpRelationship? {
    if (uid.isBlank()) return null
    return acceptedLinks.firstOrNull { it.status == "accepted" && uid in it.memberUids }
  }

  fun areAcceptedCpPartners(
    uidA: String,
    uidB: String,
    acceptedLinks: List<CpRelationship>,
  ): CpRelationship? {
    if (uidA.isBlank() || uidB.isBlank() || uidA == uidB) return null
    val expectedId = cpPairId(uidA, uidB)
    return acceptedLinks.firstOrNull {
      it.status == "accepted" && (it.id == expectedId || (uidA in it.memberUids && uidB in it.memberUids))
    }
  }

  /**
   * Computes CP Level from mutual Gift XP and Animation Aura Tier from both Gift Level & Bond Age (days).
   */
  fun cpLevelInfoFor(
    cpXp: Long,
    acceptedAtMs: Long,
    nowMs: Long = System.currentTimeMillis(),
  ): CpLevelInfo {
    val safeXp = cpXp.coerceAtLeast(0L)
    var level = 1
    var thresholdStart = 0L
    // Quadratic progression: Lv1->2 requires 100 XP, Lv2->3 requires 200 XP, Lv3->4 requires 300 XP, etc.
    while (level < 99) {
      val stepCost = level.toLong() * 100L
      val nextThreshold = thresholdStart + stepCost
      if (safeXp < nextThreshold) break
      thresholdStart = nextThreshold
      level++
    }
    val stepCost = if (level >= 99) 1L else level.toLong() * 100L
    val xpIntoLevel = (safeXp - thresholdStart).coerceAtLeast(0L)
    val progress = if (level >= 99) 1f else (xpIntoLevel.toFloat() / stepCost.toFloat()).coerceIn(0f, 1f)
    val remainingXp = if (level >= 99) 0L else (stepCost - xpIntoLevel).coerceAtLeast(0L)

    val ageDays = if (acceptedAtMs > 0L && nowMs >= acceptedAtMs) {
      ((nowMs - acceptedAtMs) / 86_400_000L).coerceAtLeast(0L)
    } else {
      0L
    }
    val ageLabel = when {
      ageDays <= 0L -> "आज जुड़े (1 दिन)"
      ageDays == 1L -> "1 दिन साथ"
      ageDays < 30L -> "$ageDays दिन साथ"
      ageDays < 365L -> "${ageDays / 30L} माह ${ageDays % 30L} दिन"
      else -> "${ageDays / 365L} वर्ष ${(ageDays % 365L) / 30L} माह"
    }

    val title = when {
      level >= 70 -> "लेजेंडरी सोलमेट्स"
      level >= 45 -> "अनमोल दिव्य जोड़ी"
      level >= 25 -> "रॉयल स्टार पार्टनर्स"
      level >= 15 -> "गोल्डन हार्ट्स"
      level >= 8 -> "दिल से जुड़े साथी"
      level >= 3 -> "पक्के दोस्त"
      else -> "नए CP दोस्त"
    }

    // Both CP Gift Level AND bond longevity (older bond = richer seat aura!) boost the visual aura tier (1..5)
    val maturityScore = level + (ageDays / 3L).toInt()
    val (auraTier, auraLabel, primaryColor, secondaryColor) = when {
      maturityScore >= 50 || ageDays >= 90L -> QuadAura(
        5,
        "दिव्य नक्षत्र बंधन",
        Color(0xFFFFD54F),
        Color(0xFF00E5FF),
      )
      maturityScore >= 28 || ageDays >= 30L -> QuadAura(
        4,
        "शाही धूमकेतु आभा",
        Color(0xFFFF4D94),
        Color(0xFFFFC107),
      )
      maturityScore >= 14 || ageDays >= 14L -> QuadAura(
        3,
        "ऑरोरा ऊर्जा धारा",
        Color(0xFF00E5FF),
        Color(0xFFB388FF),
      )
      maturityScore >= 5 || ageDays >= 3L -> QuadAura(
        2,
        "सुनहरी चिंगारी रिबन",
        Color(0xFFFF6EC7),
        Color(0xFFFFD54F),
      )
      else -> QuadAura(
        1,
        "रेशमी गुलाबी तरंग",
        Color(0xFFFF7EB6),
        Color(0xFFFF9E80),
      )
    }

    return CpLevelInfo(
      level = level,
      title = title,
      progress = progress,
      xpIntoLevel = xpIntoLevel,
      xpForNextLevel = stepCost,
      remainingXp = remainingXp,
      ageDays = ageDays,
      ageLabel = ageLabel,
      auraTier = auraTier,
      auraLabel = auraLabel,
      primaryColor = primaryColor,
      secondaryColor = secondaryColor,
    )
  }

  private data class QuadAura(
    val tier: Int,
    val label: String,
    val primary: Color,
    val secondary: Color,
  )
}
