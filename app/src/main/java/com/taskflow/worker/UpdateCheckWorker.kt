package com.taskflow.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.taskflow.update.UpdateManager

class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            UpdateManager.checkForUpdate(applicationContext, showNotification = true)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
