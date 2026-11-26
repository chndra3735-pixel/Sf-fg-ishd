package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

data class RoomMusicState(
  val trackTitle: String = "",
  val activeTrackUri: String = "",
  val isPlaying: Boolean = false,
  val isPrepared: Boolean = false,
  val volumePercent: Int = 75,
  val usingRoomBroadcast: Boolean = false,
)

/**
 * Plays audio tracks from automatic Memory Card scan (`MediaStore.Audio`) or SAF (`OpenDocument`).
 * Uses Agora audio mixing when connected as broadcaster so room listeners can hear the music,
 * and falls back cleanly to local `MediaPlayer` otherwise without double playback.
 */
class RoomMusicPlayer(
  private val appContext: Context?,
  private val audioEngine: VoiceAudioEngine,
  private val onTrackCompleted: () -> Unit = {},
) {
  private val _state = MutableStateFlow(RoomMusicState())
  val state: StateFlow<RoomMusicState> = _state.asStateFlow()

  private var mediaPlayer: MediaPlayer? = null
  private var cachedTrackFile: File? = null
  @Volatile
  private var isSpeakerMuted: Boolean = false

  suspend fun playFromUri(uri: Uri, preferredTitle: String = ""): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
      val ctx = appContext?.applicationContext
        ?: throw IllegalStateException("ऑडियो चलाने के लिए एप्लिकेशन संदर्भ उपलब्ध नहीं है।")
      stopPlaybackInternal()
      val title = preferredTitle.trim().take(64).ifBlank { resolveTrackDisplayName(ctx, uri) }
      val copiedFile = copyUriToTempTrack(ctx, uri)
      cachedTrackFile?.let { old -> if (old.absolutePath != copiedFile.absolutePath) runCatching { old.delete() } }
      cachedTrackFile = copiedFile

      val volume = _state.value.volumePercent.coerceIn(0, 100)
      if (audioEngine.startMusicMixing(copiedFile.absolutePath, volume)) {
        _state.value = RoomMusicState(
          trackTitle = title,
          activeTrackUri = uri.toString(),
          isPlaying = true,
          isPrepared = true,
          volumePercent = volume,
          usingRoomBroadcast = true,
        )
        return@runCatching title
      }

      val mp = MediaPlayer().apply {
        setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build(),
        )
        setDataSource(copiedFile.absolutePath)
        setOnCompletionListener {
          _state.value = _state.value.copy(isPlaying = false)
          onTrackCompleted()
        }
        prepare()
        val scalar = if (isSpeakerMuted) 0f else (volume / 100f)
        setVolume(scalar, scalar)
        start()
      }
      mediaPlayer = mp
      _state.value = RoomMusicState(
        trackTitle = title,
        activeTrackUri = uri.toString(),
        isPlaying = true,
        isPrepared = true,
        volumePercent = volume,
        usingRoomBroadcast = false,
      )
      title
    }.onFailure { err ->
      Log.w(TAG, "Failed to play music URI", err)
    }
  }

  fun togglePlayPause() {
    val cur = _state.value
    if (!cur.isPrepared) return
    if (cur.usingRoomBroadcast) {
      if (cur.isPlaying) {
        audioEngine.pauseMusicMixing()
        _state.value = cur.copy(isPlaying = false)
      } else {
        audioEngine.resumeMusicMixing()
        _state.value = cur.copy(isPlaying = true)
      }
      return
    }
    val mp = mediaPlayer ?: return
    runCatching {
      if (mp.isPlaying) {
        mp.pause()
        _state.value = cur.copy(isPlaying = false)
      } else {
        mp.start()
        _state.value = cur.copy(isPlaying = true)
      }
    }
  }

  fun setVolume(percent: Int) {
    val safe = percent.coerceIn(0, 100)
    val cur = _state.value
    _state.value = cur.copy(volumePercent = safe)
    if (cur.usingRoomBroadcast) {
      audioEngine.setMusicMixingVolume(safe)
    }
    val scalar = if (isSpeakerMuted) 0f else (safe / 100f)
    runCatching { mediaPlayer?.setVolume(scalar, scalar) }
  }

  fun setSpeakerMuted(muted: Boolean) {
    isSpeakerMuted = muted
    val scalar = if (muted) 0f else (_state.value.volumePercent.coerceIn(0, 100) / 100f)
    runCatching { mediaPlayer?.setVolume(scalar, scalar) }
  }

  fun stop() {
    stopPlaybackInternal()
    _state.value = RoomMusicState(volumePercent = _state.value.volumePercent)
  }

  fun release() {
    stop()
    cachedTrackFile?.let { runCatching { it.delete() } }
    cachedTrackFile = null
  }

  private fun stopPlaybackInternal() {
    if (_state.value.usingRoomBroadcast) {
      audioEngine.stopMusicMixing()
    }
    runCatching {
      mediaPlayer?.apply {
        if (isPlaying) stop()
        release()
      }
    }
    mediaPlayer = null
  }

  private fun resolveTrackDisplayName(ctx: Context, uri: Uri): String {
    runCatching {
      ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          if (idx >= 0) {
            val raw = cursor.getString(idx).orEmpty().trim()
            if (raw.isNotBlank()) return raw.substringBeforeLast('.').ifBlank { raw }.take(48)
          }
        }
      }
    }
    return uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')?.take(48)
      ?.ifBlank { "मेमोरी कार्ड ऑडियो" } ?: "मेमोरी कार्ड ऑडियो"
  }

  private fun copyUriToTempTrack(ctx: Context, uri: Uri): File {
    val dir = File(ctx.cacheDir, "room_music").apply { mkdirs() }
    val ext = ctx.contentResolver.getType(uri)
      ?.substringAfterLast('/')
      ?.substringBefore(';')
      ?.filter { it.isLetterOrDigit() }
      ?.take(8)
      ?.ifBlank { "mp3" }
      ?: "mp3"
    val outFile = File(dir, "active_track.$ext")
    ctx.contentResolver.openInputStream(uri)?.use { input ->
      outFile.outputStream().use { output ->
        input.copyTo(output)
      }
    } ?: throw IllegalStateException("चुनी गई ऑडियो फ़ाइल खोली नहीं जा सकी।")
    return outFile
  }

  private companion object {
    const val TAG = "RoomMusicPlayer"
  }
}
