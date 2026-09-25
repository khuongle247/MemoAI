package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.dao.NoteDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()

    fun getTasksForDate(date: String): Flow<List<TaskEntity>> = taskDao.getTasksForDate(date)

    suspend fun getTaskById(id: Long): TaskEntity? = taskDao.getTaskById(id)

    fun observeTaskById(id: Long): Flow<TaskEntity?> = taskDao.observeTaskById(id)

    suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteTask(id: Long) = taskDao.deleteTaskById(id)

    suspend fun toggleTaskComplete(id: Long, currentStatus: TaskStatus) {
        val newStatus = if (currentStatus == TaskStatus.COMPLETED) TaskStatus.TODO else TaskStatus.COMPLETED
        val completedAt = if (newStatus == TaskStatus.COMPLETED) System.currentTimeMillis() else null
        taskDao.updateTaskStatus(id, newStatus, completedAt)
    }

    suspend fun getAllTasksSnapshot(): List<TaskEntity> = taskDao.getAllTasksSnapshot()

    suspend fun clearAll() = taskDao.deleteAllTasks()
}

class NoteRepository(private val noteDao: NoteDao) {
    val activeNotes: Flow<List<NoteEntity>> = noteDao.getActiveNotes()
    val archivedNotes: Flow<List<NoteEntity>> = noteDao.getArchivedNotes()

    suspend fun getNoteById(id: Long): NoteEntity? = noteDao.getNoteById(id)

    fun observeNoteById(id: Long): Flow<NoteEntity?> = noteDao.observeNoteById(id)

    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)

    suspend fun insertNote(note: NoteEntity): Long = noteDao.insertNote(note)

    suspend fun updateNote(note: NoteEntity) = noteDao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteNote(id: Long) = noteDao.deleteNoteById(id)

    suspend fun togglePin(id: Long) {
        val note = noteDao.getNoteById(id) ?: return
        noteDao.updateNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleArchive(id: Long) {
        val note = noteDao.getNoteById(id) ?: return
        noteDao.updateNote(note.copy(isArchived = !note.isArchived, updatedAt = System.currentTimeMillis()))
    }

    suspend fun getAllNotesSnapshot(): List<NoteEntity> = noteDao.getAllNotesSnapshot()

    suspend fun clearAll() = noteDao.deleteAllNotes()
}

data class AppSettings(
    val darkModeOption: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val autoCreateWithoutConfirmation: Boolean = false,
    val defaultReminderMinutes: Int = 15,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val aiProvider: String = "Gemini",
    val userName: String = "Khương"
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("memo_ai_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        return AppSettings(
            darkModeOption = prefs.getString("dark_mode", "SYSTEM") ?: "SYSTEM",
            autoCreateWithoutConfirmation = prefs.getBoolean("auto_create", false),
            defaultReminderMinutes = prefs.getInt("default_reminder", 15),
            soundEnabled = prefs.getBoolean("sound_enabled", true),
            vibrationEnabled = prefs.getBoolean("vibration_enabled", true),
            aiProvider = prefs.getString("ai_provider", "Gemini") ?: "Gemini",
            userName = prefs.getString("user_name", "Khương") ?: "Khương"
        )
    }

    fun updateDarkMode(mode: String) {
        prefs.edit().putString("dark_mode", mode).apply()
        _settings.value = _settings.value.copy(darkModeOption = mode)
    }

    fun updateAutoCreate(enabled: Boolean) {
        prefs.edit().putBoolean("auto_create", enabled).apply()
        _settings.value = _settings.value.copy(autoCreateWithoutConfirmation = enabled)
    }

    fun updateDefaultReminder(minutes: Int) {
        prefs.edit().putInt("default_reminder", minutes).apply()
        _settings.value = _settings.value.copy(defaultReminderMinutes = minutes)
    }

    fun updateSound(enabled: Boolean) {
        prefs.edit().putBoolean("sound_enabled", enabled).apply()
        _settings.value = _settings.value.copy(soundEnabled = enabled)
    }

    fun updateVibration(enabled: Boolean) {
        prefs.edit().putBoolean("vibration_enabled", enabled).apply()
        _settings.value = _settings.value.copy(vibrationEnabled = enabled)
    }

    fun updateUserName(name: String) {
        prefs.edit().putString("user_name", name).apply()
        _settings.value = _settings.value.copy(userName = name)
    }
}
