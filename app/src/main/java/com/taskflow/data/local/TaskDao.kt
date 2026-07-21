package com.taskflow.data.local

import androidx.room.*
import com.taskflow.data.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY priority DESC, dueDate ASC, createdAt DESC")
    fun getAllActiveTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun getAllCompletedTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): Task?

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND title LIKE '%' || :query || '%' ORDER BY priority DESC, dueDate ASC")
    fun searchActiveTasks(query: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND category = :category ORDER BY priority DESC, dueDate ASC")
    fun getTasksByCategory(category: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND reminderTime IS NOT NULL AND reminderTime > :currentTime ORDER BY reminderTime ASC")
    suspend fun getUpcomingReminders(currentTime: Long): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    suspend fun deleteAllCompleted()

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0")
    fun getActiveTaskCount(): Flow<Int>
}
