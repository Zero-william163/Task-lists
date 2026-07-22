package com.taskflow.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.taskflow.R
import com.taskflow.data.local.TaskDatabase
import com.taskflow.data.model.Task
import com.taskflow.data.repository.TaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class TaskWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TaskWidgetFactory(applicationContext)
    }
}

class TaskWidgetFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var tasks: List<Task> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val database = TaskDatabase.getDatabase(context)
        val repository = TaskRepository(database.taskDao())
        tasks = runBlocking {
            repository.allActiveTasks.first()
        }
    }

    override fun onDestroy() {
        tasks = emptyList()
    }

    override fun getCount(): Int = tasks.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= tasks.size) return RemoteViews(context.packageName, R.layout.widget_task_item)

        val task = tasks[position]
        val views = RemoteViews(context.packageName, R.layout.widget_task_item)

        views.setTextViewText(R.id.task_title, task.title)

        val priorityColor = when (task.priority) {
            Task.PRIORITY_HIGH -> "#FFEF4444"
            Task.PRIORITY_MEDIUM -> "#FFF59E0B"
            else -> "#FF10B981"
        }
        views.setInt(R.id.task_priority_indicator, "setBackgroundColor", android.graphics.Color.parseColor(priorityColor))

        val categoryText = buildString {
            append(task.category)
            task.dueDate?.let { due ->
                val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                append(" · ").append(sdf.format(Date(due)))
            }
        }
        views.setTextViewText(R.id.task_category, categoryText)

        task.dueDate?.let { due ->
            val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
            views.setTextViewText(R.id.task_due_date, sdf.format(Date(due)))
        } ?: run {
            views.setViewVisibility(R.id.task_due_date, android.view.View.GONE)
        }

        // 点击跳转到 App
        val fillInIntent = Intent().apply {
            putExtra("task_id", task.id)
        }
        views.setOnClickFillInIntent(R.id.task_title, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
