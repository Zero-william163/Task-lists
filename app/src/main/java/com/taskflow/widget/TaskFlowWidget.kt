package com.taskflow.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.taskflow.data.local.TaskDatabase
import com.taskflow.data.repository.TaskRepository
import kotlinx.coroutines.flow.first

class TaskFlowWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = TaskDatabase.getDatabase(context)
        val repository = TaskRepository(database.taskDao())
        val tasks = repository.getUpcomingReminders(System.currentTimeMillis())
        val count = repository.activeTaskCount.first()

        provideContent {
            WidgetContent(taskCount = count, tasks = tasks)
        }
    }
}

@Composable
fun WidgetContent(taskCount: Int, tasks: List<com.taskflow.data.model.Task>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(android.graphics.Color.parseColor("#FF6750A4")))
            .cornerRadius(16.dp)
            .padding(16.dp)
    ) {
        Text(
            text = "TaskFlow",
            style = TextStyle(
                color = ColorProvider(android.graphics.Color.WHITE),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = "$taskCount 个未完成任务",
            style = TextStyle(
                color = ColorProvider(android.graphics.Color.WHITE),
                fontSize = 14.sp
            )
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        if (tasks.isEmpty()) {
            Text(
                text = "暂无任务",
                style = TextStyle(
                    color = ColorProvider(android.graphics.Color.parseColor("#E0E0E0")),
                    fontSize = 12.sp
                )
            )
        } else {
            tasks.take(3).forEach { task ->
                Text(
                    text = "• ${task.title}",
                    style = TextStyle(
                        color = ColorProvider(android.graphics.Color.WHITE),
                        fontSize = 12.sp
                    )
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
            }
        }
    }
}
