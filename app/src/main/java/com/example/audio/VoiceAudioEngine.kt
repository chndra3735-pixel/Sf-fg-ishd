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
import com.example.BuildConfig
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
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
 * Dual-mode real-time Voice Audio Engine:
 * - When `BuildConfig.AGORA_APP_ID` is configured in AI Studio Secrets, uses Agora RTC Voice SDK 4.x
 *   for live multi-user room broadcasting, audience listening, remote audio playback, and ear monitoring.
 * - When `AGORA_APP_ID` is not yet configured, falls back cleanly to local AudioRecord metering
 *   so seat/mic state can still be tested locally without crashing.
 */
class VoiceAudioEngine(private val appContext: Context) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var captureJob: Job? = null
  private var echoCanceler: AcousticEchoCanceler? = null
  private var noiseSuppressor: NoiseSuppressor? = null

  private val agoraAppId: String = BuildConfig.AGORA_APP_ID.trim()

  val isAgoraConfigured: Boolean
    get() = agoraAppId.isNotBlank() &&
      agoraAppId != "YOUR_AGORA_APP_ID" &&
      !agoraAppId.startsWith("your", ignoreCase = true)

  private var rtcEngine: RtcEngine? = null
  private var activeChannelName: String? = null
  private var currentSpeakerEnabled: Boolean = true

  @Volatile
  private var currentBroadcasterMuted: Boolean = true

  @Volatile
  private var currentIsBroadcaster: Boolean = false

  private val _audioLevel = MutableStateFlow(0f)
  val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

  private val _isSpeaking = MutableStateFlow(false)
  val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

  private val _isCapturing = MutableStateFlow(false)
  val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

  private val _isAgoraConnected = MutableStateFlow(false)
  val isAgoraConnected: StateFlow<Boolean> = _isAgoraConnected.asStateFlow()

  private val _agoraStatusText = MutableStateFlow(
    if (isAgoraConfigured) "Agora लाइव वॉइस तैयार" else "Agora App ID आवश्यक है"
  )
  val agoraStatusText: StateFlow<String> = _agoraStatusText.asStateFlow()

  @Volatile
  var monitorLoopbackEnabled: Boolean = false
    set(value) {
      field = value
      runCatching { rtcEngine?.enableInEarMonitoring(value) }
    }

  private val rtcEventHandler = object : IRtcEngineEventHandler() {
    override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
      Log.d(TAG, "Agora joined channel=$channel uid=$uid")
      _isAgoraConnected.value = true
      _agoraStatusText.value = "Agora लाइव वॉइस कनेक्टेड"
    }

    override fun onRejoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
      _isAgoraConnected.value = true
      _agoraStatusText.value = "Agora लाइव वॉइस कनेक्टेड"
    }

    override fun onConnectionLost() {
      _isAgoraConnected.value = false
      _agoraStatusText.value = "Agora नेटवर्क पुनः जुड़ रहा है..."
    }

    override fun onLeaveChannel(stats: RtcStats?) {
      _isAgoraConnected.value = false
      _audioLevel.value = 0f
      _isSpeaking.value = false
    }

    override fun onError(err: Int) {
      Log.w(TAG, "Agora RTC error code=$err")
      val hint = when (err) {
        101 -> "अमान्य Agora App ID (Secrets जांचें)"
        109, 110 -> "Agora टोकन आवश्यक/समाप्त (Testing Mode या AGORA_TEMP_TOKEN जांचें)"
        else -> "Agora त्रुटि कोड: $err"
      }
      _agoraStatusText.value = hint
    }

    override fun onAudioVolumeIndication(speakers: Array<out AudioVolumeInfo>?, totalVolume: Int) {
      if (speakers == null) return
      val localInfo = speakers.firstOrNull { it.uid == 0 }
      if (localInfo != null && currentIsBroadcaster && !currentBroadcasterMuted) {
        val normalized = (localInfo.volume / 255f).coerceIn(0f, 1f)
        _audioLevel.value = normalized
        _isSpeaking.value = localInfo.volume > 15
      } else if (currentBroadcasterMuted || !currentIsBroadcaster) {
        _audioLevel.value = 0f
        _isSpeaking.value = false
      }
    }
  }

  private fun ensureAgoraEngine(): RtcEngine? {
    if (!isAgoraConfigured) return null
    rtcEngine?.let { return it }
    return runCatching {
      val config = RtcEngineConfig().apply {
        mContext = appContext.applicationContext
        mAppId = agoraAppId
        mEventHandler = rtcEventHandler
        mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
      }
      RtcEngine.create(config)?.apply {
        setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        enableAudio()
        enableAudioVolumeIndication(200, 3, true)
        setDefaultAudioRoutetoSpeakerphone(currentSpeakerEnabled)
        setEnableSpeakerphone(currentSpeakerEnabled)
        enableInEarMonitoring(monitorLoopbackEnabled)
      }.also { engine ->
        rtcEngine = engine
      }
    }.onFailure { err ->
      Log.e(TAG, "Agora init failed: ${err.message}", err)
      _agoraStatusText.value = "Agora इंजन शुरू नहीं हो सका: ${err.message}"
    }.getOrNull()
  }

  fun joinRoomVoice(roomId: String, isBroadcaster: Boolean, isMuted: Boolean) {
    val safeChannel = roomId.replace(Regex("[^A-Za-z0-9_\\-]"), "_").take(64)
    if (safeChannel.isBlank()) return
    currentIsBroadcaster = isBroadcaster
    currentBroadcasterMuted = isMuted

    if (!isAgoraConfigured) {
      _agoraStatusText.value = "Agora App ID सेट नहीं है (AI Studio Secrets में AGORA_APP_ID जोड़ें)"
      return
    }

    val engine = ensureAgoraEngine() ?: return
    if (activeChannelName == safeChannel) {
      updateSeatPublishState(isSeated = isBroadcaster, isMuted = isMuted)
      return
    }
    if (activeChannelName != null) {
      runCatching { engine.leaveChannel() }
    }
    activeChannelName = safeChannel

    val options = buildMediaOptions(isBroadcaster = isBroadcaster, isMuted = isMuted)
    val rc = engine.joinChannel(null, safeChannel, 0, options)
    if (rc == 0) {
      _agoraStatusText.value = "Agora चैनल से जुड़ रहा है..."
      engine.muteLocalAudioStream(!isBroadcaster || isMuted)
      engine.enableLocalAudio(isBroadcaster && !isMuted)
      _isCapturing.value = isBroadcaster && !isMuted
    } else {
      Log.w(TAG, "Agora joinChannel returned error code=$rc")
      _agoraStatusText.value = "Agora चैनल जॉइन त्रुटि: $rc"
    }
  }

  fun updateSeatPublishState(isSeated: Boolean, isMuted: Boolean) {
    currentIsBroadcaster = isSeated
    currentBroadcasterMuted = isMuted
    val effectivePublish = isSeated && !isMuted
    if (!effectivePublish) {
      _isSpeaking.value = false
      _audioLevel.value = 0f
    }

    if (isAgoraConfigured && activeChannelName != null) {
      val engine = rtcEngine ?: return
      val options = buildMediaOptions(isBroadcaster = isSeated, isMuted = isMuted)
      engine.setClientRole(
        if (isSeated) Constants.CLIENT_ROLE_BROADCASTER else Constants.CLIENT_ROLE_AUDIENCE
      )
      engine.updateChannelMediaOptions(options)
      engine.enableLocalAudio(effectivePublish)
      engine.muteLocalAudioStream(!effectivePublish)
      _isCapturing.value = effectivePublish
    } else if (!isAgoraConfigured) {
      if (effectivePublish) startCapture() else stopCapture()
    }
  }

  fun leaveRoomVoice() {
    activeChannelName = null
    currentIsBroadcaster = false
    currentBroadcasterMuted = true
    _isAgoraConnected.value = false
    if (isAgoraConfigured) {
      runCatching {
        rtcEngine?.muteLocalAudioStream(true)
        rtcEngine?.leaveChannel()
      }
    }
    stopCapture()
    _agoraStatusText.value = if (isAgoraConfigured) "Agora लाइव वॉइस तैयार" else "Agora App ID आवश्यक है"
  }

  private fun buildMediaOptions(isBroadcaster: Boolean, isMuted: Boolean): ChannelMediaOptions {
    val publishMic = isBroadcaster && !isMuted
    return ChannelMediaOptions().apply {
      channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
      clientRoleType = if (isBroadcaster) {
        Constants.CLIENT_ROLE_BROADCASTER
      } else {
        Constants.CLIENT_ROLE_AUDIENCE
      }
      publishMicrophoneTrack = publishMic
      autoSubscribeAudio = true
    }
  }

  fun setSpeakerphone(enabled: Boolean) {
    currentSpeakerEnabled = enabled
    runCatching {
      rtcEngine?.setDefaultAudioRoutetoSpeakerphone(enabled)
      rtcEngine?.setEnableSpeakerphone(enabled)
      val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
      audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
      @Suppress("DEPRECATION")
      audioManager?.isSpeakerphoneOn = enabled
    }.onFailure { Log.w(TAG, "Speaker toggle warning: ${it.message}") }
  }

  @SuppressLint("MissingPermission")
  fun startCapture() {
    if (isAgoraConfigured) {
      updateSeatPublishState(isSeated = true, isMuted = false)
      return
    }
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
    if (isAgoraConfigured && activeChannelName != null) {
      updateSeatPublishState(isSeated = currentIsBroadcaster, isMuted = true)
    }
    captureJob?.cancel()
    captureJob = null
    _isCapturing.value = false
    _isSpeaking.value = false
    _audioLevel.value = 0f
  }

  fun release() {
    leaveRoomVoice()
    runCatching {
      if (rtcEngine != null) {
        RtcEngine.destroy()
        rtcEngine = null
      }
    }
    scope.cancel()
  }

  private companion object {
    const val TAG = "VoiceAudioEngine"
  }
}
