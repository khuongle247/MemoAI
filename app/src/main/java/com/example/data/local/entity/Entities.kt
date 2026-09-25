package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaskStatus {
    TODO,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDate: String, // Format: YYYY-MM-DD
    val dueTime: String? = null, // Format: HH:mm (24h)
    val reminderMinutesBefore: Int = 0, // e.g. 0 = at time, 15, 30, 60, 1440 (1 day)
    val repeatRule: String = "NONE", // NONE, DAILY, WEEKDAYS, WEEKLY, MONTHLY
    val category: String = "Công việc", // Công việc, Học tập, Cá nhân, Dự án, Tài chính, Ý tưởng, Khác
    val linkedNoteId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "Cá nhân",
    val tags: String = "", // Comma-separated tags, e.g. "meeting,project"
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val color: Long = 0xFFFFFFFF, // ARGB color
    val tableData: String = "", // JSON string for table rows & columns
    val drawingData: String = "", // JSON string for vector drawing strokes
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
