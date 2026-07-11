package com.example.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object VideoLinkExtractor {
    private const val TAG = "VideoLinkExtractor"
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private const val browserUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    suspend fun resolveDirectVideoUrl(url: String): String = withContext(Dispatchers.IO) {
        val trimmed = url.trim()
        Log.d(TAG, "Resolving URL: $trimmed")
        try {
            // 1. Google Drive Link Extraction
            if (trimmed.contains("drive.google.com") || trimmed.contains("docs.google.com")) {
                val fileId = extractGoogleDriveFileId(trimmed)
                if (fileId != null) {
                    val driveDirectUrl = "https://docs.google.com/uc?export=download&id=$fileId"
                    Log.d(TAG, "Google Drive resolved to direct download CDN: $driveDirectUrl")
                    return@withContext driveDirectUrl
                }
            }

            // 2. Dropbox Link Extraction
            if (trimmed.contains("dropbox.com")) {
                // Change dl=0 to dl=1, or use direct cdn host dl.dropboxusercontent.com
                var directUrl = trimmed
                if (directUrl.contains("www.dropbox.com")) {
                    directUrl = directUrl.replace("www.dropbox.com", "dl.dropboxusercontent.com")
                }
                if (directUrl.contains("dl=0")) {
                    directUrl = directUrl.replace("dl=0", "dl=1")
                } else if (!directUrl.contains("dl=1")) {
                    directUrl = if (directUrl.contains("?")) "$directUrl&dl=1" else "$directUrl?dl=1"
                }
                Log.d(TAG, "Dropbox resolved to direct CDN stream: $directUrl")
                return@withContext directUrl
            }

            // 3. Mediafire Link Extraction
            if (trimmed.contains("mediafire.com")) {
                val directMediafire = scrapeMediafireDirectUrl(trimmed)
                if (directMediafire != null) {
                    Log.d(TAG, "Mediafire scraping resolved to: $directMediafire")
                    return@withContext directMediafire
                }
            }

            // 4. Any other page - check if it's already a direct video link or if we need to inspect headers/HTML
            val resolvedUrl = followRedirectsAndCheckType(trimmed)
            Log.d(TAG, "Final resolved URL: $resolvedUrl")
            return@withContext resolvedUrl
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving video URL $trimmed", e)
            return@withContext trimmed
        }
    }

    private fun extractGoogleDriveFileId(url: String): String? {
        val patterns = listOf(
            "/file/d/([a-zA-Z0-9-_]{25,})",
            "id=([a-zA-Z0-9-_]{25,})",
            "/d/([a-zA-Z0-9-_]{25,})"
        )
        for (pat in patterns) {
            val matcher = Pattern.compile(pat).matcher(url)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        return null
    }

    private fun scrapeMediafireDirectUrl(url: String): String? {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", browserUserAgent)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val html = response.body?.string() ?: ""
                
                // Match direct download href in Mediafire
                // Look for href="https://downloadXXX.mediafire.com/..."
                val mediafirePattern = Pattern.compile("href=\"(https?://download[0-9]*\\.mediafire\\.com/[^\"]+)\"")
                val matcher = mediafirePattern.matcher(html)
                if (matcher.find()) {
                    return matcher.group(1)
                }
                
                // Fallback: look for any .mp4 or .mkv url inside href
                val fallbackPattern = Pattern.compile("href=\"(https?://[^\"]+\\.(?:mp4|mkv|mov|webm|3gp|m3u8)[^\"]*)\"")
                val fbMatcher = fallbackPattern.matcher(html)
                if (fbMatcher.find()) {
                    return fbMatcher.group(1)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scraping Mediafire URL $url", e)
        }
        return null
    }

    private fun followRedirectsAndCheckType(url: String): String {
        try {
            // Check if it's already a direct media file extension
            val lower = url.lowercase()
            if (lower.contains(".mp4") || lower.contains(".mov") || lower.contains(".mkv") || 
                lower.contains(".webm") || lower.contains(".3gp") || lower.contains(".m3u8")
            ) {
                return url
            }

            // Execute a HEAD request to check HTTP Headers/Content-Type
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", browserUserAgent)
                .head()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val contentType = response.header("Content-Type") ?: ""
                    Log.d(TAG, "HEAD request returned Content-Type: $contentType, URL: ${response.request.url}")
                    if (contentType.startsWith("video/") || contentType.contains("application/x-mpegURL") || contentType.contains("application/vnd.apple.mpegurl")) {
                        return response.request.url.toString()
                    }
                }
            }

            // If HEAD wasn't conclusive, perform a GET and scan the HTML for streamable video source
            val getRequest = Request.Builder()
                .url(url)
                .header("User-Agent", browserUserAgent)
                .build()
            client.newCall(getRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val contentType = response.header("Content-Type") ?: ""
                    if (contentType.startsWith("video/")) {
                        return response.request.url.toString()
                    }
                    
                    val html = response.body?.string() ?: ""
                    // Try to extract <video src="..."> or <source src="...">
                    val videoSrcPattern = Pattern.compile("<video[^>]*src=\"([^\"]+)\"|<source[^>]*src=\"([^\"]+)\"")
                    val matcher = videoSrcPattern.matcher(html)
                    if (matcher.find()) {
                        val foundUrl = matcher.group(1) ?: matcher.group(2)
                        if (foundUrl != null) {
                            if (foundUrl.startsWith("http://") || foundUrl.startsWith("https://")) {
                                return foundUrl
                            } else {
                                // Resolve relative URL
                                val baseUri = response.request.url
                                val resolved = baseUri.newBuilder(foundUrl)?.build()
                                if (resolved != null) {
                                    return resolved.toString()
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error following redirects or checking content type for $url", e)
        }
        return url
    }
}
