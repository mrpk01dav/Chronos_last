package com.chronos.tasktracker.data.dao

import androidx.room.*
import com.chronos.tasktracker.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isPinned DESC, dueDate ASC, dueTime ASC")
    fun getAllTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate = :date ORDER BY isPinned DESC, dueTime ASC")
    fun getTasksByDateFlow(date: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0")
    suspend fun getPendingTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: String)

    @Query("UPDATE tasks SET isCompleted = 1, completedAt = :now WHERE id = :taskId")
    suspend fun markTaskComplete(taskId: String, now: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1 AND dueDate = :date")
    suspend fun getCompletedCountForDate(date: String): Int
}
