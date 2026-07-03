package com.marcel.personaltrainer

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class ApkUpdateInstaller(private val context: Context) {
    private val downloadManager = context.getSystemService(DownloadManager::class.java)

    fun canInstallPackages(): Boolean = context.packageManager.canRequestPackageInstalls()

    fun installPermissionIntent(): Intent = Intent(
        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        Uri.parse("package:${context.packageName}"),
    )

    suspend fun download(release: AppRelease): Uri {
        val request = DownloadManager.Request(Uri.parse(release.apkUrl))
            .setTitle(context.getString(R.string.update_download_title, release.versionName))
            .setDescription(context.getString(R.string.update_download_description))
            .setMimeType(APK_MIME_TYPE)
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED,
            )
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "daily-movement-${release.versionName}-${System.currentTimeMillis()}.apk",
            )
        val downloadId = downloadManager.enqueue(request)
        return awaitDownload(downloadId)
    }

    fun install(apkUri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(apkUri, APK_MIME_TYPE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }

    private suspend fun awaitDownload(downloadId: Long): Uri = withContext(Dispatchers.IO) {
        while (true) {
            downloadManager.query(
                DownloadManager.Query().setFilterById(downloadId),
            ).use { cursor ->
                check(cursor.moveToFirst()) { "The APK download was not found" }
                when (
                    cursor.getInt(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS),
                    )
                ) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        return@withContext downloadManager.getUriForDownloadedFile(downloadId)
                            ?: error("The downloaded APK could not be opened")
                    }

                    DownloadManager.STATUS_FAILED -> {
                        val reason = cursor.getInt(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON),
                        )
                        error("The APK download failed with reason $reason")
                    }
                }
            }
            delay(DOWNLOAD_POLL_INTERVAL_MS)
        }
        error("Unreachable")
    }

    private companion object {
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        const val DOWNLOAD_POLL_INTERVAL_MS = 500L
    }
}
