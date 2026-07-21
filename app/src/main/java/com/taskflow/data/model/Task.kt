package com.taskflow.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val reminderTime: Long? = null,
    val priority: Int = PRIORITY_MEDIUM,
    val category: String = CATEGORY_NONE,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
) {
    companion object {
        const val PRIORITY_HIGH = 3
        const val PRIORITY_MEDIUM = 2
        const val PRIORITY_LOW = 1

        const val CATEGORY_NONE = "未分类"
        const val CATEGORY_WORK = "工作"
        const val CATEGORY_PERSONAL = "个人"
        const val CATEGORY_STUDY = "学习"
        const val CATEGORY_HEALTH = "健康"
        const val CATEGORY_SHOPPING = "购物"

        val CATEGORIES = listOf(
            CATEGORY_NONE,
            CATEGORY_WORK,
            CATEGORY_PERSONAL,
            CATEGORY_STUDY,
            CATEGORY_HEALTH,
            CATEGORY_SHOPPING
        )
    }
}
