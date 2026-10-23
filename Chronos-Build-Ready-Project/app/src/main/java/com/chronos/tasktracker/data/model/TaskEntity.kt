package com.chronos.tasktracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String = "",
    val categoryId: String = "assignments", // college, assignments, personal, health, projects, other
    val priority: String = "medium", // high, medium, low
    val dueDate: String, // YYYY-MM-DD
    val dueTime: String = "12:00", // HH:mm
    val estimatedDurationMinutes: Int = 30,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val isPinned: Boolean = false,
    val subtasksJson: String = "[]",
    val recurrenceType: String = "none", // none, daily, weekdays, weekly, monthly, yearly, custom
    val recurrenceInterval: Int = 1,
    val recurrenceDaysOfWeek: String = "", // e.g. "1,3,5" for Mon, Wed, Fri
    val recurrenceEndDate: String? = null,
    val repeatLimit: Int? = null,
    val occurrenceCount: Int = 1,
    val reminderMinutesBefore: Int = 15,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
