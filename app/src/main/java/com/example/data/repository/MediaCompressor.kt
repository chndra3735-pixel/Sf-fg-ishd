package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.min

/**
 * Automatically shrinks arbitrarily large photos (DP, room covers, moments, emojis, thumbnails)
 * and videos (moments, full-screen gifts) down to compact MB/KB files before uploading.
 */
object MediaCompressor {
  private const val TAG = "MediaCompressor"
  private const val MAX_FINAL_UPLOAD_BYTES = 45L * 1024L * 1024L

  data class PreparedMedia(
    val file: File,
    val mimeType: String,
    val extension: String,
    val originalBytes: Long,
    val finalBytes: Long,
  )

  suspend fun prepareForUpload(
    context: Context,
    uri: Uri,
    folder: String,
    isVideo: Boolean,
  ): PreparedMedia {
    return if (isVideo) {
      prepareVideo(context, uri, folder)
    } else {
      withContext(Dispatchers.IO) {
        prepareImage(context, uri, folder)
      }
    }
  }

  private fun prepareImage(
    context: Context,
    uri: Uri,
    folder: String,
  ): PreparedMedia {
    val resolver = context.contentResolver
    val originalBytes = queryUriSizeBytes(context, uri)
    val maxDimension = when (folder) {
      "emojis" -> 512
      "dp", "gifts" -> 720
      else -> 1280
    }
    val targetBytes = when (folder) {
      "emojis" -> 160 * 1024L
      "dp", "gifts" -> 260 * 1024L
      else -> 520 * 1024L
    }

    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { stream ->
      BitmapFactory.decodeStream(stream, null, boundsOpts)
    } ?: throw IllegalStateException("चयनित फ़ोटो पढ़ी नहीं जा सकी।")

    if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) {
      throw IllegalStateException("अमान्य इमेज फ़ाइल चुनी गई है।")
    }

    val orientation = runCatching {
      resolver.openInputStream(uri)?.use { stream ->
        ExifInterface(stream).getAttributeInt(
          ExifInterface.TAG_ORIENTATION,
          ExifInterface.ORIENTATION_NORMAL,
        )
      } ?: ExifInterface.ORIENTATION_NORMAL
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    val sampleSize = computeInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, maxDimension)
    val decodeOpts = BitmapFactory.Options().apply {
      inSampleSize = sampleSize
      inPreferredConfig = Bitmap.Config.ARGB_8888
    }

    var decoded = resolver.openInputStream(uri)?.use { stream ->
      BitmapFactory.decodeStream(stream, null, decodeOpts)
    } ?: throw IllegalStateException("फ़ोटो डिकोड नहीं हो सकी।")

    try {
      val oriented = applyExifTransform(decoded, orientation)
      if (oriented !== decoded) {
        decoded.recycle()
        decoded = oriented
      }

      val scaled = scaleDownIfNeeded(decoded, maxDimension)
      if (scaled !== decoded) {
        decoded.recycle()
        decoded = scaled
      }

      val keepTransparency = decoded.hasAlpha() && (folder == "emojis" || folder == "gifts")
      val format = if (keepTransparency) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          Bitmap.CompressFormat.WEBP_LOSSY
        } else {
          @Suppress("DEPRECATION")
          Bitmap.CompressFormat.WEBP
        }
      } else {
        Bitmap.CompressFormat.JPEG
      }
      val ext = if (keepTransparency) "webp" else "jpg"
      val mimeType = if (keepTransparency) "image/webp" else "image/jpeg"

      val cacheDir = File(context.cacheDir, "swar_upload_tmp").apply { mkdirs() }
      val outFile = File(cacheDir, "img_${UUID.randomUUID().toString().take(8)}.$ext")

      var quality = 84
      var bestBytes: ByteArray
      while (true) {
        val baos = ByteArrayOutputStream()
        decoded.compress(format, quality, baos)
        bestBytes = baos.toByteArray()
        if (bestBytes.size <= targetBytes || quality <= 48) {
          break
        }
        quality -= 10
      }

      outFile.writeBytes(bestBytes)
      val finalSize = outFile.length()
      Log.i(TAG, "Image compressed ($folder): ${originalBytes / 1024}KB -> ${finalSize / 1024}KB (q=$quality)")
      return PreparedMedia(
        file = outFile,
        mimeType = mimeType,
        extension = ext,
        originalBytes = originalBytes,
        finalBytes = finalSize,
      )
    } finally {
      if (!decoded.isRecycled) {
        decoded.recycle()
      }
    }
  }

  private suspend fun prepareVideo(
    context: Context,
    uri: Uri,
    folder: String,
  ): PreparedMedia {
    val resolver = context.contentResolver
    val detectedMime = resolver.getType(uri).orEmpty()
    val originalBytes = queryUriSizeBytes(context, uri)
    val meta = withContext(Dispatchers.IO) { readVideoMetadata(context, uri) }

    val cacheDir = File(context.cacheDir, "swar_upload_tmp").apply { mkdirs() }

    // Already small MP4 (< 2.5 MB): copy to temp stream file directly for instant upload.
    if (originalBytes in 1L..2_500_000L && detectedMime.contains("mp4")) {
      val directFile = withContext(Dispatchers.IO) {
        copyUriToTempFile(context, uri, File(cacheDir, "vid_raw_${UUID.randomUUID().toString().take(8)}.mp4"))
      }
      if (directFile != null && directFile.length() > 0L) {
        return PreparedMedia(
          file = directFile,
          mimeType = "video/mp4",
          extension = "mp4",
          originalBytes = originalBytes,
          finalBytes = directFile.length(),
        )
      }
    }

    val compressedFile = File(cacheDir, "vid_cmp_${UUID.randomUUID().toString().take(8)}.mp4")
    val targetTotalMb = if (folder == "gifts") 4.5 else 7.5
    val durationSec = (meta.durationMs / 1000.0).coerceAtLeast(1.0)
    val targetTotalBits = targetTotalMb * 1024.0 * 1024.0 * 8.0
    val computedVideoBps = ((targetTotalBits / durationSec) - 96_000.0)
      .toInt()
      .coerceIn(320_000, 950_000)

    val transcodeSucceeded = runCatching {
      transcodeVideoWithMedia3(
        context = context,
        inputUri = uri,
        outputFile = compressedFile,
        sourceWidth = meta.width,
        sourceHeight = meta.height,
        targetBitrateBps = computedVideoBps,
      )
    }.onFailure { err ->
      Log.w(TAG, "Media3 video compression failed: ${err.message}", err)
    }.isSuccess

    if (transcodeSucceeded && compressedFile.exists() && compressedFile.length() > 0L) {
      val compressedBytes = compressedFile.length()
      if (compressedBytes <= MAX_FINAL_UPLOAD_BYTES) {
        Log.i(
          TAG,
          "Video compressed ($folder): ${formatMb(originalBytes)} -> ${formatMb(compressedBytes)} (bps=$computedVideoBps)",
        )
        return PreparedMedia(
          file = compressedFile,
          mimeType = "video/mp4",
          extension = "mp4",
          originalBytes = originalBytes,
          finalBytes = compressedBytes,
        )
      }
    }

    compressedFile.delete()

    // Fallback if device codec couldn't transcode and original file itself fits within upload limit.
    val fallbackFile = withContext(Dispatchers.IO) {
      copyUriToTempFile(context, uri, File(cacheDir, "vid_fb_${UUID.randomUUID().toString().take(8)}.mp4"))
    }
    if (fallbackFile != null && fallbackFile.length() in 1L..MAX_FINAL_UPLOAD_BYTES) {
      val ext = if (detectedMime.contains("quicktime") || detectedMime.contains("mov")) "mov" else "mp4"
      val mime = if (ext == "mov") "video/quicktime" else "video/mp4"
      return PreparedMedia(
        file = fallbackFile,
        mimeType = mime,
        extension = ext,
        originalBytes = originalBytes,
        finalBytes = fallbackFile.length(),
      )
    }
    fallbackFile?.delete()
    throw IllegalStateException("यह वीडियो बहुत बड़ा है या कम्प्रेश नहीं हो सका। कृपया छोटा वीडियो क्लिप चुनें।")
  }

  @OptIn(UnstableApi::class)
  private suspend fun transcodeVideoWithMedia3(
    context: Context,
    inputUri: Uri,
    outputFile: File,
    sourceWidth: Int,
    sourceHeight: Int,
    targetBitrateBps: Int,
  ) = withContext(Dispatchers.Main.immediate) {
    suspendCancellableCoroutine { continuation ->
      val mediaItem = MediaItem.fromUri(inputUri)
      val editedBuilder = EditedMediaItem.Builder(mediaItem)

      val shortSide = min(sourceWidth, sourceHeight)
      if (sourceWidth > 0 && sourceHeight > 0 && shortSide > 480) {
        val targetHeight = if (sourceHeight > sourceWidth) {
          // Portrait video: keep short width ~480px by calculating proportional even height.
          val scaledH = ((480.0 * sourceHeight / sourceWidth).toInt() / 2) * 2
          scaledH.coerceIn(480, 854)
        } else {
          // Landscape / square video: crisp 480p height.
          480
        }
        editedBuilder.setEffects(
          Effects(
            emptyList(),
            listOf(Presentation.createForHeight(targetHeight)),
          )
        )
      }

      val encoderFactory = DefaultEncoderFactory.Builder(context)
        .setRequestedVideoEncoderSettings(
          VideoEncoderSettings.Builder()
            .setBitrate(targetBitrateBps)
            .build()
        )
        .build()

      var transformerRef: Transformer? = null
      val listener = object : Transformer.Listener {
        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
          if (continuation.isActive) {
            continuation.resume(Unit)
          }
        }

        override fun onError(
          composition: Composition,
          exportResult: ExportResult,
          exportException: ExportException,
        ) {
          if (continuation.isActive) {
            continuation.resumeWithException(exportException)
          }
        }
      }

      val transformer = Transformer.Builder(context)
        .setVideoMimeType(MimeTypes.VIDEO_H264)
        .setAudioMimeType(MimeTypes.AUDIO_AAC)
        .setEncoderFactory(encoderFactory)
        .addListener(listener)
        .build()

      transformerRef = transformer
      continuation.invokeOnCancellation {
        runCatching { transformerRef?.cancel() }
      }
      transformer.start(editedBuilder.build(), outputFile.absolutePath)
    }
  }

  private data class VideoMeta(
    val width: Int,
    val height: Int,
    val durationMs: Long,
  )

  private fun readVideoMetadata(context: Context, uri: Uri): VideoMeta {
    val retriever = MediaMetadataRetriever()
    return try {
      retriever.setDataSource(context, uri)
      val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
      val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
      val rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
      val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
      val orientedW = if (rot == 90 || rot == 270) h else w
      val orientedH = if (rot == 90 || rot == 270) w else h
      VideoMeta(orientedW, orientedH, duration)
    } catch (_: Exception) {
      VideoMeta(0, 0, 0L)
    } finally {
      runCatching { retriever.release() }
    }
  }

  private fun queryUriSizeBytes(context: Context, uri: Uri): Long {
    val resolver = context.contentResolver
    runCatching {
      resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
          if (idx >= 0 && !cursor.isNull(idx)) {
            return cursor.getLong(idx)
          }
        }
      }
    }
    return runCatching {
      resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
    }.getOrDefault(0L)
  }

  private fun copyUriToTempFile(context: Context, uri: Uri, destination: File): File? {
    return runCatching {
      context.contentResolver.openInputStream(uri)?.use { input ->
        destination.outputStream().use { output ->
          input.copyTo(output)
        }
      } ?: return null
      destination
    }.getOrNull()
  }

  private fun computeInSampleSize(width: Int, height: Int, maxDim: Int): Int {
    var sample = 1
    while (width / (sample * 2) >= maxDim && height / (sample * 2) >= maxDim) {
      sample *= 2
    }
    return sample
  }

  private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
    val longest = max(bitmap.width, bitmap.height)
    if (longest <= maxDim) return bitmap
    val ratio = maxDim.toFloat() / longest.toFloat()
    val targetW = max(1, (bitmap.width * ratio).toInt())
    val targetH = max(1, (bitmap.height * ratio).toInt())
    return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
  }

  private fun applyExifTransform(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
      ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
      ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
      ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
      ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
      ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
      else -> return bitmap
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
  }

  private fun formatMb(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    return String.format(java.util.Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
  }
}
