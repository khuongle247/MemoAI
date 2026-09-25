package com.example.ai

import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.TaskEntity

interface AIProvider {
    suspend fun parseVoiceIntent(userPrompt: String, currentDateTimeContext: String): AIIntentResult
    suspend fun chatWithAssistant(
        userMessage: String,
        currentTasks: List<TaskEntity>,
        currentNotes: List<NoteEntity>,
        conversationHistory: List<Pair<String, String>>
    ): String
    suspend fun summarizeNote(noteTitle: String, noteContent: String): NoteSummaryResult
    suspend fun extractTasksFromNote(noteTitle: String, noteContent: String, currentDate: String): List<ExtractedTask>
}
