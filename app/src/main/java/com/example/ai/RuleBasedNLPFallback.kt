package com.example.ai

import com.example.data.local.entity.TaskPriority
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object RuleBasedNLPFallback {

    fun parseIntent(prompt: String, currentDateTimeContext: String): AIIntentResult {
        val lower = prompt.lowercase(Locale.getDefault()).trim()
        val today = LocalDate.now()
        val now = LocalTime.now()

        // 1. Detect Note Intent (PRIORITY: Note keywords must trigger CREATE_NOTE)
        val isNote = lower.contains("ghi chú") ||
                lower.contains("tạo 1 ghi chú") ||
                lower.contains("tạo một ghi chú") ||
                lower.contains("tạo ghi chú") ||
                lower.contains("thêm ghi chú") ||
                lower.contains("viết ghi chú") ||
                lower.contains("lưu ghi chú") ||
                lower.contains("note") ||
                lower.contains("tạo note") ||
                lower.contains("nhớ giúp tôi") ||
                lower.contains("ghi lại") ||
                lower.contains("ý tưởng") ||
                lower.contains("mật khẩu wifi") ||
                lower.contains("lưu lại")

        if (isNote) {
            var rawBody = prompt
                .replace(
                    Regex(
                        "^(tạo\\s*(cho tôi|giúp tôi)?\\s*(1|một)?\\s*ghi chú\\s*(mới)?\\s*(là|rằng|với nội dung|nội dung|:)?|" +
                        "thêm\\s*(cho tôi|giúp tôi)?\\s*(1|một)?\\s*ghi chú\\s*(mới)?\\s*(là|rằng|với nội dung|nội dung|:)?|" +
                        "viết\\s*(1|một)?\\s*ghi chú\\s*(là|rằng|:)?|" +
                        "lưu\\s*(1|một)?\\s*ghi chú\\s*(là|rằng|:)?|" +
                        "ghi chú\\s*(rằng|là|mới là|mới|:)?|" +
                        "tạo\\s*(1|một)?\\s*note\\s*(là|rằng|:)?|" +
                        "thêm\\s*(1|một)?\\s*note\\s*(là|rằng|:)?|" +
                        "note\\s*(lại là|lại|rằng|là|:)?|" +
                        "nhớ giúp tôi\\s*(là|rằng|:)?|" +
                        "ghi lại\\s*(ý tưởng này là|ý tưởng|là|rằng|:)?|" +
                        "lưu lại\\s*(rằng|là|:)?)\\s*",
                        RegexOption.IGNORE_CASE
                    ),
                    ""
                )
                .trim()
            if (rawBody.isEmpty()) rawBody = prompt

            var extractedTitle = ""
            var extractedContent = ""

            // Pattern 1: "Tiêu đề [X] nội dung (là) [Y]"
            val titleFirstRegex = Regex("(?:có\\s*)?tiêu đề\\s*(?:là|:)?\\s*(.+?)\\s+(?:với\\s*)?nội dung\\s*(?:là|:)?\\s*(.+)", RegexOption.IGNORE_CASE)
            val matchTitleFirst = titleFirstRegex.find(rawBody)

            // Pattern 2: "Nội dung (là) [X] tiêu đề (là) [Y]"
            val contentFirstRegex = Regex("(?:với\\s*)?nội dung\\s*(?:là|:)?\\s*(.+?)\\s+(?:có\\s*)?tiêu đề\\s*(?:là|:)?\\s*(.+)", RegexOption.IGNORE_CASE)
            val matchContentFirst = contentFirstRegex.find(rawBody)

            if (matchTitleFirst != null) {
                extractedTitle = matchTitleFirst.groupValues[1].trim()
                extractedContent = matchTitleFirst.groupValues[2].trim()
            } else if (matchContentFirst != null) {
                extractedContent = matchContentFirst.groupValues[1].trim()
                extractedTitle = matchContentFirst.groupValues[2].trim()
            } else {
                // If user specifies only "tiêu đề (là) [X]"
                val onlyTitleRegex = Regex("^(?:có\\s*)?tiêu đề\\s*(?:là|:)?\\s*(.+)", RegexOption.IGNORE_CASE)
                val onlyTitleMatch = onlyTitleRegex.find(rawBody)

                // If user specifies only "nội dung (là) [X]"
                val onlyContentRegex = Regex("^(?:với\\s*)?nội dung\\s*(?:là|:)?\\s*(.+)", RegexOption.IGNORE_CASE)
                val onlyContentMatch = onlyContentRegex.find(rawBody)

                if (onlyTitleMatch != null) {
                    val full = onlyTitleMatch.groupValues[1].trim()
                    extractedTitle = full
                    extractedContent = full
                } else if (onlyContentMatch != null) {
                    val full = onlyContentMatch.groupValues[1].trim()
                    extractedContent = full
                    extractedTitle = if (full.length > 40) {
                        val firstSentence = full.split(".", "\n", ",", ";").firstOrNull()?.trim() ?: ""
                        if (firstSentence.length in 5..40) firstSentence
                        else full.take(35).trim() + "..."
                    } else full
                } else {
                    extractedContent = rawBody
                    extractedTitle = if (rawBody.length > 40) {
                        val firstSentence = rawBody.split(".", "\n", ",", ";").firstOrNull()?.trim() ?: ""
                        if (firstSentence.length in 5..40) firstSentence
                        else rawBody.take(35).trim() + "..."
                    } else {
                        rawBody
                    }
                }
            }

            // Strip any remaining structural markers
            extractedTitle = extractedTitle
                .replace(Regex("^(tiêu đề\\s*(?:là|:)?|nội dung\\s*(?:là|:)?)\\s*", RegexOption.IGNORE_CASE), "")
                .trim()
            extractedContent = extractedContent
                .replace(Regex("^(nội dung\\s*(?:là|:)?|tiêu đề\\s*(?:là|:)?)\\s*", RegexOption.IGNORE_CASE), "")
                .trim()

            if (extractedTitle.isEmpty()) extractedTitle = extractedContent.take(35)
            if (extractedContent.isEmpty()) extractedContent = extractedTitle

            val category = when {
                lower.contains("wifi") || lower.contains("nhà") || lower.contains("mua") -> "Cá nhân"
                lower.contains("học") || lower.contains("bài") || lower.contains("sách") -> "Học tập"
                lower.contains("code") || lower.contains("dự án") || lower.contains("project") -> "Dự án"
                lower.contains("tiền") || lower.contains("chi") || lower.contains("lương") -> "Tài chính"
                lower.contains("công việc") || lower.contains("họp") || lower.contains("báo cáo") -> "Công việc"
                else -> "Ý tưởng"
            }

            return AIIntentResult(
                intent = IntentType.CREATE_NOTE,
                confidence = 0.98f,
                explanation = "Phát hiện ý định tạo Ghi chú từ giọng nói/văn bản.",
                noteData = ParsedNoteData(
                    title = extractedTitle.replaceFirstChar { it.uppercase() },
                    content = extractedContent.replaceFirstChar { it.uppercase() },
                    category = category,
                    tags = listOf(category.lowercase())
                ),
                rawPrompt = prompt
            )
        }

        // 2. Detect Analysis / Query / Ranking / Questions (MUST NOT BECOME A TASK!)
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
                lower.contains("làm gì hôm nay") ||
                lower.contains("hôm nay làm gì") ||
                lower.contains("hôm nay tôi phải làm gì") ||
                lower.contains("xem công việc") ||
                lower.contains("cho tôi xem") ||
                lower.contains("có những việc gì") ||
                lower.contains("gợi ý") ||
                lower.contains("tư vấn") ||
                lower.contains("giúp tôi lên kế hoạch") ||
                lower.contains("thế nào") ||
                lower.contains("như thế nào") ||
                lower.contains("tại sao") ||
                lower.contains("làm sao") ||
                lower.endsWith("?") ||
                lower.endsWith("ạ?")

        if (isQueryOrAnalysis) {
            return AIIntentResult(
                intent = IntentType.GENERAL_QUERY,
                confidence = 0.95f,
                explanation = "Yêu cầu hỏi đáp / phân tích công việc cho Trợ lý AI",
                rawPrompt = prompt
            )
        }

        // 3. Detect Task Intent ONLY if user explicitly wants to create / schedule a task
        val isExplicitTaskCreation = lower.startsWith("tạo việc") ||
                lower.startsWith("tạo nhiệm vụ") ||
                lower.startsWith("thêm việc") ||
                lower.startsWith("thêm nhiệm vụ") ||
                lower.startsWith("tạo task") ||
                lower.startsWith("thêm task") ||
                lower.startsWith("lên lịch") ||
                lower.startsWith("đặt lịch") ||
                lower.contains("nhắc tôi") ||
                lower.contains("nhắc việc") ||
                lower.contains("hẹn lịch") ||
                lower.contains("hẹn giờ") ||
                lower.contains("báo thức") ||
                lower.startsWith("hẹn ") ||
                (lower.contains("ngày mai") || lower.contains("chiều mai") || lower.contains("sáng mai") || lower.contains("tối mai"))

        if (!isExplicitTaskCreation) {
            return AIIntentResult(
                intent = IntentType.GENERAL_QUERY,
                confidence = 0.85f,
                explanation = "Yêu cầu hội thoại / hỏi đáp cho Trợ lý AI",
                rawPrompt = prompt
            )
        }

        var targetDate = today
        var targetTime: LocalTime? = null
        var reminderMinutes = 0

        // Parse date
        when {
            lower.contains("ngày kia") || lower.contains("mốt") -> {
                targetDate = today.plusDays(2)
            }
            lower.contains("ngày mai") || lower.contains("sáng mai") || lower.contains("chiều mai") || lower.contains("tối mai") || lower.contains("mai") -> {
                targetDate = today.plusDays(1)
            }
            lower.contains("tuần sau") -> {
                targetDate = today.plusWeeks(1)
            }
            lower.contains("thứ hai") || lower.contains("thứ 2") -> {
                targetDate = getNextDayOfWeek(today, DayOfWeek.MONDAY)
            }
            lower.contains("thứ ba") || lower.contains("thứ 3") -> {
                targetDate = getNextDayOfWeek(today, DayOfWeek.TUESDAY)
            }
            lower.contains("thứ tư") || lower.contains("thứ 4") -> {
                targetDate = getNextDayOfWeek(today, DayOfWeek.WEDNESDAY)
            }
            lower.contains("thứ năm") || lower.contains("thứ 5") -> {
                targetDate = getNextDayOfWeek(today, DayOfWeek.THURSDAY)
            }
            lower.contains("thứ sáu") || lower.contains("thứ 6") -> {
                targetDate = getNextDayOfWeek(today, DayOfWeek.FRIDAY)
            }
            lower.contains("thứ bảy") || lower.contains("thứ 7") -> {
                targetDate = getNextDayOfWeek(today, DayOfWeek.SATURDAY)
            }
            lower.contains("chủ nhật") || lower.contains("cn") -> {
                targetDate = getNextDayOfWeek(today, DayOfWeek.SUNDAY)
            }
        }

        // Relative hours / minutes from now (e.g. "sau 2 tiếng", "2 tiếng nữa", "sau 30 phút")
        val hoursAheadMatch = Regex("(\\d+)\\s*(tiếng|giờ)\\s*nữa|sau\\s*(\\d+)\\s*(tiếng|giờ)").find(lower)
        if (hoursAheadMatch != null) {
            val num = (hoursAheadMatch.groupValues[1].ifEmpty { hoursAheadMatch.groupValues[3] }).toLongOrNull() ?: 1
            val future = now.plusHours(num)
            targetTime = future
        }

        val minutesAheadMatch = Regex("(\\d+)\\s*(phút)\\s*nữa|sau\\s*(\\d+)\\s*(phút)").find(lower)
        if (minutesAheadMatch != null) {
            val num = (minutesAheadMatch.groupValues[1].ifEmpty { minutesAheadMatch.groupValues[3] }).toLongOrNull() ?: 30
            val future = now.plusMinutes(num)
            targetTime = future
        }

        // Specific time parsing (e.g. "8 giờ sáng", "8h sáng", "7 giờ tối", "19:00", "14h30", "18:45")
        if (targetTime == null) {
            val timeRegex = Regex("(\\d{1,2})\\s*(?:giờ|h)(?:\\s*(\\d{1,2}))?\\s*(sáng|trưa|chiều|tối)?|(\\d{1,2}):(\\d{2})")
            val match = timeRegex.find(lower)
            if (match != null) {
                if (match.groupValues[4].isNotEmpty() && match.groupValues[5].isNotEmpty()) {
                    val h = match.groupValues[4].toIntOrNull() ?: 9
                    val m = match.groupValues[5].toIntOrNull() ?: 0
                    targetTime = LocalTime.of(h.coerceIn(0, 23), m.coerceIn(0, 59))
                } else {
                    var h = match.groupValues[1].toIntOrNull() ?: 9
                    val m = match.groupValues[2].toIntOrNull() ?: 0
                    val period = match.groupValues[3]
                    if ((period == "tối" || period == "chiều") && h < 12) {
                        h += 12
                    } else if (period == "sáng" && h == 12) {
                        h = 0
                    }
                    targetTime = LocalTime.of(h.coerceIn(0, 23), m.coerceIn(0, 59))
                }
            } else {
                // Default morning if "sáng mai"
                if (lower.contains("sáng")) targetTime = LocalTime.of(8, 0)
                else if (lower.contains("chiều")) targetTime = LocalTime.of(14, 0)
                else if (lower.contains("tối")) targetTime = LocalTime.of(19, 0)
                else targetTime = LocalTime.of(9, 0)
            }
        }

        // Reminder offset
        if (lower.contains("trước 15 phút")) reminderMinutes = 15
        else if (lower.contains("trước 30 phút")) reminderMinutes = 30
        else if (lower.contains("trước 1 tiếng") || lower.contains("trước 1 giờ")) reminderMinutes = 60
        else if (lower.contains("trước 10 phút")) reminderMinutes = 10

        // Extract title
        var cleanTitle = prompt
            .replace(Regex("^(lên cho tôi|tạo cho tôi|thêm cho tôi|lên lịch cho tôi|lên lịch|lên|tạo|thêm|đặt lịch|hẹn lịch)\\s*(1|một)?\\s*(nhiệm vụ|công việc|task|việc)?\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^(ngày mai|ngày kia|mai|hôm nay|thứ [2-7]|chủ nhật)\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(lúc|vào lúc)?\\s*\\d{1,2}\\s*(?:giờ|h)(?:\\s*\\d{1,2})?\\s*(sáng|trưa|chiều|tối)?\\s*(nay|mai)?", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\d{1,2}:\\d{2}", RegexOption.IGNORE_CASE), "")
            .replace(Regex("nhớ nhắc tôi|nhắc tôi|nhắc|hãy nhắc|giúp tôi", RegexOption.IGNORE_CASE), "")
            .replace(Regex("trong \\d+\\s*(tiếng|phút|giờ)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("và nhắc tôi trước \\d+\\s*(phút|tiếng)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("nhắc trước \\d+\\s*(phút|tiếng)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^(nội dung là|nội dung|với nội dung là|với nội dung|về việc là|về việc)\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex(",\\s*(nội dung là|nội dung|với nội dung là|với nội dung|về việc là|về việc)\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^(có\\s*)?tiêu đề\\s*(?:là|:)?\\s*", RegexOption.IGNORE_CASE), "")
            .trim()

        if (cleanTitle.isEmpty() || cleanTitle.length < 3) {
            cleanTitle = prompt
        }

        // Priority
        val priority = when {
            lower.contains("gấp") || lower.contains("urgent") || lower.contains("khẩn cấp") -> TaskPriority.URGENT
            lower.contains("quan trọng") || lower.contains("báo cáo") || lower.contains("deadline") || lower.contains("thi") -> TaskPriority.HIGH
            lower.contains("khi rảnh") || lower.contains("thong thả") -> TaskPriority.LOW
            else -> TaskPriority.MEDIUM
        }

        // Category
        val category = when {
            lower.contains("học") || lower.contains("bài") || lower.contains("spring") || lower.contains("java") || lower.contains("android") -> "Học tập"
            lower.contains("họp") || lower.contains("báo cáo") || lower.contains("api") || lower.contains("deploy") || lower.contains("sếp") -> "Công việc"
            lower.contains("mẹ") || lower.contains("bố") || lower.contains("bạn") || lower.contains("gia đình") || lower.contains("gym") -> "Cá nhân"
            lower.contains("tiền") || lower.contains("ngân hàng") || lower.contains("hóa đơn") -> "Tài chính"
            else -> "Công việc"
        }

        val dateStr = targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val timeStr = targetTime.format(DateTimeFormatter.ofPattern("HH:mm"))

        return AIIntentResult(
            intent = IntentType.CREATE_TASK,
            confidence = 0.92f,
            explanation = "Đã phân tích yêu cầu tạo công việc với lịch hẹn.",
            taskData = ParsedTaskData(
                title = cleanTitle.replaceFirstChar { it.uppercase() },
                date = dateStr,
                time = timeStr,
                reminderMinutesBefore = reminderMinutes,
                priority = priority,
                category = category
            ),
            rawPrompt = prompt
        )
    }

    private fun getNextDayOfWeek(from: LocalDate, targetDay: DayOfWeek): LocalDate {
        var d = from.plusDays(1)
        while (d.dayOfWeek != targetDay) {
            d = d.plusDays(1)
        }
        return d
    }
}
