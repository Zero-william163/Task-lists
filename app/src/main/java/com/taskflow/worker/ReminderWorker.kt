package com.taskflow.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.*
import com.taskflow.data.model.Task
import com.taskflow.receiver.AlarmReceiver
import java.util.concurrent.TimeUnit

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val taskId = inputData.getLong("taskId", -1)
        val title = inputData.getString("title") ?: ""
        val description = inputData.getString("description") ?: ""

        if (taskId != -1L) {
            com.taskflow.util.NotificationHelper.showReminderNotification(
                applicationContext,
                taskId,
                title,
                description
            )
        }
        return Result.success()
    }

    companion object {
        fun scheduleReminder(context: Context, task: Task) {
            val reminderTime = task.reminderTime ?: return
            val currentTime = System.currentTimeMillis()
            if (reminderTime <= currentTime) return

            val delay = reminderTime - currentTime

            val inputData = workDataOf(
                "taskId" to task.id,
                "title" to task.title,
                "description" to task.description
            )

            val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag("reminder_${task.id}")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "reminder_${task.id}",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }

        fun cancelReminder(context: Context, taskId: Long) {
            WorkManager.getInstance(context).cancelUniqueWork("reminder_$taskId")
        }
    }
}
