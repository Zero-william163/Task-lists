package com.taskflow.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.taskflow.data.local.TaskDatabase
import com.taskflow.data.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskFlowWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TaskFlowWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateWidgetData(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            updateWidgetData(context)
        }
    }

    private fun updateWidgetData(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = TaskDatabase.getDatabase(context)
                val repository = TaskRepository(database.taskDao())
                val tasks = repository.getUpcomingReminders(System.currentTimeMillis())
                val count = repository.activeTaskCount

                val manager = GlanceAppWidgetManager(context)
                val widget = TaskFlowWidget()
                val ids = manager.getGlanceIds(TaskFlowWidget::class.java)
                ids.forEach { id ->
                    widget.update(context, id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
