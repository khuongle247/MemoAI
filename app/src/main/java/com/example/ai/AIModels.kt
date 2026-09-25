package com.example.ai

import com.example.data.local.entity.TaskPriority

enum class IntentType {
    CREATE_TASK,
    CREATE_NOTE,
    UPDATE_TASK,
    COMPLETE_TASK,
    DELETE_TASK,
    SEARCH_NOTE,
    SEARCH_TASK,
    SUMMARIZE_NOTE,
    EXTRACT_TASKS,
    GENERAL_QUERY
}

data class ParsedTaskData(
    val title: String,
    val description: String = "",
    val date: String, // YYYY-MM-DD
    val time: String? = null, // HH:mm
    val reminderMinutesBefore: Int = 0,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val category: String = "Công việc",
    val repeatRule: String = "NONE"
)

data class ParsedNoteData(
    val title: String,
    val content: String,
    val category: String = "Cá nhân",
    val tags: List<String> = emptyList()
)

data class AIIntentResult(
    val intent: IntentType,
    val confidence: Float = 0.95f,
    val explanation: String = "",
    val taskData: ParsedTaskData? = null,
    val noteData: ParsedNoteData? = null,
    val searchQuery: String? = null,
    val rawPrompt: String = ""
)

data class NoteSummaryResult(
    val summary: String,
    val keyPoints: List<String>,
    val actionItems: List<String>
)

data class ExtractedTask(
    val title: String,
    val dueDate: String,
    val dueTime: String? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    var isSelected: Boolean = true
)
