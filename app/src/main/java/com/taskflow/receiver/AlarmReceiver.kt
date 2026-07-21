package com.taskflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.taskflow.util.NotificationHelper

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("taskId", -1)
        val title = intent.getStringExtra("title") ?: "任务提醒"
        val description = intent.getStringExtra("description") ?: ""

        if (taskId != -1L) {
            NotificationHelper.showReminderNotification(
                context,
                taskId,
                title,
                description
            )
        }
    }
}
