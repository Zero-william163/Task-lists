package com.taskflow.update

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.getSystemService

class UpdateService(private val context: Context) {

    fun startDownload(url: String): Long {
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle("TaskFlow 更新下载")
            setDescription("正在下载最新版本...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "taskflow_update.apk")
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val downloadManager = context.getSystemService<DownloadManager>()
        return downloadManager?.enqueue(request) ?: -1
    }
}
