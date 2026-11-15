package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Real-time Android AudioRecord/AudioTrack microphone capture engine.
 * Computes live RMS voice amplitude for speaking rings and supports optional local monitor loopback
 * plus speakerphone routing.
 */
class VoiceAudioEngine(private val appContext: Context) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var captureJob: Job? = null
  private var echoCanceler: AcousticEchoCanceler? = null
  private var noiseSuppressor: NoiseSuppressor? = null

  private val _audioLevel = MutableStateFlow(0f)
  val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

  private val _isSpeaking = MutableStateFlow(false)
  val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

  private val _isCapturing = MutableStateFlow(false)
  val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

  @Volatile
  var monitorLoopbackEnabled: Boolean = false

  fun setSpeakerphone(enabled: Boolean) {
    runCatching {
      val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
      audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
      @Suppress("DEPRECATION")
      audioManager?.isSpeakerphoneOn = enabled
    }.onFailure { Log.w(TAG, "Speaker toggle warning: ${it.message}") }
  }

  @SuppressLint("MissingPermission")
  fun startCapture() {
    if (captureJob?.isActive == true) return
    captureJob = scope.launch {
      val sampleRate = 16000
      val channelIn = AudioFormat.CHANNEL_IN_MONO
      val encoding = AudioFormat.ENCODING_PCM_16BIT
      val minRecBuf = AudioRecord.getMinBufferSize(sampleRate, channelIn, encoding)
      if (minRecBuf <= 0) {
        Log.w(TAG, "AudioRecord buffer unavailable on device ($minRecBuf)")
        return@launch
      }

      val bufferSize = maxOf(minRecBuf, 2048)
      var recorder: AudioRecord? = null
      var track: AudioTrack? = null
      try {
        recorder = AudioRecord(
          MediaRecorder.AudioSource.VOICE_COMMUNICATION,
          sampleRate,
          channelIn,
          encoding,
          bufferSize * 2,
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
          Log.w(TAG, "AudioRecord failed to initialize")
          return@launch
        }

        val sessionId = recorder.audioSessionId
        if (AcousticEchoCanceler.isAvailable()) {
          echoCanceler = AcousticEchoCanceler.create(sessionId)?.apply { enabled = true }
        }
        if (NoiseSuppressor.isAvailable()) {
          noiseSuppressor = NoiseSuppressor.create(sessionId)?.apply { enabled = true }
        }

        val minPlayBuf = AudioTrack.getMinBufferSize(
          sampleRate,
          AudioFormat.CHANNEL_OUT_MONO,
          encoding,
        ).coerceAtLeast(2048)

        track = AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
              .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
              .build()
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(encoding)
              .setSampleRate(sampleRate)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setBufferSizeInBytes(minPlayBuf * 2)
          .setTransferMode(AudioTrack.MODE_STREAM)
          .build()

        recorder.startRecording()
        track.play()
        _isCapturing.value = true

        val pcmBuffer = ShortArray(bufferSize / 2)
        var lastActiveMs = 0L

        while (isActive) {
          val readCount = recorder.read(pcmBuffer, 0, pcmBuffer.size)
          if (readCount > 0) {
            var sumSq = 0.0
            for (i in 0 until readCount) {
              val sample = pcmBuffer[i].toDouble()
              sumSq += sample * sample
            }
            val rms = sqrt(sumSq / readCount)
            val normalized = (rms / 6500.0).toFloat().coerceIn(0f, 1f)
            _audioLevel.value = normalized

            val now = System.currentTimeMillis()
            if (normalized > 0.06f) {
              lastActiveMs = now
              _isSpeaking.value = true
            } else if (now - lastActiveMs > 450L) {
              _isSpeaking.value = false
            }

            if (monitorLoopbackEnabled && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
              track.write(pcmBuffer, 0, readCount)
            }
          }
        }
      } catch (e: Exception) {
        Log.w(TAG, "Audio capture stopped: ${e.message}")
      } finally {
        _isCapturing.value = false
        _isSpeaking.value = false
        _audioLevel.value = 0f
        runCatching { echoCanceler?.release() }
        runCatching { noiseSuppressor?.release() }
        echoCanceler = null
        noiseSuppressor = null
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        runCatching { track?.stop() }
        runCatching { track?.release() }
      }
    }
  }

  fun stopCapture() {
    captureJob?.cancel()
    captureJob = null
    _isCapturing.value = false
    _isSpeaking.value = false
    _audioLevel.value = 0f
  }

  fun release() {
    stopCapture()
    scope.cancel()
  }

  private companion object {
    const val TAG = "VoiceAudioEngine"
  }
}
