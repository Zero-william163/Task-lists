package com.taskflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.taskflow.widget.TaskFlowWidgetReceiver

class WidgetUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TaskFlowWidgetReceiver.updateAllWidgets(context)
    }
}
