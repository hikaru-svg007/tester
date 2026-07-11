package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import java.io.ByteArrayOutputStream

object FileUtils {

    /**
     * Reads a document Uri as a plain text string.
     */
    fun readUriAsText(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Resolves the size of a Uri in bytes.
     */
    fun getUriFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                it.length
            } ?: -1L
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * Resolves the display name of a Uri.
     */
    fun getUriFileName(context: Context, uri: Uri): String {
        var name = "file"
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = it.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return name
    }

    /**
     * Compresses the selected image Uri to a JPEG Base64 representation.
     * Prevents out-of-memory or high memory usage on larger captures.
     */
    fun compressUriToJpegBase64(context: Context, uri: Uri): Pair<String, String>? {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
            
            // Undergo sampling to load efficiently
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            // Find scale value
            val maxDimension = 800
            var scale = 1
            if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                scale = Math.pow(
                    2.0,
                    Math.ceil(
                        Math.log(
                            maxDimension.toDouble() / Math.max(options.outHeight, options.outWidth)
                        ) / Math.log(0.5)
                    )
                ).toInt()
            }

            // Load decoded bitmap with appropriate downscale factor
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            val b = contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            } ?: return null

            // Generate scale to exact limit if needed
            val width = b.width
            val height = b.height
            val finalBitmap = if (width > maxDimension || height > maxDimension) {
                val ratio = width.toFloat() / height.toFloat()
                val (newW, newH) = if (ratio > 1) {
                    Pair(maxDimension, (maxDimension / ratio).toInt())
                } else {
                    Pair((maxDimension * ratio).toInt(), maxDimension)
                }
                Bitmap.createScaledBitmap(b, newW, newH, true)
            } else {
                b
            }

            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            val jpegBytes = outputStream.toByteArray()
            val base64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
            
            Pair(base64, "image/jpeg")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Checks if a file URI or name is a video.
     */
    fun isVideoFile(context: Context, uri: Uri, fileName: String): Boolean {
        return try {
            val mimeType = context.contentResolver.getType(uri) ?: ""
            if (mimeType.startsWith("video/")) return true
            val ext = fileName.substringAfterLast('.', "").lowercase()
            ext in listOf("mp4", "mkv", "3gp", "mov", "webm", "avi", "flv")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Extracts evenly-spaced frames from a video Uri as base64 strings.
     */
    fun extractVideoFramesAsBase64(
        context: Context, 
        uri: Uri, 
        maxFrames: Int = 120, 
        targetIntervalMs: Long = 1000L
    ): List<String> {
        val frames = mutableListOf<String>()
        val retriever = android.media.MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            if (durationMs > 0) {
                // Target extracting 1 frame every targetIntervalMs
                var frameCount = (durationMs / targetIntervalMs).toInt()
                if (frameCount < 3) frameCount = 3
                if (frameCount > maxFrames) frameCount = maxFrames
                
                val intervalMs = durationMs / (frameCount + 1)
                for (i in 1..frameCount) {
                    val timeUs = (i * intervalMs) * 1000L
                    val bitmap = retriever.getFrameAtTime(timeUs, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (bitmap != null) {
                        val scaled = resizeBitmap(bitmap, 480) // 480px width is perfect & extremely lightweight
                        val outputStream = ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                        val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                        frames.add(base64)
                        if (scaled != bitmap) {
                            scaled.recycle()
                        }
                        bitmap.recycle()
                    }
                }
            } else {
                val bitmap = retriever.getFrameAtTime(0L)
                if (bitmap != null) {
                    val scaled = resizeBitmap(bitmap, 480)
                    val outputStream = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                    val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                    frames.add(base64)
                    if (scaled != bitmap) {
                        scaled.recycle()
                    }
                    bitmap.recycle()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return frames
    }

    fun parseHlsPlaylist(videoUrl: String, maxSegments: Int = 5): Pair<String?, List<String>> {
        var mapUrl: String? = null
        val segmentUrls = mutableListOf<String>()
        try {
            val browserUserAgent = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            var currentUrl = videoUrl
            var content = ""
            
            // Limit redirects or sub-playlist checks up to 3 times
            for (redirect in 1..3) {
                val request = okhttp3.Request.Builder()
                    .url(currentUrl)
                    .header("User-Agent", browserUserAgent)
                    .header("Referer", videoUrl)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return Pair(null, emptyList())
                    content = response.body?.string() ?: ""
                }
                
                if (content.contains("#EXT-X-STREAM-INF")) {
                    // This is a Master Playlist. Find a variant playlist line.
                    val lines = content.lines()
                    var subPath = ""
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                            subPath = trimmed
                            break
                        }
                    }
                    if (subPath.isNotBlank()) {
                        currentUrl = if (subPath.startsWith("http://") || subPath.startsWith("https://")) {
                            subPath
                        } else {
                            val base = currentUrl.substringBeforeLast("/")
                            "$base/$subPath"
                        }
                    } else {
                        break
                    }
                } else {
                    break
                }
            }
            
            if (content.contains("#EXTINF")) {
                val lines = content.lines()
                
                // Parse #EXT-X-MAP:URI="init.mp4"
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("#EXT-X-MAP:")) {
                        val uriRegex = java.util.regex.Pattern.compile("URI=\"([^\"]+)\"")
                        val matcher = uriRegex.matcher(trimmed)
                        if (matcher.find()) {
                            val rawMap = matcher.group(1) ?: ""
                            mapUrl = if (rawMap.startsWith("http://") || rawMap.startsWith("https://")) {
                                rawMap
                            } else {
                                val base = currentUrl.substringBeforeLast("/")
                                "$base/$rawMap"
                            }
                        }
                    }
                }

                val allSegments = mutableListOf<String>()
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                        val segmentUrl = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                            trimmed
                        } else {
                            val base = currentUrl.substringBeforeLast("/")
                            "$base/$trimmed"
                        }
                        allSegments.add(segmentUrl)
                    }
                }
                
                if (allSegments.isNotEmpty()) {
                    val total = allSegments.size
                    if (maxSegments <= 1) {
                        segmentUrls.add(allSegments[0])
                    } else if (total <= maxSegments) {
                        segmentUrls.addAll(allSegments)
                    } else {
                        val step = (total - 1).toDouble() / (maxSegments - 1)
                        for (i in 0 until maxSegments) {
                            val index = (i * step).toInt().coerceIn(0, total - 1)
                            segmentUrls.add(allSegments[index])
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(mapUrl, segmentUrls)
    }

    /**
     * Extracts evenly-spaced frames from a remote video URL as base64 strings.
     * Tries direct retrieval via HTTP range requests first (super fast).
     * Falls back to pre-downloading with a robust timeout and browser headers if direct fails.
     */
    fun extractVideoFramesFromUrlAsBase64(context: Context, videoUrl: String, maxFrames: Int = 5): List<String> {
        val frames = mutableListOf<String>()
        val browserUserAgent = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        
        val resolvedUrl = kotlinx.coroutines.runBlocking {
            VideoLinkExtractor.resolveDirectVideoUrl(videoUrl)
        }
        
        if (resolvedUrl.contains(".m3u8", ignoreCase = true)) {
            val (mapUrl, segmentUrls) = parseHlsPlaylist(resolvedUrl, maxFrames)
            val hlsFrames = mutableListOf<String>()
            
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build()
                
            // Download the initialization map if it exists
            var initBytes: ByteArray? = null
            if (mapUrl != null) {
                try {
                    val request = okhttp3.Request.Builder()
                        .url(mapUrl)
                        .header("User-Agent", browserUserAgent)
                        .header("Referer", resolvedUrl)
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            initBytes = response.body?.bytes()
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FileUtils", "Failed to download HLS map segment: $mapUrl", e)
                }
            }

            for (segUrl in segmentUrls) {
                var extracted = false
                
                // Strategy A: Direct native stream retrieval from segment URL (super fast, avoids physical downloads)
                val directRet = android.media.MediaMetadataRetriever()
                try {
                    val headers = java.util.HashMap<String, String>()
                    headers["User-Agent"] = browserUserAgent
                    headers["Referer"] = resolvedUrl
                    directRet.setDataSource(segUrl, headers)
                    val bitmap = directRet.getFrameAtTime() ?: directRet.getFrameAtTime(0L, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (bitmap != null) {
                        val scaled = resizeBitmap(bitmap, 512)
                        val outputStream = ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                        val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                        hlsFrames.add(base64)
                        android.util.Log.d("FileUtils", "Successfully extracted frame via direct stream from HLS segment: $segUrl")
                        extracted = true
                    }
                } catch (e: Exception) {
                    android.util.Log.w("FileUtils", "Direct stream extraction failed for segment $segUrl: ${e.message}. Trying download fallback.")
                } finally {
                    try {
                        directRet.release()
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }

                if (!extracted) {
                    // Strategy B: Download fallback
                    var tempFile: java.io.File? = null
                    try {
                        val request = okhttp3.Request.Builder()
                            .url(segUrl)
                            .header("User-Agent", browserUserAgent)
                            .header("Referer", resolvedUrl)
                            .build()
                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val segmentBytes = response.body?.bytes()
                                if (segmentBytes != null) {
                                    val suffix = if (segUrl.contains(".mp4") || mapUrl != null) ".mp4" else ".ts"
                                    val tf = java.io.File.createTempFile("hls_seg_tmp_", suffix, context.cacheDir)
                                    tf.outputStream().use { fos ->
                                        if (initBytes != null) {
                                            fos.write(initBytes!!)
                                        }
                                        fos.write(segmentBytes)
                                    }
                                    tempFile = tf
                                }
                            }
                        }
                        
                        if (tempFile != null && tempFile.exists() && tempFile.length() > 0) {
                            val retriever = android.media.MediaMetadataRetriever()
                            try {
                                retriever.setDataSource(tempFile.absolutePath)
                                val bitmap = retriever.getFrameAtTime() ?: retriever.getFrameAtTime(0L, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                if (bitmap != null) {
                                    val scaled = resizeBitmap(bitmap, 512)
                                    val outputStream = ByteArrayOutputStream()
                                    scaled.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                                    val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                                    hlsFrames.add(base64)
                                    android.util.Log.d("FileUtils", "Successfully extracted frame from HLS segment file $segUrl")
                                } else {
                                    android.util.Log.w("FileUtils", "Retriever returned null frame for HLS segment file $segUrl")
                                }
                            } finally {
                                try {
                                    retriever.release()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("FileUtils", "Failed to extract frame from HLS segment file $segUrl: ${e.message}")
                    } finally {
                        try {
                            tempFile?.delete()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            if (hlsFrames.isNotEmpty()) {
                return hlsFrames
            }
        }

        val headers = java.util.HashMap<String, String>()
        headers["User-Agent"] = browserUserAgent
        headers["Referer"] = resolvedUrl

        // Strategy 1: Direct HTTP range requests (very fast, uses minimal bandwidth)
        val directRetriever = android.media.MediaMetadataRetriever()
        var directSuccess = false
        try {
            directRetriever.setDataSource(resolvedUrl, headers)
            val durationStr = directRetriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            if (durationMs > 0) {
                val intervalMs = durationMs / (maxFrames + 1)
                for (i in 1..maxFrames) {
                    val timeUs = (i * intervalMs) * 1000L
                    val bitmap = directRetriever.getFrameAtTime(timeUs, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (bitmap != null) {
                        val scaled = resizeBitmap(bitmap, 512)
                        val outputStream = ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                        val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                        frames.add(base64)
                    }
                }
                if (frames.isNotEmpty()) {
                    directSuccess = true
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("FileUtils", "Direct stream frame extraction failed or restricted: ${e.message}. Trying download cache.")
        } finally {
            try {
                directRetriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (directSuccess) {
            return frames
        }

        // Strategy 2: Pre-download video with browser headers and generous timeouts
        val retriever = android.media.MediaMetadataRetriever()
        var tempFile: java.io.File? = null
        try {
            try {
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val request = okhttp3.Request.Builder()
                    .url(resolvedUrl)
                    .header("User-Agent", browserUserAgent)
                    .header("Referer", resolvedUrl)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body
                        if (body != null) {
                            val suffix = if (resolvedUrl.lowercase().endsWith(".mkv")) ".mkv" else ".mp4"
                            val tf = java.io.File.createTempFile("remote_video_extract_", suffix, context.cacheDir)
                            tf.outputStream().use { fos ->
                                body.byteStream().copyTo(fos)
                            }
                            tempFile = tf
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FileUtils", "Failed to pre-download remote video for frame extraction: ${e.message}")
            }

            if (tempFile != null && tempFile!!.exists() && tempFile!!.length() > 0) {
                retriever.setDataSource(tempFile!!.absolutePath)
                val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                val durationMs = durationStr?.toLongOrNull() ?: 0L
                if (durationMs > 0) {
                    val intervalMs = durationMs / (maxFrames + 1)
                    for (i in 1..maxFrames) {
                        val timeUs = (i * intervalMs) * 1000L
                        val bitmap = retriever.getFrameAtTime(timeUs, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        if (bitmap != null) {
                            val scaled = resizeBitmap(bitmap, 512)
                            val outputStream = ByteArrayOutputStream()
                            scaled.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                            val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                            frames.add(base64)
                        }
                    }
                } else {
                    val bitmap = retriever.getFrameAtTime(0L)
                    if (bitmap != null) {
                        val scaled = resizeBitmap(bitmap, 512)
                        val outputStream = ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                        val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                        frames.add(base64)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                tempFile?.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return frames
    }

    /**
     * Extracts total duration from HLS (.m3u8) playlist file using standard browser User-Agent headers.
     */
    fun extractHlsDurationMsFromUrl(videoUrl: String): Long {
        try {
            val browserUserAgent = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            var currentUrl = videoUrl
            var content = ""
            
            // Limit redirects or sub-playlist checks up to 3 times
            for (redirect in 1..3) {
                val request = okhttp3.Request.Builder()
                    .url(currentUrl)
                    .header("User-Agent", browserUserAgent)
                    .header("Referer", videoUrl)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return 0L
                    content = response.body?.string() ?: ""
                }
                
                if (content.contains("#EXT-X-STREAM-INF")) {
                    // This is a Master Playlist. Find a variant playlist line.
                    val lines = content.lines()
                    var subPath = ""
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                            subPath = trimmed
                            break
                        }
                    }
                    if (subPath.isNotBlank()) {
                        currentUrl = if (subPath.startsWith("http://") || subPath.startsWith("https://")) {
                            subPath
                        } else {
                            val base = currentUrl.substringBeforeLast("/")
                            "$base/$subPath"
                        }
                    } else {
                        break
                    }
                } else {
                    break
                }
            }
            
            if (content.contains("#EXTINF")) {
                var totalSeconds = 0.0
                val lines = content.lines()
                for (line in lines) {
                    if (line.startsWith("#EXTINF:")) {
                        val valPart = line.substringAfter("#EXTINF:").substringBefore(",")
                        val seconds = valPart.toDoubleOrNull()
                        if (seconds != null) {
                            totalSeconds += seconds
                        }
                    }
                }
                return (totalSeconds * 1000).toLong()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0L
    }

    /**
     * Unified duration retriever supporting local files, content URIs, regular remote videos, and HLS .m3u8 streams.
     * Sends custom browser User-Agent headers to ensure CDNs don't block metadata queries.
     */
    fun getVideoDurationMs(context: Context, pathOrUrl: String): Long {
        if (pathOrUrl.contains(".m3u8", ignoreCase = true)) {
            return extractHlsDurationMsFromUrl(pathOrUrl)
        }
        val retriever = android.media.MediaMetadataRetriever()
        try {
            if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
                val browserUserAgent = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                val headers = java.util.HashMap<String, String>()
                headers["User-Agent"] = browserUserAgent
                headers["Referer"] = pathOrUrl
                retriever.setDataSource(pathOrUrl, headers)
            } else if (pathOrUrl.startsWith("content://")) {
                retriever.setDataSource(context, android.net.Uri.parse(pathOrUrl))
            } else {
                retriever.setDataSource(pathOrUrl)
            }
            val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            return durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return 0L
    }

    /**
     * Extracts video URLs from a text string.
     */
    fun extractVideoUrls(text: String): List<String> {
        val urls = mutableListOf<String>()
        try {
            val pattern = java.util.regex.Pattern.compile(
                "https?://[a-zA-Z0-9.\\-_]+(?:/[a-zA-Z0-9.\\-_#?&=~\\+!*'(),;:@%]+)*",
                java.util.regex.Pattern.CASE_INSENSITIVE
            )
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                val url = matcher.group()
                val lowerUrl = url.lowercase()
                val cleanUrl = url.substringBefore("?").substringBefore("#").lowercase()
                if (lowerUrl.contains("youtube.com", ignoreCase = true) || 
                    lowerUrl.contains("youtu.be", ignoreCase = true) || 
                    lowerUrl.contains("tiktok.com", ignoreCase = true) || 
                    lowerUrl.contains("vimeo.com", ignoreCase = true) || 
                    lowerUrl.contains("instagram.com", ignoreCase = true) || 
                    lowerUrl.contains("facebook.com", ignoreCase = true) || 
                    cleanUrl.endsWith(".mp4", ignoreCase = true) || 
                    cleanUrl.endsWith(".mov", ignoreCase = true) || 
                    cleanUrl.endsWith(".mkv", ignoreCase = true) || 
                    cleanUrl.endsWith(".webm", ignoreCase = true) || 
                    cleanUrl.endsWith(".m3u8", ignoreCase = true) ||
                    lowerUrl.contains(".m3u8", ignoreCase = true) ||
                    lowerUrl.contains(".mp4", ignoreCase = true) ||
                    lowerUrl.contains(".webm", ignoreCase = true) ||
                    lowerUrl.contains(".mkv", ignoreCase = true) ||
                    lowerUrl.contains("video-stream", ignoreCase = true) ||
                    lowerUrl.contains("raw-video", ignoreCase = true) ||
                    cleanUrl.endsWith(".3gp", ignoreCase = true)
                ) {
                    if (!urls.contains(url)) {
                        urls.add(url)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return urls
    }

    /**
     * Resizes bitmap to ensure low footprint.
     */
    fun resizeBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Saves a Uri's content to a file in the app's cache directory and returns its absolute path.
     */
    fun saveUriToCacheFile(context: Context, uri: Uri, fileName: String): String? {
        return try {
            val cleanName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val cacheFile = java.io.File(context.cacheDir, "attached_" + System.currentTimeMillis() + "_" + cleanName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                cacheFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            cacheFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Clears all cached attached files starting with "attached_" to save storage.
     */
    fun clearAttachedVideoCache(context: Context) {
        try {
            val cacheDir = context.cacheDir
            val files = cacheDir.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isFile && file.name.startsWith("attached_")) {
                        val deleted = file.delete()
                        android.util.Log.d("FileUtils", "Auto-deleted cache file: ${file.name}, success: $deleted")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
