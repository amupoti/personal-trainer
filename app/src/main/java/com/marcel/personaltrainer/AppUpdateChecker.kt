package com.marcel.personaltrainer

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class AppRelease(
    val versionName: String,
    val apkUrl: String,
)

object AppUpdateChecker {
    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/amupoti/personal-trainer/releases/latest"

    suspend fun fetchLatestRelease(): AppRelease = withContext(Dispatchers.IO) {
        val connection = URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "personal-trainer-android")
            check(connection.responseCode == HttpURLConnection.HTTP_OK) {
                "GitHub returned HTTP ${connection.responseCode}"
            }

            val release = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val assets = release.getJSONArray("assets")
            val apkUrl = (0 until assets.length())
                .asSequence()
                .map(assets::getJSONObject)
                .firstOrNull { it.getString("name").endsWith(".apk", ignoreCase = true) }
                ?.getString("browser_download_url")
                ?: error("The latest release does not contain an APK")

            AppRelease(
                versionName = release.getString("tag_name").removePrefix("v"),
                apkUrl = apkUrl,
            )
        } finally {
            connection.disconnect()
        }
    }
}

fun isNewerVersion(latest: String, current: String): Boolean {
    val latestParts = latest.toVersionParts() ?: return false
    val currentParts = current.toVersionParts() ?: return false
    return (0 until maxOf(latestParts.size, currentParts.size)).any { index ->
        val latestPart = latestParts.getOrElse(index) { 0 }
        val currentPart = currentParts.getOrElse(index) { 0 }
        if (latestPart != currentPart) {
            return latestPart > currentPart
        }
        false
    }
}

private fun String.toVersionParts(): List<Int>? {
    val normalized = removePrefix("v")
    if (!normalized.matches(Regex("""\d+(\.\d+)*"""))) {
        return null
    }
    return normalized.split(".").map(String::toInt)
}
