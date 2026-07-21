package com.taskflow.data.repository

import com.taskflow.data.local.TaskDao
import com.taskflow.data.model.Task
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    val allActiveTasks: Flow<List<Task>> = taskDao.getAllActiveTasks()
    val allCompletedTasks: Flow<List<Task>> = taskDao.getAllCompletedTasks()
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val activeTaskCount: Flow<Int> = taskDao.getActiveTaskCount()

    suspend fun getTaskById(id: Long): Task? = taskDao.getTaskById(id)

    fun searchActiveTasks(query: String): Flow<List<Task>> = taskDao.searchActiveTasks(query)

    fun getTasksByCategory(category: String): Flow<List<Task>> = taskDao.getTasksByCategory(category)

    suspend fun getUpcomingReminders(currentTime: Long): List<Task> =
        taskDao.getUpcomingReminders(currentTime)

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    suspend fun deleteAllCompleted() = taskDao.deleteAllCompleted()
}
