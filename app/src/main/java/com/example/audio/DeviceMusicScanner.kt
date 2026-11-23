package com.example.audio

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Immutable
data class DeviceMusicTrack(
  val id: String,
  val uri: Uri,
  val title: String,
  val artist: String = "",
  val folderName: String = "स्टोरेज",
  val durationMs: Long = 0L,
  val isFromSdCard: Boolean = false,
)

object DeviceMusicScanner {
  private const val TAG = "DeviceMusicScanner"
  private const val MAX_TRACKS = 1500
  private const val MAX_SAF_DEPTH = 4
  private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "wav", "ogg", "flac", "opus", "3gp", "amr", "wma")

  fun requiredAudioReadPermission(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      Manifest.permission.READ_MEDIA_AUDIO
    } else {
      Manifest.permission.READ_EXTERNAL_STORAGE
    }
  }

  fun hasAudioReadPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
      context,
      requiredAudioReadPermission(),
    ) == PackageManager.PERMISSION_GRANTED
  }

  fun formatDurationClock(durationMs: Long): String {
    if (durationMs <= 0L) return "--:--"
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
      String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
      String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
  }

  fun takePersistableFolderPermission(context: Context, treeUri: Uri) {
    runCatching {
      context.contentResolver.takePersistableUriPermission(
        treeUri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION,
      )
    }
  }

  suspend fun scanAllDeviceMusic(
    context: Context,
    savedTreeUris: Set<String> = emptySet(),
  ): List<DeviceMusicTrack> = withContext(Dispatchers.IO) {
    val ctx = context.applicationContext
    val tracks = mutableListOf<DeviceMusicTrack>()
    val seenKeys = HashSet<String>()

    fun addTrackIfUnique(track: DeviceMusicTrack) {
      if (tracks.size >= MAX_TRACKS) return
      val dedupKey = "${track.uri}|${track.title.lowercase(Locale.ROOT)}|${track.durationMs}"
      if (seenKeys.add(track.uri.toString()) && seenKeys.add(dedupKey)) {
        tracks.add(track)
      }
    }

    if (hasAudioReadPermission(ctx)) {
      val volumeUris = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          val volumes = runCatching { MediaStore.getExternalVolumeNames(ctx) }.getOrNull().orEmpty()
          volumes.forEach { vol ->
            runCatching { MediaStore.Audio.Media.getContentUri(vol) }.getOrNull()?.let { add(vol to it) }
          }
        }
        val defaultUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        if (none { it.second == defaultUri }) {
          add("external" to defaultUri)
        }
      }

      volumeUris.forEach { (volName, collectionUri) ->
        if (tracks.size >= MAX_TRACKS) return@forEach
        queryMediaStoreCollection(ctx, volName, collectionUri).forEach(::addTrackIfUnique)
      }
    }

    savedTreeUris.forEach { rawUri ->
      if (tracks.size >= MAX_TRACKS) return@forEach
      val parsed = runCatching { Uri.parse(rawUri) }.getOrNull() ?: return@forEach
      scanSafTree(ctx, parsed).forEach(::addTrackIfUnique)
    }

    tracks
  }

  private fun queryMediaStoreCollection(
    context: Context,
    volumeName: String,
    collectionUri: Uri,
  ): List<DeviceMusicTrack> {
    val result = mutableListOf<DeviceMusicTrack>()
    val projection = buildList {
      add(MediaStore.Audio.Media._ID)
      add(MediaStore.Audio.Media.TITLE)
      add(MediaStore.Audio.Media.DISPLAY_NAME)
      add(MediaStore.Audio.Media.ARTIST)
      add(MediaStore.Audio.Media.DURATION)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        add(MediaStore.Audio.Media.RELATIVE_PATH)
        add(MediaStore.Audio.Media.VOLUME_NAME)
      } else {
        @Suppress("DEPRECATION")
        add(MediaStore.Audio.Media.DATA)
      }
    }.toTypedArray()

    val selection = "${MediaStore.Audio.Media.DURATION} >= 3000 OR ${MediaStore.Audio.Media.IS_MUSIC} != 0"
    val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

    runCatching {
      context.contentResolver.query(
        collectionUri,
        projection,
        selection,
        null,
        sortOrder,
      )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
        val nameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
        val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
        val durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
        val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
        } else {
          -1
        }
        val volCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          cursor.getColumnIndex(MediaStore.Audio.Media.VOLUME_NAME)
        } else {
          -1
        }
        @Suppress("DEPRECATION")
        val dataCol = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
          cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
        } else {
          -1
        }

        while (cursor.moveToNext() && result.size < MAX_TRACKS) {
          val id = cursor.getLong(idCol)
          val rawName = if (nameCol >= 0) cursor.getString(nameCol).orEmpty() else ""
          val rawTitle = if (titleCol >= 0) cursor.getString(titleCol).orEmpty() else ""
          val displayTitle = rawTitle.trim().ifBlank {
            rawName.substringBeforeLast('.').trim()
          }.ifBlank { "ऑडियो ट्रैक #$id" }.take(64)

          val rawArtist = if (artistCol >= 0) cursor.getString(artistCol).orEmpty().trim() else ""
          val cleanArtist = if (rawArtist.equals("<unknown>", ignoreCase = true) || rawArtist.isBlank()) {
            ""
          } else {
            rawArtist.take(40)
          }

          val durationMs = if (durationCol >= 0) cursor.getLong(durationCol).coerceAtLeast(0L) else 0L
          val resolvedVolume = if (volCol >= 0) cursor.getString(volCol).orEmpty() else volumeName
          val relPath = if (relPathCol >= 0) cursor.getString(relPathCol).orEmpty() else ""
          val dataPath = if (dataCol >= 0) cursor.getString(dataCol).orEmpty() else ""

          val folderLabel = when {
            relPath.isNotBlank() -> relPath.trimEnd('/').substringAfterLast('/').ifBlank { "Music" }
            dataPath.isNotBlank() -> dataPath.substringBeforeLast('/').substringAfterLast('/').ifBlank { "Storage" }
            else -> "Music"
          }
          val isSdCard = (resolvedVolume.isNotBlank() &&
            resolvedVolume != "external_primary" &&
            resolvedVolume != "external") ||
            (dataPath.contains("/storage/") && !dataPath.contains("/storage/emulated/"))

          val trackUri = ContentUris.withAppendedId(collectionUri, id)
          result.add(
            DeviceMusicTrack(
              id = "ms_${resolvedVolume}_$id",
              uri = trackUri,
              title = displayTitle,
              artist = cleanArtist,
              folderName = if (isSdCard) "SD कार्ड • $folderLabel" else folderLabel,
              durationMs = durationMs,
              isFromSdCard = isSdCard,
            ),
          )
        }
      }
    }.onFailure { err ->
      Log.w(TAG, "Failed scanning MediaStore collection $collectionUri", err)
    }

    return result
  }

  private fun scanSafTree(context: Context, treeUri: Uri): List<DeviceMusicTrack> {
    val rootId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return emptyList()
    val collected = mutableListOf<DeviceMusicTrack>()
    val queue = ArrayDeque<Pair<String, Int>>()
    queue.add(rootId to 0)

    while (queue.isNotEmpty() && collected.size < MAX_TRACKS) {
      val (docId, depth) = queue.removeFirst()
      val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
      runCatching {
        context.contentResolver.query(
          childrenUri,
          arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
          ),
          null,
          null,
          null,
        )?.use { cursor ->
          val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
          val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
          val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
          while (cursor.moveToNext() && collected.size < MAX_TRACKS) {
            val childId = cursor.getString(idIdx) ?: continue
            val childName = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else ""
            val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx).orEmpty() else ""
            if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
              if (depth < MAX_SAF_DEPTH) {
                queue.add(childId to (depth + 1))
              }
            } else if (isAudioCandidate(childName, mime)) {
              val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
              val cleanTitle = childName.substringBeforeLast('.').trim().ifBlank { childName }.take(64)
              val parentFolder = docId.substringAfterLast(':').substringBeforeLast('/', "").substringAfterLast('/')
                .ifBlank { "SD कार्ड फ़ोल्डर" }
              collected.add(
                DeviceMusicTrack(
                  id = "saf_${childId.hashCode()}",
                  uri = fileUri,
                  title = cleanTitle.ifBlank { "ऑडियो ट्रैक" },
                  artist = "",
                  folderName = "SD कार्ड • $parentFolder",
                  durationMs = 0L,
                  isFromSdCard = true,
                ),
              )
            }
          }
        }
      }.onFailure { err ->
        Log.w(TAG, "Failed scanning SAF tree $docId", err)
      }
    }
    return collected
  }

  private fun isAudioCandidate(displayName: String, mimeType: String): Boolean {
    if (mimeType.startsWith("audio/", ignoreCase = true)) return true
    val ext = displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
    return ext in AUDIO_EXTENSIONS
  }
}
