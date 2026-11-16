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
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipInputStream
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

  private const val MAX_GIF_UPLOAD_BYTES = 10L * 1024L * 1024L
  private const val MAX_ZIP_ARCHIVE_BYTES = 25L * 1024L * 1024L
  private const val MAX_ZIP_ENTRY_BYTES = 10L * 1024L * 1024L

  data class PreparedMedia(
    val file: File,
    val mimeType: String,
    val extension: String,
    val originalBytes: Long,
    val finalBytes: Long,
  )

  data class ExtractedZipEntry(
    val label: String,
    val tempUri: Uri,
    val tempFile: File,
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
    val mime = resolver.getType(uri).orEmpty().lowercase(Locale.ROOT)
    val displayName = queryUriDisplayName(context, uri).lowercase(Locale.ROOT)

    val header = ByteArray(12)
    val headerLen = resolver.openInputStream(uri)?.use { stream ->
      readFullyUpTo(stream, header)
    } ?: throw IllegalStateException("चयनित फ़ाइल पढ़ी नहीं जा सकी।")

    if (isGifHeader(header, headerLen) || mime == "image/gif" || displayName.endsWith(".gif")) {
      val cacheDir = File(context.cacheDir, "swar_upload_tmp").apply { mkdirs() }
      val outFile = File(cacheDir, "gif_${UUID.randomUUID().toString().take(8)}.gif")
      val copied = copyUriToTempFile(context, uri, outFile)
        ?: throw IllegalStateException("GIF फ़ाइल पढ़ी नहीं जा सकी।")
      val finalSize = copied.length()
      if (finalSize <= 0L) {
        copied.delete()
        throw IllegalStateException("चयनित GIF फ़ाइल खाली है।")
      }
      if (finalSize > MAX_GIF_UPLOAD_BYTES) {
        copied.delete()
        throw IllegalStateException("GIF फ़ाइल बहुत बड़ी है (${formatMb(finalSize)})। कृपया 10 MB से छोटी GIF चुनें।")
      }
      Log.i(TAG, "GIF preserved ($folder): ${finalSize / 1024}KB")
      return PreparedMedia(
        file = copied,
        mimeType = "image/gif",
        extension = "gif",
        originalBytes = if (originalBytes > 0L) originalBytes else finalSize,
        finalBytes = finalSize,
      )
    }

    if (isZipHeader(header, headerLen) || mime.contains("zip") || displayName.endsWith(".zip")) {
      val extracted = extractZipEntriesSync(context, uri, maxCount = 1)
      if (extracted.isEmpty()) {
        throw IllegalStateException("इस ZIP फ़ाइल में कोई मान्य GIF या इमेज (.gif, .png, .jpg, .webp) नहीं मिली।")
      }
      val first = extracted.first()
      return try {
        prepareImage(context, first.tempUri, folder)
      } finally {
        runCatching { first.tempFile.delete() }
      }
    }

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
      throw IllegalStateException("अमान्य इमेज फ़ाइल चुनी गई है। कृपया GIF, PNG, JPG या ZIP चुनें।")
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

  suspend fun isZipArchive(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
    val mime = context.contentResolver.getType(uri).orEmpty().lowercase(Locale.ROOT)
    val name = queryUriDisplayName(context, uri).lowercase(Locale.ROOT)
    if (mime.contains("zip") || name.endsWith(".zip")) return@withContext true
    val header = ByteArray(4)
    val len = runCatching {
      context.contentResolver.openInputStream(uri)?.use { readFullyUpTo(it, header) } ?: 0
    }.getOrDefault(0)
    isZipHeader(header, len)
  }

  suspend fun extractZipEntries(
    context: Context,
    uri: Uri,
    maxCount: Int = 20,
  ): List<ExtractedZipEntry> = withContext(Dispatchers.IO) {
    extractZipEntriesSync(context, uri, maxCount)
  }

  private fun extractZipEntriesSync(
    context: Context,
    uri: Uri,
    maxCount: Int = 20,
  ): List<ExtractedZipEntry> {
    val size = queryUriSizeBytes(context, uri)
    if (size > MAX_ZIP_ARCHIVE_BYTES) {
      throw IllegalStateException("ZIP फ़ाइल बहुत बड़ी है (${formatMb(size)})। कृपया 25 MB से छोटी ZIP चुनें।")
    }
    val cacheDir = File(context.cacheDir, "swar_zip_tmp").apply { mkdirs() }
    val gifResults = mutableListOf<ExtractedZipEntry>()
    val otherResults = mutableListOf<ExtractedZipEntry>()

    context.contentResolver.openInputStream(uri)?.use { raw ->
      ZipInputStream(BufferedInputStream(raw)).use { zis ->
        var entry = zis.nextEntry
        var totalCollected = 0
        while (entry != null && totalCollected < maxCount * 2) {
          val rawName = entry.name.orEmpty()
          val baseName = rawName.substringAfterLast('/').substringAfterLast('\\')
          val lower = baseName.lowercase(Locale.ROOT)
          val isIgnored = entry.isDirectory ||
            rawName.contains("__MACOSX", ignoreCase = true) ||
            baseName.startsWith(".")
          val ext = when {
            lower.endsWith(".gif") -> "gif"
            lower.endsWith(".webp") -> "webp"
            lower.endsWith(".png") -> "png"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "jpg"
            else -> ""
          }
          if (!isIgnored && ext.isNotEmpty()) {
            val bytes = readEntryBounded(zis, MAX_ZIP_ENTRY_BYTES)
            if (bytes != null && bytes.isNotEmpty()) {
              val outFile = File(cacheDir, "zip_${UUID.randomUUID().toString().take(8)}.$ext")
              outFile.writeBytes(bytes)
              val cleanLabel = baseName.substringBeforeLast('.')
                .replace('_', ' ')
                .replace('-', ' ')
                .trim()
                .take(30)
                .ifBlank { "स्टिकर ${totalCollected + 1}" }
              val item = ExtractedZipEntry(
                label = cleanLabel,
                tempUri = Uri.fromFile(outFile),
                tempFile = outFile,
              )
              if (ext == "gif") gifResults += item else otherResults += item
              totalCollected++
            }
          }
          zis.closeEntry()
          entry = zis.nextEntry
        }
      }
    } ?: throw IllegalStateException("ZIP फ़ाइल खोली नहीं जा सकी।")

    val combined = (gifResults + otherResults)
    val keep = combined.take(maxCount)
    combined.drop(maxCount).forEach { runCatching { it.tempFile.delete() } }
    return keep
  }

  fun queryUriDisplayName(context: Context, uri: Uri): String {
    if (uri.scheme == "file") {
      return uri.lastPathSegment.orEmpty()
    }
    runCatching {
      context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          if (idx >= 0 && !cursor.isNull(idx)) {
            return cursor.getString(idx).orEmpty()
          }
        }
      }
    }
    return uri.lastPathSegment.orEmpty()
  }

  private fun readFullyUpTo(input: InputStream, buf: ByteArray): Int {
    var total = 0
    while (total < buf.size) {
      val read = input.read(buf, total, buf.size - total)
      if (read <= 0) break
      total += read
    }
    return total
  }

  private fun readEntryBounded(zis: ZipInputStream, maxBytes: Long): ByteArray? {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    var total = 0L
    while (true) {
      val count = zis.read(buffer)
      if (count <= 0) break
      total += count
      if (total > maxBytes) return null
      out.write(buffer, 0, count)
    }
    return out.toByteArray()
  }

  private fun isGifHeader(header: ByteArray, len: Int): Boolean {
    if (len < 6) return false
    return header[0] == 'G'.code.toByte() &&
      header[1] == 'I'.code.toByte() &&
      header[2] == 'F'.code.toByte() &&
      header[3] == '8'.code.toByte() &&
      (header[4] == '7'.code.toByte() || header[4] == '9'.code.toByte()) &&
      header[5] == 'a'.code.toByte()
  }

  private fun isZipHeader(header: ByteArray, len: Int): Boolean {
    if (len < 4) return false
    return header[0] == 0x50.toByte() &&
      header[1] == 0x4B.toByte() &&
      (header[2] == 0x03.toByte() || header[2] == 0x05.toByte() || header[2] == 0x07.toByte()) &&
      (header[3] == 0x04.toByte() || header[3] == 0x06.toByte() || header[3] == 0x08.toByte())
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

  suspend fun extractVideoThumbnailFile(context: Context, videoUri: Uri): File? = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    try {
      retriever.setDataSource(context, videoUri)
      val raw = retriever.getFrameAtTime(500_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        ?: retriever.getFrameAtTime(0L)
        ?: return@withContext null
      val scaled = scaleDownIfNeeded(raw, 512)
      if (scaled !== raw) {
        raw.recycle()
      }
      try {
        val cacheDir = File(context.cacheDir, "swar_upload_tmp").apply { mkdirs() }
        val outFile = File(cacheDir, "vthumb_${UUID.randomUUID().toString().take(8)}.jpg")
        outFile.outputStream().use { out ->
          scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        outFile.takeIf { it.length() > 0L }
      } finally {
        if (!scaled.isRecycled) {
          scaled.recycle()
        }
      }
    } catch (t: Throwable) {
      Log.w(TAG, "Video thumbnail extraction failed", t)
      null
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
