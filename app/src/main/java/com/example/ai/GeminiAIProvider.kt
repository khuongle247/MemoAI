package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskPriority
import com.example.data.local.entity.TaskStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class GeminiAIProvider : AIProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val model = "gemini-2.5-flash"

    private suspend fun callGemini(
        prompt: String,
        systemInstruction: String? = null,
        isJsonOutput: Boolean = false,
        maxTokens: Int = 1024
    ): String? = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }
        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d("GeminiAIProvider", "No valid Gemini API key found, fallback will be used.")
            return@withContext null
        }

        try {
            val rootJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", prompt))
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            rootJson.put("contents", contentsArray)

            // Fast generationConfig
            val genConfig = JSONObject()
            genConfig.put("maxOutputTokens", maxTokens)
            genConfig.put("temperature", if (isJsonOutput) 0.1 else 0.7)
            if (isJsonOutput) {
                genConfig.put("responseMimeType", "application/json")
            }
            rootJson.put("generationConfig", genConfig)

            if (!systemInstruction.isNullOrBlank()) {
                val sysContent = JSONObject()
                val sysParts = JSONArray()
                sysParts.put(JSONObject().put("text", systemInstruction))
                sysContent.put("parts", sysParts)
                rootJson.put("systemInstruction", sysContent)
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val body = rootJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).post(body).build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w("GeminiAIProvider", "Gemini HTTP error ${response.code}: ${response.message}")
                return@withContext null
            }

            val respBody = response.body?.string() ?: return@withContext null
            val respJson = JSONObject(respBody)
            val candidates = respJson.optJSONArray("candidates") ?: return@withContext null
            if (candidates.length() == 0) return@withContext null
            val firstCandidate = candidates.getJSONObject(0)
            val parts = firstCandidate.optJSONObject("content")?.optJSONArray("parts") ?: return@withContext null
            if (parts.length() == 0) return@withContext null
            parts.getJSONObject(0).optString("text", "")
        } catch (e: Exception) {
            Log.e("GeminiAIProvider", "Error calling Gemini", e)
            null
        }
    }

    private fun normalizeCategory(cat: String, defaultCat: String): String {
        return when (cat.trim().lowercase()) {
            "work", "công việc" -> "Công việc"
            "study", "học tập", "học" -> "Học tập"
            "personal", "cá nhân" -> "Cá nhân"
            "project", "dự án" -> "Dự án"
            "finance", "tài chính" -> "Tài chính"
            "ideas", "ý tưởng" -> "Ý tưởng"
            "other", "khác" -> "Khác"
            else -> if (cat.isNotBlank()) cat else defaultCat
        }
    }

    override suspend fun parseVoiceIntent(userPrompt: String, currentDateTimeContext: String): AIIntentResult {
        val systemInstruction = """
            You are MemoAI, a Vietnamese and English personal voice assistant for notes and task management.
            The current reference date and time is: $currentDateTimeContext.

            STRICT INTENT CLASSIFICATION RULES:
            1. If the user mentions "ghi chú", "tạo ghi chú", "tạo 1 ghi chú", "tạo một ghi chú", "thêm ghi chú", "viết ghi chú", "lưu ghi chú", "note", "tạo note", "nhớ giúp tôi", "ghi lại", "lưu lại" -> INTENT MUST BE "CREATE_NOTE" (NEVER "CREATE_TASK"!).
               Extract:
               - "note": { "title": "Concise clean title without 'tạo ghi chú'", "content": "Full note content", "category": "Cá nhân" | "Công việc" | "Học tập" | "Dự án" | "Tài chính" | "Ý tưởng" | "Khác", "tags": ["tag1"] }
            2. "CREATE_TASK" is ONLY for tasks with an actionable deadline, reminder, alarm, or to-do schedule (e.g. "nhắc tôi...", "hẹn lịch...", "8h sáng mai nộp báo cáo", "chiều mai họp").
            3. If user explicitly says "tạo ghi chú" or "tạo 1 ghi chú", it is 100% "CREATE_NOTE".

            Output STRICT JSON only (no markdown, no ```json formatting):
            {
              "intent": "CREATE_TASK" | "CREATE_NOTE" | "COMPLETE_TASK" | "SEARCH_TASK" | "SEARCH_NOTE" | "GENERAL_QUERY",
              "confidence": 0.98,
              "explanation": "Brief explanation in Vietnamese of what was understood",
              "task": {
                 "title": "Clean concise task title",
                 "description": "Any extra details or duration mentioned",
                 "date": "YYYY-MM-DD",
                 "time": "HH:mm" (24-hour format, or null if no time),
                 "reminderMinutesBefore": 0 | 15 | 30 | 60,
                 "priority": "LOW" | "MEDIUM" | "HIGH" | "URGENT",
                 "category": "Công việc" | "Học tập" | "Cá nhân" | "Dự án" | "Tài chính" | "Ý tưởng" | "Khác"
              },
              "note": {
                 "title": "Concise note title",
                 "content": "Full note body",
                 "category": "Công việc" | "Học tập" | "Cá nhân" | "Dự án" | "Tài chính" | "Ý tưởng" | "Khác",
                 "tags": ["tag1", "tag2"]
              },
              "searchQuery": "text to search"
            }
        """.trimIndent()

        val rawText = callGemini(userPrompt, systemInstruction, isJsonOutput = true, maxTokens = 500)
        if (!rawText.isNullOrBlank()) {
            try {
                val firstBrace = rawText.indexOf('{')
                val lastBrace = rawText.lastIndexOf('}')
                val jsonStr = if (firstBrace != -1 && lastBrace > firstBrace) {
                    rawText.substring(firstBrace, lastBrace + 1)
                } else {
                    rawText.trim()
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()
                }
                val json = JSONObject(jsonStr)
                val intentStr = json.optString("intent", "CREATE_TASK")
                val intent = try {
                    IntentType.valueOf(intentStr)
                } catch (_: Exception) {
                    IntentType.CREATE_TASK
                }

                val explanation = json.optString("explanation", "")
                val confidence = json.optDouble("confidence", 0.95).toFloat()

                val taskObj = json.optJSONObject("task")
                var taskData: ParsedTaskData? = null
                if (taskObj != null && taskObj.has("title")) {
                    val priorityStr = taskObj.optString("priority", "MEDIUM")
                    val priority = try { TaskPriority.valueOf(priorityStr) } catch (_: Exception) { TaskPriority.MEDIUM }
                    taskData = ParsedTaskData(
                        title = taskObj.optString("title", userPrompt),
                        description = taskObj.optString("description", ""),
                        date = taskObj.optString("date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)),
                        time = if (taskObj.has("time") && !taskObj.isNull("time") && taskObj.getString("time").isNotBlank()) taskObj.getString("time") else null,
                        reminderMinutesBefore = taskObj.optInt("reminderMinutesBefore", 0),
                        priority = priority,
                        category = normalizeCategory(taskObj.optString("category", "Công việc"), "Công việc")
                    )
                }

                // If CREATE_TASK intent recognized but task object was missing or empty
                if (intent == IntentType.CREATE_TASK && taskData == null) {
                    val ruleFallback = RuleBasedNLPFallback.parseIntent(userPrompt, currentDateTimeContext)
                    taskData = ruleFallback.taskData
                }

                val noteObj = json.optJSONObject("note")
                var noteData: ParsedNoteData? = null
                if (noteObj != null && noteObj.has("title")) {
                    val tagsList = mutableListOf<String>()
                    val tagsArray = noteObj.optJSONArray("tags")
                    if (tagsArray != null) {
                        for (i in 0 until tagsArray.length()) {
                            tagsList.add(tagsArray.getString(i))
                        }
                    }
                    noteData = ParsedNoteData(
                        title = noteObj.optString("title", "Ghi chú mới"),
                        content = noteObj.optString("content", userPrompt),
                        category = normalizeCategory(noteObj.optString("category", "Cá nhân"), "Cá nhân"),
                        tags = tagsList
                    )
                }

                if (intent == IntentType.CREATE_NOTE && noteData == null) {
                    val ruleFallback = RuleBasedNLPFallback.parseIntent(userPrompt, currentDateTimeContext)
                    noteData = ruleFallback.noteData
                }

                return AIIntentResult(
                    intent = intent,
                    confidence = confidence,
                    explanation = explanation,
                    taskData = taskData,
                    noteData = noteData,
                    searchQuery = if (json.has("searchQuery") && !json.isNull("searchQuery")) json.getString("searchQuery") else null,
                    rawPrompt = userPrompt
                )
            } catch (e: Exception) {
                Log.w("GeminiAIProvider", "Failed to parse JSON response: $rawText", e)
            }
        }

        // Seamless Fallback
        return RuleBasedNLPFallback.parseIntent(userPrompt, currentDateTimeContext)
    }

    override suspend fun chatWithAssistant(
        userMessage: String,
        currentTasks: List<TaskEntity>,
        currentNotes: List<NoteEntity>,
        conversationHistory: List<Pair<String, String>>
    ): String {
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val todayTasks = currentTasks.filter { it.dueDate == todayStr }
        val upcomingTasks = currentTasks.filter { it.dueDate > todayStr }
        val overdueTasks = currentTasks.filter { it.dueDate < todayStr && it.status != TaskStatus.COMPLETED }
        val urgentTasks = currentTasks.filter { it.status != TaskStatus.COMPLETED && it.priority == TaskPriority.URGENT }
        val highTasks = currentTasks.filter { it.status != TaskStatus.COMPLETED && it.priority == TaskPriority.HIGH }
        val mediumTasks = currentTasks.filter { it.status != TaskStatus.COMPLETED && it.priority == TaskPriority.MEDIUM }
        val lowTasks = currentTasks.filter { it.status != TaskStatus.COMPLETED && it.priority == TaskPriority.LOW }

        val contextInfo = buildString {
            appendLine("--- THÔNG TIN CÔNG VIỆC VÀ GHI CHÚ THỰC TẾ TRONG HỆ THỐNG ---")
            appendLine("Hôm nay là: $todayStr")
            appendLine("Tổng số công việc hiện có: ${currentTasks.size} việc (${currentTasks.count { it.status != TaskStatus.COMPLETED }} chưa xong)")

            appendLine("\n[TOÀN BỘ CÔNG VIỆC PHÂN THEO MỨC ĐỘ ƯU TIÊN]:")
            appendLine("- 🔴 KHẨN CẤP (${urgentTasks.size} việc):")
            if (urgentTasks.isEmpty()) appendLine("  (Không có)")
            else urgentTasks.forEach { appendLine("  * [Hạn: ${it.dueDate} ${it.dueTime ?: ""}] ${it.title} [Nhóm: ${it.category}]") }

            appendLine("- 🟠 CAO (${highTasks.size} việc):")
            if (highTasks.isEmpty()) appendLine("  (Không có)")
            else highTasks.forEach { appendLine("  * [Hạn: ${it.dueDate} ${it.dueTime ?: ""}] ${it.title} [Nhóm: ${it.category}]") }

            appendLine("- 🟡 TRUNG BÌNH (${mediumTasks.size} việc):")
            if (mediumTasks.isEmpty()) appendLine("  (Không có)")
            else mediumTasks.forEach { appendLine("  * [Hạn: ${it.dueDate} ${it.dueTime ?: ""}] ${it.title} [Nhóm: ${it.category}]") }

            appendLine("- 🟢 THẤP (${lowTasks.size} việc):")
            if (lowTasks.isEmpty()) appendLine("  (Không có)")
            else lowTasks.forEach { appendLine("  * [Hạn: ${it.dueDate} ${it.dueTime ?: ""}] ${it.title} [Nhóm: ${it.category}]") }

            appendLine("\n[CÔNG VIỆC HÔM NAY (${todayTasks.size})]:")
            if (todayTasks.isEmpty()) {
                appendLine("Không có công việc nào hôm nay.")
            } else {
                todayTasks.forEach { t ->
                    val statusMark = if (t.status == TaskStatus.COMPLETED) "[✓ Đã xong]" else "[ ]"
                    appendLine("- $statusMark ${t.dueTime ?: "Cả ngày"}: ${t.title} (Ưu tiên: ${t.priority.name}, Nhóm: ${t.category})")
                }
            }

            if (overdueTasks.isNotEmpty()) {
                appendLine("\n[CÔNG VIỆC QUÁ HẠN (${overdueTasks.size})]:")
                overdueTasks.forEach { t ->
                    appendLine("- [QUÁ HẠN từ ${t.dueDate}] ${t.title} (${t.priority.name})")
                }
            }

            if (upcomingTasks.isNotEmpty()) {
                appendLine("\n[CÔNG VIỆC SẮP TỚI (${upcomingTasks.size})]:")
                upcomingTasks.take(5).forEach { t ->
                    appendLine("- ${t.dueDate} ${t.dueTime ?: ""}: ${t.title}")
                }
            }

            appendLine("\n[GHI CHÚ GẦN ĐÂY (${currentNotes.size})]:")
            currentNotes.take(5).forEach { n ->
                appendLine("- [${n.category}] ${n.title}: ${n.content.take(60)}...")
            }
        }

        val systemInstruction = """
            Bạn là MemoAI — Trợ lý cá nhân thông minh quản lý ghi chú và công việc của người dùng (tên mặc định là Khương).
            Quy tắc giao tiếp & định dạng:
            1. Trả lời bằng tiếng Việt tự nhiên, thân thiện, lịch sự, văn phong thanh lịch và chuẩn mực. Tránh văn phong thô ráp hay cộc lốc.
            2. Trình bày nội dung đẹp mắt với định dạng Markdown: dùng gạch đầu dòng rõ ràng (- hoặc *), in đậm tiêu đề quan trọng (**tiêu đề**), kèm các biểu tượng emoji tinh tế (📌, ⏰, ⚡, 💡, 🎯, ✨).
            3. Sử dụng 100% dữ liệu thực tế được cung cấp bên dưới để trả lời các câu hỏi như 'Hôm nay tôi phải làm gì?', 'Việc nào quan trọng nhất?', 'Xếp loại tất cả công việc từ khẩn cấp đến thấp', v.v.
            4. Tuyệt đối không bịa đặt task không có trong danh sách. Nếu hôm nay không có việc, hãy chúc mừng người dùng thong thả hoặc nhắc các việc sắp tới.
            5. Khi người dùng yêu cầu xếp loại, phân loại hoặc sắp xếp công việc theo mức độ ưu tiên, hãy phân tích và trình bày danh sách chi tiết theo thứ tự: 🔴 Khẩn cấp -> 🟠 Cao -> 🟡 Trung bình -> 🟢 Thấp kèm thời hạn.
            6. Nếu người dùng đưa ra yêu cầu tạo công việc hoặc ghi chú, hệ thống MemoAI đã tự động lưu trữ vào cơ sở dữ liệu. Nếu bạn phản hồi, hãy tóm tắt lịch sự và nhắc người dùng xem tab Công việc hoặc Lịch trình.
            
            $contextInfo
        """.trimIndent()

        val promptWithHistory = buildString {
            conversationHistory.takeLast(4).forEach { (user, ai) ->
                appendLine("User: $user")
                appendLine("Assistant: $ai")
            }
            appendLine("User: $userMessage")
        }

        val aiResponse = callGemini(promptWithHistory, systemInstruction, isJsonOutput = false, maxTokens = 800)
        if (!aiResponse.isNullOrBlank()) {
            return aiResponse
        }

        // Local intelligent response fallback if offline or no API key
        val lower = userMessage.lowercase()
        return when {
            lower.contains("xếp loại") || lower.contains("phân loại") || lower.contains("sắp xếp") ||
                    (lower.contains("tất cả") && lower.contains("công việc")) || lower.contains("từ khẩn cấp") -> {
                if (currentTasks.isEmpty()) {
                    "Hiện tại bạn chưa có công việc nào trong danh sách. Hãy tạo công việc mới để tôi giúp bạn theo dõi nhé! ✨"
                } else {
                    buildString {
                        appendLine("📊 Dưới đây là phân loại toàn bộ công việc của bạn theo mức độ ưu tiên từ Khẩn cấp đến Thấp:\n")
                        appendLine("🔴 **1. Mức Khẩn cấp (${urgentTasks.size} việc):**")
                        if (urgentTasks.isEmpty()) appendLine("  _Không có công việc khẩn cấp nào._")
                        else urgentTasks.forEach { appendLine("  • **${it.title}** (Hạn: ${it.dueDate} ${it.dueTime ?: ""}) [${it.category}]") }

                        appendLine("\n🟠 **2. Mức Cao (${highTasks.size} việc):**")
                        if (highTasks.isEmpty()) appendLine("  _Không có công việc mức cao nào._")
                        else highTasks.forEach { appendLine("  • **${it.title}** (Hạn: ${it.dueDate} ${it.dueTime ?: ""}) [${it.category}]") }

                        appendLine("\n🟡 **3. Mức Trung bình (${mediumTasks.size} việc):**")
                        if (mediumTasks.isEmpty()) appendLine("  _Không có công việc mức trung bình._")
                        else mediumTasks.forEach { appendLine("  • **${it.title}** (Hạn: ${it.dueDate} ${it.dueTime ?: ""}) [${it.category}]") }

                        appendLine("\n🟢 **4. Mức Thấp (${lowTasks.size} việc):**")
                        if (lowTasks.isEmpty()) appendLine("  _Không có công việc mức thấp._")
                        else lowTasks.forEach { appendLine("  • **${it.title}** (Hạn: ${it.dueDate} ${it.dueTime ?: ""}) [${it.category}]") }
                    }
                }
            }
            lower.contains("hôm nay") || lower.contains("phải làm gì") || lower.contains("làm gì hôm nay") -> {
                if (todayTasks.isEmpty()) {
                    "🎉 Hôm nay ($todayStr) bạn không có công việc nào trong lịch trình! Bạn có muốn ghi chú ý tưởng mới hoặc lên lịch cho ngày mai không?"
                } else {
                    buildString {
                        appendLine("📋 Hôm nay bạn có ${todayTasks.size} công việc cần thực hiện:\n")
                        todayTasks.forEach { t ->
                            val statusIcon = if (t.status == TaskStatus.COMPLETED) "✅" else "⏳"
                            val time = t.dueTime ?: "Cả ngày"
                            val priIcon = when (t.priority) {
                                TaskPriority.URGENT -> "🔴"
                                TaskPriority.HIGH -> "🟠"
                                TaskPriority.MEDIUM -> "🟡"
                                TaskPriority.LOW -> "🟢"
                            }
                            appendLine("$statusIcon $priIcon $time — ${t.title} [${t.category}]")
                        }
                    }
                }
            }
            lower.contains("quan trọng nhất") || lower.contains("ưu tiên") -> {
                val highest = todayTasks.maxByOrNull { it.priority.ordinal } ?: currentTasks.filter { it.status != TaskStatus.COMPLETED }.maxByOrNull { it.priority.ordinal }
                if (highest != null) {
                    "⚡ Việc quan trọng nhất của bạn lúc này là:\n👉 **${highest.title}** (Mức ưu tiên: ${highest.priority.name}, Hạn: ${highest.dueDate} ${highest.dueTime ?: ""}). Hãy tập trung hoàn thành nó trước nhé!"
                } else {
                    "Hiện tại bạn không có việc nào đang tồn đọng với mức ưu tiên cao. Bạn đang quản lý thời gian rất tốt!"
                }
            }
            lower.contains("ghi chú") -> {
                if (currentNotes.isEmpty()) {
                    "Bạn chưa có ghi chú nào. Hãy nhấn biểu tượng bút viết hoặc nói 'Ghi chú ý tưởng...' để tạo ngay nhé!"
                } else {
                    buildString {
                        appendLine("📝 Bạn có ${currentNotes.size} ghi chú trong sổ tay. Các ghi chú gần nhất:\n")
                        currentNotes.take(3).forEach {
                            appendLine("• **${it.title}** (${it.category})")
                        }
                    }
                }
            }
            else -> {
                "Tôi đã sẵn sàng hỗ trợ bạn quản lý ghi chú, lịch hẹn và công việc! Bạn có thể hỏi tôi: 'Hôm nay tôi phải làm gì?', 'Việc nào quan trọng nhất?', hoặc ra lệnh: 'Ngày mai 8h sáng nhắc tôi họp team'."
            }
        }
    }

    override suspend fun summarizeNote(noteTitle: String, noteContent: String): NoteSummaryResult {
        val systemInstruction = """
            Tóm tắt ghi chú sau đây.
            Trả về JSON thuần túy (không markdown, không ```json):
            {
               "summary": "Đoạn tóm tắt ngắn gọn 2-3 câu",
               "keyPoints": ["Ý chính 1", "Ý chính 2", "Ý chính 3"],
               "actionItems": ["Hành động 1", "Hành động 2"]
            }
        """.trimIndent()

        val prompt = "Tiêu đề: $noteTitle\nNội dung: $noteContent"
        val response = callGemini(prompt, systemInstruction, isJsonOutput = true, maxTokens = 600)
        if (!response.isNullOrBlank()) {
            try {
                val clean = response.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val json = JSONObject(clean)
                val summary = json.optString("summary", "")
                val kpList = mutableListOf<String>()
                val kpArray = json.optJSONArray("keyPoints")
                if (kpArray != null) {
                    for (i in 0 until kpArray.length()) kpList.add(kpArray.getString(i))
                }
                val actList = mutableListOf<String>()
                val actArray = json.optJSONArray("actionItems")
                if (actArray != null) {
                    for (i in 0 until actArray.length()) actList.add(actArray.getString(i))
                }
                return NoteSummaryResult(summary, kpList, actList)
            } catch (e: Exception) {
                Log.w("GeminiAIProvider", "Failed to parse summary json", e)
            }
        }

        // Local fallback summary
        val lines = noteContent.split("\n").filter { it.isNotBlank() }
        val summary = if (noteContent.length > 120) noteContent.take(120) + "..." else noteContent
        val keyPoints = lines.take(3).map { it.trim().removePrefix("-").removePrefix("•").trim() }
        val actionItems = lines.filter { it.contains("cần", ignoreCase = true) || it.contains("phải", ignoreCase = true) || it.contains("hẹn", ignoreCase = true) }
            .map { it.trim() }

        return NoteSummaryResult(
            summary = summary,
            keyPoints = if (keyPoints.isNotEmpty()) keyPoints else listOf(noteTitle),
            actionItems = if (actionItems.isNotEmpty()) actionItems else listOf("Xem lại nội dung ghi chú")
        )
    }

    override suspend fun extractTasksFromNote(noteTitle: String, noteContent: String, currentDate: String): List<ExtractedTask> {
        val systemInstruction = """
            Phân tích ghi chú và trích xuất tất cả các công việc (Action items / Tasks) cần làm.
            Hôm nay là: $currentDate.
            Trả về JSON thuần túy (không markdown, không ```json):
            [
              {
                "title": "Tên công việc",
                "dueDate": "YYYY-MM-DD",
                "dueTime": "HH:mm" (hoặc null),
                "priority": "LOW" | "MEDIUM" | "HIGH" | "URGENT"
              }
            ]
        """.trimIndent()

        val prompt = "Tiêu đề: $noteTitle\nNội dung:\n$noteContent"
        val response = callGemini(prompt, systemInstruction, isJsonOutput = true, maxTokens = 800)
        if (!response.isNullOrBlank()) {
            try {
                val clean = response.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val array = JSONArray(clean)
                val list = mutableListOf<ExtractedTask>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val pStr = obj.optString("priority", "MEDIUM")
                    val priority = try { TaskPriority.valueOf(pStr) } catch (_: Exception) { TaskPriority.MEDIUM }
                    list.add(
                        ExtractedTask(
                            title = obj.optString("title", "Công việc trích xuất"),
                            dueDate = obj.optString("dueDate", currentDate),
                            dueTime = if (obj.has("dueTime") && !obj.isNull("dueTime")) obj.getString("dueTime") else null,
                            priority = priority
                        )
                    )
                }
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                Log.w("GeminiAIProvider", "Failed to parse extract tasks json", e)
            }
        }

        // Local fallback extraction
        val lines = noteContent.split("\n", ".", ",").map { it.trim() }.filter { it.length > 5 }
        val extracted = mutableListOf<ExtractedTask>()
        val tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)

        for (line in lines) {
            val lower = line.lowercase()
            if (lower.contains("gửi") || lower.contains("hoàn thành") || lower.contains("họp") ||
                lower.contains("nộp") || lower.contains("deploy") || lower.contains("kiểm tra") ||
                lower.contains("gọi") || lower.contains("mua")
            ) {
                val cleanTitle = line.removePrefix("-").removePrefix("•").trim()
                extracted.add(
                    ExtractedTask(
                        title = cleanTitle.replaceFirstChar { it.uppercase() },
                        dueDate = if (lower.contains("ngày mai") || lower.contains("mai")) tomorrow else currentDate,
                        dueTime = "09:00",
                        priority = if (lower.contains("gấp") || lower.contains("hoàn thành")) TaskPriority.HIGH else TaskPriority.MEDIUM
                    )
                )
            }
        }

        if (extracted.isEmpty()) {
            extracted.add(
                ExtractedTask(
                    title = "Theo dõi: $noteTitle",
                    dueDate = tomorrow,
                    dueTime = "09:00",
                    priority = TaskPriority.MEDIUM
                )
            )
        }

        return extracted
    }
}
