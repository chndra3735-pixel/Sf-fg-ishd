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
import com.example.data.repository.SupabaseNetworkClient
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
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
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
  private val agoraTokenEndpoint: String = BuildConfig.AGORA_TOKEN_ENDPOINT.trim()

  val isAgoraConfigured: Boolean
    get() = agoraAppId.isNotBlank() &&
      agoraAppId != "YOUR_AGORA_APP_ID" &&
      !agoraAppId.startsWith("your", ignoreCase = true)

  val isTokenServerConfigured: Boolean
    get() = agoraTokenEndpoint.startsWith("https://") &&
      !agoraTokenEndpoint.contains("your-token-server.example.com", ignoreCase = true)

  private var rtcEngine: RtcEngine? = null
  private var activeChannelName: String? = null
  private var tokenFetchJob: Job? = null
  private var lastJoinedRole: String = "subscriber"
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
        109, 110 -> if (isTokenServerConfigured) {
          "Agora टोकन अमान्य/समाप्त — टोकन सर्वर पर AGORA_APP_ID और AGORA_APP_CERTIFICATE जांचें"
        } else {
          "Agora प्रोजेक्ट 'Secured Mode (Token)' पर है — टोकन सर्वर (AGORA_TOKEN_ENDPOINT) जोड़ें या Testing Mode उपयोग करें"
        }
        else -> "Agora त्रुटि कोड: $err"
      }
      _agoraStatusText.value = hint
    }

    override fun onTokenPrivilegeWillExpire(token: String?) {
      val channel = activeChannelName ?: return
      if (!isTokenServerConfigured) return
      scope.launch {
        val role = if (currentIsBroadcaster) "publisher" else "subscriber"
        fetchAgoraToken(channel, role)
          .onSuccess { fetched ->
            rtcEngine?.renewToken(fetched.token)
            lastJoinedRole = role
          }
          .onFailure { err ->
            Log.w(TAG, "Token renew warning: ${err.message}")
          }
      }
    }

    override fun onRequestToken() {
      val channel = activeChannelName ?: return
      if (!isTokenServerConfigured) return
      scope.launch {
        val role = if (currentIsBroadcaster) "publisher" else "subscriber"
        fetchAgoraToken(channel, role)
          .onSuccess { fetched ->
            rtcEngine?.renewToken(fetched.token)
            lastJoinedRole = role
          }
          .onFailure { err ->
            _agoraStatusText.value = "Agora टोकन नवीनीकरण विफल: ${err.message}"
          }
      }
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
    tokenFetchJob?.cancel()
    if (activeChannelName != null) {
      runCatching { engine.leaveChannel() }
    }
    activeChannelName = safeChannel

    if (!isTokenServerConfigured) {
      val options = buildMediaOptions(isBroadcaster = isBroadcaster, isMuted = isMuted)
      val rc = engine.joinChannel(null, safeChannel, 0, options)
      handleJoinResult(engine, rc, isBroadcaster, isMuted, "testing")
      return
    }

    val requestedRole = if (isBroadcaster) "publisher" else "subscriber"
    _agoraStatusText.value = "सुरक्षित Agora टोकन प्राप्त हो रहा है..."
    tokenFetchJob = scope.launch {
      fetchAgoraToken(safeChannel, requestedRole)
        .onSuccess { fetched ->
          if (activeChannelName != safeChannel) return@launch
          val latestBroadcaster = currentIsBroadcaster
          val latestMuted = currentBroadcasterMuted
          lastJoinedRole = requestedRole
          val options = buildMediaOptions(isBroadcaster = latestBroadcaster, isMuted = latestMuted)
          val rc = engine.joinChannel(fetched.token, safeChannel, fetched.uid, options)
          handleJoinResult(engine, rc, latestBroadcaster, latestMuted, requestedRole)
        }
        .onFailure { err ->
          if (activeChannelName == safeChannel) {
            _isAgoraConnected.value = false
            _agoraStatusText.value = "टोकन सर्वर त्रुटि: ${err.message ?: "कनेक्शन विफल"}"
          }
        }
    }
  }

  private fun handleJoinResult(
    engine: RtcEngine,
    rc: Int,
    isBroadcaster: Boolean,
    isMuted: Boolean,
    role: String,
  ) {
    if (rc == 0) {
      lastJoinedRole = role
      _agoraStatusText.value = if (isTokenServerConfigured) {
        "सुरक्षित टोकन मोड से जुड़ रहा है..."
      } else {
        "Agora चैनल से जुड़ रहा है..."
      }
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

    val channel = activeChannelName
    if (isAgoraConfigured && channel != null) {
      val engine = rtcEngine ?: return
      if (isTokenServerConfigured && isSeated && lastJoinedRole != "publisher") {
        scope.launch {
          fetchAgoraToken(channel, "publisher")
            .onSuccess { fetched ->
              if (activeChannelName == channel) {
                engine.renewToken(fetched.token)
                lastJoinedRole = "publisher"
                applyRoleAndMediaOptions(engine, isSeated = true, isMuted = currentBroadcasterMuted)
              }
            }
            .onFailure { err ->
              _agoraStatusText.value = "स्पीकर टोकन अपडेट विफल: ${err.message}"
            }
        }
        return
      }
      applyRoleAndMediaOptions(engine, isSeated = isSeated, isMuted = isMuted)
    } else if (!isAgoraConfigured) {
      if (effectivePublish) startCapture() else stopCapture()
    }
  }

  private fun applyRoleAndMediaOptions(engine: RtcEngine, isSeated: Boolean, isMuted: Boolean) {
    val effectivePublish = isSeated && !isMuted
    val options = buildMediaOptions(isBroadcaster = isSeated, isMuted = isMuted)
    engine.setClientRole(
      if (isSeated) Constants.CLIENT_ROLE_BROADCASTER else Constants.CLIENT_ROLE_AUDIENCE
    )
    engine.updateChannelMediaOptions(options)
    engine.enableLocalAudio(effectivePublish)
    engine.muteLocalAudioStream(!effectivePublish)
    _isCapturing.value = effectivePublish
  }

  fun leaveRoomVoice() {
    tokenFetchJob?.cancel()
    tokenFetchJob = null
    activeChannelName = null
    lastJoinedRole = "subscriber"
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

  private data class FetchedToken(val token: String, val uid: Int)

  private suspend fun fetchAgoraToken(channelName: String, role: String): Result<FetchedToken> =
    withContext(Dispatchers.IO) {
      runCatching {
        val payload = JSONObject().apply {
          put("channelName", channelName)
          put("uid", 0)
          put("role", role)
        }.toString()
        val requestBuilder = Request.Builder()
          .url(agoraTokenEndpoint)
          .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))

        val supaAnon = BuildConfig.SUPABASE_ANON_KEY.trim().let { raw ->
          if (raw.startsWith("Sb_publishable_")) "sb_publishable_" + raw.removePrefix("Sb_publishable_") else raw
        }
        if (agoraTokenEndpoint.contains(".supabase.co", ignoreCase = true) &&
          supaAnon.isNotBlank() &&
          supaAnon != "your-supabase-anon-key"
        ) {
          requestBuilder.addHeader("apikey", supaAnon)
          requestBuilder.addHeader("Authorization", "Bearer $supaAnon")
        }

        SupabaseNetworkClient.sharedOkHttpClient.newCall(requestBuilder.build()).execute().use { resp ->
          val rawBody = resp.body?.string().orEmpty().trim()
          if (!resp.isSuccessful) {
            val serverErr = runCatching { JSONObject(rawBody).optString("error") }.getOrDefault("")
            throw IllegalStateException(serverErr.ifBlank { "HTTP ${resp.code}" })
          }
          if (rawBody.startsWith("{")) {
            val json = JSONObject(rawBody)
            val token = json.optString("token").trim()
            val uid = json.optInt("uid", 0)
            if (token.isBlank()) {
              val msg = json.optString("error").ifBlank { json.optString("message") }
              throw IllegalStateException(
                if (msg.isNotBlank()) "Edge Function ने टोकन नहीं भेजा ($msg) — agora-token कोड डिप्लॉय करें"
                else "सर्वर से खाली टोकन मिला"
              )
            }
            FetchedToken(token = token, uid = uid)
          } else {
            require(rawBody.isNotBlank()) { "सर्वर से खाली टोकन मिला" }
            FetchedToken(token = rawBody, uid = 0)
          }
        }
      }
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
