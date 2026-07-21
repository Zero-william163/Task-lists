package com.taskflow

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.taskflow.data.local.TaskDatabase
import com.taskflow.data.repository.TaskRepository
import com.taskflow.worker.UpdateCheckWorker
import com.taskflow.util.NotificationHelper
import java.util.concurrent.TimeUnit

class TaskFlowApplication : Application() {

    lateinit var repository: TaskRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        val database = TaskDatabase.getDatabase(this)
        repository = TaskRepository(database.taskDao())

        NotificationHelper.createNotificationChannels(this)
        scheduleUpdateCheck()
    }

    private fun scheduleUpdateCheck() {
        val workRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "update_check",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    companion object {
        lateinit var instance: TaskFlowApplication
            private set
    }
}
