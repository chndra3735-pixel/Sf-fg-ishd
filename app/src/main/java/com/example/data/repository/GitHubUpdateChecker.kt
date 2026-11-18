package com.example.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

data class AppUpdateInfo(
  val hasUpdate: Boolean,
  val currentVersionName: String,
  val currentVersionCode: Int,
  val latestVersionTag: String,
  val releaseTitle: String,
  val releaseNotes: String,
  val downloadUrl: String,
  val releasePageUrl: String,
  val repoSlug: String,
)

object GitHubUpdateChecker {
  private val GITHUB_URL_REGEX = Regex(
    """(?:https?://)?(?:www\.)?github\.com/([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)""",
    RegexOption.IGNORE_CASE,
  )
  private val SLUG_ONLY_REGEX = Regex("""^([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)$""")
  private val DIGITS_REGEX = Regex("""\d+""")

  /**
   * Extracts clean "owner/repo" from any pasted GitHub link, release URL, .git URL,
   * share text, or raw "owner/repo" string.
   */
  fun extractRepoSlug(rawInput: String): String? {
    val trimmed = rawInput.trim()
    if (trimmed.isBlank()) return null

    val urlMatch = GITHUB_URL_REGEX.find(trimmed)
    val (rawOwner, rawRepo) = if (urlMatch != null) {
      urlMatch.groupValues[1] to urlMatch.groupValues[2]
    } else {
      val firstToken = trimmed.split(Regex("""\s+""")).firstOrNull().orEmpty().trim('/')
      val slugMatch = SLUG_ONLY_REGEX.find(firstToken) ?: return null
      slugMatch.groupValues[1] to slugMatch.groupValues[2]
    }

    val owner = rawOwner.trim()
    val repo = rawRepo.removeSuffix(".git").trim()
    if (owner.isBlank() || repo.isBlank()) return null
    if (owner.equals("owner", ignoreCase = true) && repo.equals("repo", ignoreCase = true)) return null
    if (owner.startsWith("your-", ignoreCase = true) || repo.startsWith("your-", ignoreCase = true)) return null
    return "$owner/$repo"
  }

  /**
   * Parses numeric version parts (e.g. "v1.0.12" -> [1, 0, 12]) and compares numerically.
   * When currentVersionName is a short 2-part string like "1.0" with currentVersionCode > 1,
   * uses [1, 0, currentVersionCode] so GitHub Actions release tags (v1.0.<run_number>) match accurately.
   */
  fun parseVersionParts(versionText: String): List<Int> {
    return DIGITS_REGEX.findAll(versionText)
      .mapNotNull { it.value.toIntOrNull() }
      .toList()
  }

  fun isNewerVersion(
    remoteTagOrName: String,
    currentVersionName: String,
    currentVersionCode: Int = 1,
  ): Boolean {
    val remoteParts = parseVersionParts(remoteTagOrName)
    if (remoteParts.isEmpty()) return false

    val parsedCurrent = parseVersionParts(currentVersionName)
    val effectiveCurrent = when {
      parsedCurrent.isEmpty() -> listOf(1, 0, currentVersionCode.coerceAtLeast(1))
      parsedCurrent.size == 2 && currentVersionCode > 1 -> listOf(parsedCurrent[0], parsedCurrent[1], currentVersionCode)
      else -> parsedCurrent
    }

    val maxLen = maxOf(remoteParts.size, effectiveCurrent.size)
    for (i in 0 until maxLen) {
      val remoteVal = remoteParts.getOrElse(i) { 0 }
      val currentVal = effectiveCurrent.getOrElse(i) { 0 }
      if (remoteVal > currentVal) return true
      if (remoteVal < currentVal) return false
    }
    return false
  }

  suspend fun checkLatestRelease(
    repoInput: String,
    currentVersionName: String,
    currentVersionCode: Int,
  ): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
    val slug = extractRepoSlug(repoInput)
      ?: return@withContext Result.failure(
        IllegalArgumentException("मान्य GitHub रिपॉजिटरी लिंक दर्ज करें (उदा. https://github.com/username/SwarRoom)।")
      )

    val apiUrl = "https://api.github.com/repos/$slug/releases/latest"
    val request = Request.Builder()
      .url(apiUrl)
      .get()
      .header("Accept", "application/vnd.github+json")
      .header("User-Agent", "SwarRoom-Android/$currentVersionName")
      .build()

    runCatching {
      SupabaseNetworkClient.sharedOkHttpClient.newCall(request).execute().use { response ->
        val bodyStr = response.body?.string().orEmpty()
        if (response.code == 404) {
          throw IllegalStateException(
            "GitHub ($slug) पर अभी कोई पब्लिक रिलीज़ नहीं मिली। मुख्य शाखा (main) पर पुश करने के बाद रिलीज़ अपने आप बनेगी।"
          )
        }
        if (response.code == 403 || response.code == 429) {
          throw IllegalStateException("GitHub API सीमा अस्थायी रूप से पूरी हो गई है। कुछ मिनट बाद पुनः जांचें।")
        }
        if (!response.isSuccessful || bodyStr.isBlank()) {
          throw IllegalStateException("GitHub अपडेट जांच विफल (कोड ${response.code})।")
        }

        val json = JSONObject(bodyStr)
        val tagName = json.optString("tag_name", "").trim()
        val releaseName = json.optString("name", "").trim()
        val releaseNotes = json.optString("body", "").trim()
        val htmlUrl = json.optString("html_url", "https://github.com/$slug/releases/latest").trim()

        var apkDownloadUrl = ""
        val assets = json.optJSONArray("assets")
        if (assets != null) {
          for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val name = asset.optString("name", "").lowercase()
            val browserUrl = asset.optString("browser_download_url", "").trim()
            if (browserUrl.isNotBlank() && name.endsWith(".apk")) {
              apkDownloadUrl = browserUrl
              break
            }
          }
        }

        val effectiveTag = tagName.ifBlank { releaseName.ifBlank { "latest" } }
        val hasUpdate = isNewerVersion(
          remoteTagOrName = effectiveTag,
          currentVersionName = currentVersionName,
          currentVersionCode = currentVersionCode,
        )

        AppUpdateInfo(
          hasUpdate = hasUpdate,
          currentVersionName = currentVersionName,
          currentVersionCode = currentVersionCode,
          latestVersionTag = effectiveTag,
          releaseTitle = releaseName.ifBlank { "SwarRoom $effectiveTag" },
          releaseNotes = releaseNotes,
          downloadUrl = apkDownloadUrl.ifBlank { htmlUrl },
          releasePageUrl = htmlUrl,
          repoSlug = slug,
        )
      }
    }
  }
}
