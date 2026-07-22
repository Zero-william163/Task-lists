package com.taskflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.taskflow.data.local.TaskDatabase
import com.taskflow.data.repository.TaskRepository
import com.taskflow.widget.TaskFlowWidgetReceiver
import com.taskflow.worker.ReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val database = TaskDatabase.getDatabase(context)
                val repository = TaskRepository(database.taskDao())
                val currentTime = System.currentTimeMillis()
                val tasks = repository.getUpcomingReminders(currentTime)

                tasks.forEach { task ->
                    task.reminderTime?.let { time ->
                        if (time > currentTime) {
                            ReminderWorker.scheduleReminder(context, task)
                        }
                    }
                }

                TaskFlowWidgetReceiver.updateAllWidgets(context)
            }
        }
    }
}
