package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MemoApplication
import com.example.ai.AIIntentResult
import com.example.ai.ExtractedTask
import com.example.ai.IntentType
import com.example.ai.NoteSummaryResult
import com.example.ai.ParsedTaskData
import com.example.ai.RuleBasedNLPFallback
import com.example.ai.SpeechRecognizerHelper
import com.example.ai.VoiceState
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskPriority
import com.example.data.local.entity.TaskStatus
import com.example.data.repository.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionSuggested: AIIntentResult? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MemoApplication
    private val taskRepository = app.taskRepository
    private val noteRepository = app.noteRepository
    val settingsRepository = app.settingsRepository
    val reminderManager = app.reminderManager
    val aiProvider = app.aiProvider

    val speechRecognizerHelper = SpeechRecognizerHelper(application)
    val voiceState: StateFlow<VoiceState> = speechRecognizerHelper.voiceState
    val partialVoiceText: StateFlow<String> = speechRecognizerHelper.partialText

    val allTasks: StateFlow<List<TaskEntity>> = taskRepository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeNotes: StateFlow<List<NoteEntity>> = noteRepository.activeNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedNotes: StateFlow<List<NoteEntity>> = noteRepository.archivedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = settingsRepository.settings

    // Today Tasks
    private val todayString: String
        get() = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    val todayTasks: StateFlow<List<TaskEntity>> = allTasks.combine(MutableStateFlow(Unit)) { tasks, _ ->
        tasks.filter { it.dueDate == todayString }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingTasks: StateFlow<List<TaskEntity>> = allTasks.combine(MutableStateFlow(Unit)) { tasks, _ ->
        tasks.filter { it.dueDate > todayString && it.status != TaskStatus.COMPLETED }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Intent Dialog State
    private val _aiIntentResult = MutableStateFlow<AIIntentResult?>(null)
    val aiIntentResult: StateFlow<AIIntentResult?> = _aiIntentResult.asStateFlow()

    private val _isAiProcessing = MutableStateFlow(false)
    val isAiProcessing: StateFlow<Boolean> = _isAiProcessing.asStateFlow()

    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

    // Assistant Chat Messages
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "ai",
                text = "Xin chào Khương! Tôi là trợ lý MemoAI. Tôi có thể giúp bạn kiểm tra lịch làm việc hôm nay, tìm ghi chú, hoặc tạo công việc mới bằng giọng nói."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAssistantThinking = MutableStateFlow(false)
    val isAssistantThinking: StateFlow<Boolean> = _isAssistantThinking.asStateFlow()

    fun clearFeedback() {
        _userFeedbackMessage.value = null
    }

    fun dismissAiPreview() {
        _aiIntentResult.value = null
        speechRecognizerHelper.reset()
    }

    // Voice recognition trigger
    fun startVoiceInput() {
        speechRecognizerHelper.startListening()
    }

    fun stopVoiceInput() {
        speechRecognizerHelper.stopListening()
    }

    fun processSpokenText(text: String, onGeneralQueryOrChat: (() -> Unit)? = null) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _isAiProcessing.value = true
            val nowContext = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val lower = text.lowercase().trim()
            val hasNoteKeyword = lower.contains("ghi chú") || lower.contains("note") ||
                    lower.contains("lưu ý") || lower.contains("nhớ giúp tôi") || lower.contains("ghi lại")
            val hasAlarmKeyword = lower.contains("nhắc tôi") || lower.contains("báo thức") || lower.contains("hẹn giờ") ||
                    lower.contains("báo tôi") || lower.contains("hẹn lúc")

            val fastLocal = RuleBasedNLPFallback.parseIntent(text, nowContext)

            // Fast path: if local parser detects a note or high-confidence task, respond in < 1ms!
            val result = if (hasNoteKeyword && !hasAlarmKeyword) {
                fastLocal
            } else if (fastLocal.confidence >= 0.90f) {
                fastLocal
            } else {
                try {
                    aiProvider.parseVoiceIntent(text, nowContext)
                } catch (e: Exception) {
                    fastLocal
                }
            }
            _isAiProcessing.value = false

            when (result.intent) {
                IntentType.CREATE_TASK -> {
                    if (settings.value.autoCreateWithoutConfirmation && result.confidence >= 0.85f && result.taskData != null) {
                        executeIntentAction(result)
                    } else {
                        _aiIntentResult.value = result
                    }
                }
                IntentType.CREATE_NOTE -> {
                    if (settings.value.autoCreateWithoutConfirmation && result.confidence >= 0.85f && result.noteData != null) {
                        executeIntentAction(result)
                    } else {
                        _aiIntentResult.value = result
                    }
                }
                IntentType.COMPLETE_TASK -> {
                    executeIntentAction(result)
                }
                IntentType.SEARCH_TASK, IntentType.SEARCH_NOTE, IntentType.GENERAL_QUERY -> {
                    // Send to AI Assistant Chat and switch to Assistant screen so the user sees the answer!
                    sendChatMessage(text)
                    onGeneralQueryOrChat?.invoke()
                }
                else -> {
                    sendChatMessage(text)
                    onGeneralQueryOrChat?.invoke()
                }
            }
        }
    }

    fun executeIntentAction(result: AIIntentResult) {
        viewModelScope.launch {
            when (result.intent) {
                IntentType.CREATE_TASK -> {
                    result.taskData?.let { data ->
                        val task = TaskEntity(
                            title = data.title,
                            description = data.description,
                            dueDate = data.date,
                            dueTime = data.time,
                            reminderMinutesBefore = data.reminderMinutesBefore,
                            priority = data.priority,
                            category = data.category,
                            repeatRule = data.repeatRule
                        )
                        val id = taskRepository.insertTask(task)
                        reminderManager.scheduleTaskReminder(task.copy(id = id))
                        _userFeedbackMessage.value = "Đã lên lịch công việc: \"${task.title}\""
                    }
                }
                IntentType.CREATE_NOTE -> {
                    result.noteData?.let { data ->
                        val note = NoteEntity(
                            title = data.title,
                            content = data.content,
                            category = data.category,
                            tags = data.tags.joinToString(",")
                        )
                        noteRepository.insertNote(note)
                        _userFeedbackMessage.value = "Đã lưu ghi chú: \"${note.title}\""
                    }
                }
                IntentType.COMPLETE_TASK -> {
                    // Find matching task by raw prompt
                    val query = result.rawPrompt.lowercase()
                    val match = allTasks.value.firstOrNull { t ->
                        t.status != TaskStatus.COMPLETED && query.contains(t.title.lowercase())
                    }
                    if (match != null) {
                        taskRepository.toggleTaskComplete(match.id, TaskStatus.TODO)
                        _userFeedbackMessage.value = "Đã đánh dấu hoàn thành: \"${match.title}\""
                    } else {
                        _userFeedbackMessage.value = "Không tìm thấy công việc phù hợp để hoàn thành."
                    }
                }
                else -> {
                    _userFeedbackMessage.value = result.explanation
                }
            }
            dismissAiPreview()
        }
    }

    fun saveTask(task: TaskEntity) {
        viewModelScope.launch {
            if (task.id == 0L) {
                val newId = taskRepository.insertTask(task)
                val savedTask = task.copy(id = newId)
                reminderManager.scheduleTaskReminder(savedTask)
                _userFeedbackMessage.value = "Đã tạo công việc: ${task.title}"
            } else {
                taskRepository.updateTask(task)
                if (task.status == TaskStatus.COMPLETED) {
                    reminderManager.cancelTaskReminder(task.id)
                } else {
                    reminderManager.scheduleTaskReminder(task)
                }
                _userFeedbackMessage.value = "Đã cập nhật công việc"
            }
        }
    }

    fun toggleTaskComplete(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.toggleTaskComplete(task.id, task.status)
            if (task.status != TaskStatus.COMPLETED) {
                reminderManager.cancelTaskReminder(task.id)
            } else {
                reminderManager.scheduleTaskReminder(task.copy(status = TaskStatus.TODO))
            }
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            reminderManager.cancelTaskReminder(taskId)
            taskRepository.deleteTask(taskId)
            _userFeedbackMessage.value = "Đã xóa công việc."
        }
    }

    fun saveNote(note: NoteEntity) {
        viewModelScope.launch {
            if (note.id == 0L) {
                noteRepository.insertNote(note)
                _userFeedbackMessage.value = "Đã tạo ghi chú mới."
            } else {
                noteRepository.updateNote(note)
                _userFeedbackMessage.value = "Đã lưu ghi chú."
            }
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            noteRepository.deleteNote(noteId)
            _userFeedbackMessage.value = "Đã xóa ghi chú."
        }
    }

    fun toggleNotePin(noteId: Long) {
        viewModelScope.launch {
            noteRepository.togglePin(noteId)
        }
    }

    fun toggleNoteArchive(noteId: Long) {
        viewModelScope.launch {
            noteRepository.toggleArchive(noteId)
        }
    }

    // AI Assistant Chat
    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return
        val userMsg = ChatMessage(sender = "user", text = userText)
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isAssistantThinking.value = true
            val nowContext = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

            // 1. Check if user is asking a question / analysis query
            val lower = userText.lowercase().trim()
            val isQueryOrAnalysis = lower.contains("xếp loại") ||
                    lower.contains("sắp xếp") ||
                    lower.contains("phân loại") ||
                    lower.contains("liệt kê") ||
                    lower.contains("danh sách") ||
                    lower.contains("tổng kết") ||
                    lower.contains("tóm tắt") ||
                    lower.contains("phân tích") ||
                    lower.contains("đánh giá") ||
                    lower.contains("việc nào") ||
                    lower.contains("hôm nay làm gì") ||
                    lower.contains("hôm nay tôi phải làm gì") ||
                    lower.contains("có những việc gì") ||
                    lower.contains("cho tôi xem") ||
                    lower.contains("gợi ý") ||
                    lower.contains("tư vấn") ||
                    lower.contains("làm sao") ||
                    lower.contains("như thế nào") ||
                    lower.contains("thế nào") ||
                    lower.endsWith("?")

            val hasNoteKeyword = lower.contains("ghi chú") || lower.contains("note") ||
                    lower.contains("lưu ý") || lower.contains("nhớ giúp tôi") || lower.contains("ghi lại")
            val hasAlarmKeyword = lower.contains("nhắc tôi") || lower.contains("báo thức") || lower.contains("hẹn giờ") ||
                    lower.contains("báo tôi") || lower.contains("hẹn lúc")

            val fastLocal = RuleBasedNLPFallback.parseIntent(userText, nowContext)

            var intentResult: AIIntentResult? = null
            if (!isQueryOrAnalysis) {
                if (hasNoteKeyword && !hasAlarmKeyword) {
                    intentResult = fastLocal
                } else if (fastLocal.confidence >= 0.90f && fastLocal.intent == IntentType.CREATE_TASK) {
                    intentResult = fastLocal
                }
            }

            if (intentResult != null && intentResult.intent == IntentType.CREATE_TASK && intentResult.taskData != null) {
                val data = intentResult.taskData
                val task = TaskEntity(
                    title = data.title,
                    description = data.description,
                    dueDate = data.date,
                    dueTime = data.time,
                    reminderMinutesBefore = data.reminderMinutesBefore,
                    priority = data.priority,
                    category = data.category,
                    repeatRule = data.repeatRule
                )
                val id = taskRepository.insertTask(task)
                reminderManager.scheduleTaskReminder(task.copy(id = id))
                _userFeedbackMessage.value = "Đã lên lịch công việc: \"${task.title}\""

                val friendlyDate = if (task.dueDate == todayStr) "hôm nay (${task.dueDate})" else "ngày ${task.dueDate}"
                val timeStr = if (!task.dueTime.isNullOrBlank()) " vào lúc **${task.dueTime}**" else ""
                val prioBadge = when (task.priority) {
                    TaskPriority.URGENT -> "🔴 Khẩn cấp"
                    TaskPriority.HIGH -> "🟠 Cao"
                    TaskPriority.MEDIUM -> "🟡 Trung bình"
                    TaskPriority.LOW -> "🟢 Thấp"
                }

                val confirmationMsg = buildString {
                    appendLine("Dạ vâng! Tôi đã ghi nhận và thêm công việc mới này vào lịch trình của bạn rồi nhé:")
                    appendLine("")
                    appendLine("📌 **Nhiệm vụ:** ${task.title}")
                    appendLine("* ⏰ **Thời gian:** $friendlyDate$timeStr")
                    appendLine("* 🏷️ **Nhóm:** ${task.category}")
                    appendLine("* ⚡ **Mức ưu tiên:** $prioBadge")
                    if (task.description.isNotBlank()) {
                        appendLine("* 📝 **Ghi chú thêm:** ${task.description}")
                    }
                    appendLine("")
                    appendLine("Nhiệm vụ đã được lưu thành công vào cơ sở dữ liệu. Bạn có thể xem ngay trong tab **Công việc** hoặc **Lịch trình** nhé! ✨")
                }

                _isAssistantThinking.value = false
                _chatMessages.value = _chatMessages.value + ChatMessage(sender = "ai", text = confirmationMsg)
                return@launch
            } else if (intentResult != null && intentResult.intent == IntentType.CREATE_NOTE && intentResult.noteData != null) {
                val data = intentResult.noteData
                val note = NoteEntity(
                    title = data.title,
                    content = data.content,
                    category = data.category,
                    tags = data.tags.joinToString(",")
                )
                val id = noteRepository.insertNote(note)
                _userFeedbackMessage.value = "Đã tạo ghi chú: \"${note.title}\""

                val confirmationMsg = buildString {
                    appendLine("Dạ vâng! Tôi đã lưu ghi chú mới này vào sổ tay của bạn rồi nhé:")
                    appendLine("")
                    appendLine("📝 **Tiêu đề:** ${note.title}")
                    appendLine("* 📄 **Nội dung:** ${note.content}")
                    appendLine("* 🏷️ **Danh mục:** ${note.category}")
                    appendLine("")
                    appendLine("Ghi chú đã sẵn sàng trong tab **Ghi chú** để bạn tra cứu bất cứ lúc nào! 💡")
                }

                _isAssistantThinking.value = false
                _chatMessages.value = _chatMessages.value + ChatMessage(sender = "ai", text = confirmationMsg)
                return@launch
            }

            // General query / conversational response
            val history = _chatMessages.value.dropLast(1).map { it.sender to it.text }
            val answer = aiProvider.chatWithAssistant(
                userMessage = userText,
                currentTasks = allTasks.value,
                currentNotes = activeNotes.value,
                conversationHistory = history
            )
            _isAssistantThinking.value = false
            _chatMessages.value = _chatMessages.value + ChatMessage(sender = "ai", text = answer)
        }
    }

    // AI Summarize Note
    fun summarizeNote(note: NoteEntity, onResult: (NoteSummaryResult) -> Unit) {
        viewModelScope.launch {
            _isAiProcessing.value = true
            val result = aiProvider.summarizeNote(note.title, note.content)
            _isAiProcessing.value = false
            onResult(result)
        }
    }

    // AI Extract Tasks From Note
    fun extractTasksFromNote(note: NoteEntity, onResult: (List<ExtractedTask>) -> Unit) {
        viewModelScope.launch {
            _isAiProcessing.value = true
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val result = aiProvider.extractTasksFromNote(note.title, note.content, today)
            _isAiProcessing.value = false
            onResult(result)
        }
    }

    fun createBatchTasksFromNote(tasks: List<ExtractedTask>, noteId: Long) {
        viewModelScope.launch {
            var count = 0
            tasks.filter { it.isSelected }.forEach { item ->
                val entity = TaskEntity(
                    title = item.title,
                    dueDate = item.dueDate,
                    dueTime = item.dueTime,
                    priority = item.priority,
                    category = "Công việc",
                    linkedNoteId = noteId
                )
                val id = taskRepository.insertTask(entity)
                reminderManager.scheduleTaskReminder(entity.copy(id = id))
                count++
            }
            _userFeedbackMessage.value = "Đã tạo $count công việc từ ghi chú!"
        }
    }

    // Export & Clear Data
    suspend fun exportDataJson(): String {
        val tasks = taskRepository.getAllTasksSnapshot()
        val notes = noteRepository.getAllNotesSnapshot()
        val root = org.json.JSONObject()
        val taskArray = org.json.JSONArray()
        tasks.forEach { t ->
            val obj = org.json.JSONObject()
            obj.put("title", t.title)
            obj.put("dueDate", t.dueDate)
            obj.put("dueTime", t.dueTime)
            obj.put("priority", t.priority.name)
            obj.put("status", t.status.name)
            obj.put("category", t.category)
            taskArray.put(obj)
        }
        val noteArray = org.json.JSONArray()
        notes.forEach { n ->
            val obj = org.json.JSONObject()
            obj.put("title", n.title)
            obj.put("content", n.content)
            obj.put("category", n.category)
            noteArray.put(obj)
        }
        root.put("tasks", taskArray)
        root.put("notes", noteArray)
        return root.toString(2)
    }

    fun testNotification() {
        reminderManager.showTestNotification()
        _userFeedbackMessage.value = "Đã phát chuông và gửi thông báo thử nghiệm! 🔔"
    }

    fun updateNotificationSound(uri: String, name: String) {
        settingsRepository.updateNotificationSound(uri, name)
        reminderManager.updateChannelSound()
        _userFeedbackMessage.value = "Đã cập nhật âm thanh: $name 🎵"
    }

    fun clearAllData() {
        viewModelScope.launch {
            allTasks.value.forEach { reminderManager.cancelTaskReminder(it.id) }
            taskRepository.clearAll()
            noteRepository.clearAll()
            _userFeedbackMessage.value = "Đã xóa toàn bộ dữ liệu."
        }
    }
}
