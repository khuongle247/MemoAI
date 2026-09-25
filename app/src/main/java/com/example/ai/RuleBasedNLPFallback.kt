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

        // 1. Detect Note Intent
        val isNote = lower.startsWith("ghi chú") ||
                lower.startsWith("note") ||
                lower.contains("nhớ giúp tôi") ||
                lower.contains("ghi lại") ||
                lower.contains("ý tưởng") ||
                lower.contains("mật khẩu wifi") ||
                lower.contains("lưu lại")

        if (isNote) {
            var content = prompt
                .replace(Regex("^(ghi chú rằng|ghi chú là|ghi chú|nhớ giúp tôi là|nhớ giúp tôi|note lại|lưu lại|ghi lại ý tưởng này là|ghi lại)\\s*", RegexOption.IGNORE_CASE), "")
                .trim()
            if (content.isEmpty()) content = prompt

            val title = if (content.length > 40) {
                val firstDot = content.indexOf('.')
                if (firstDot in 5..40) content.substring(0, firstDot)
                else content.take(35) + "..."
            } else {
                content.take(40)
            }

            val category = when {
                lower.contains("wifi") || lower.contains("nhà") -> "Cá nhân"
                lower.contains("học") || lower.contains("bài") -> "Học tập"
                lower.contains("code") || lower.contains("dự án") || lower.contains("project") -> "Dự án"
                lower.contains("tiền") || lower.contains("chi") || lower.contains("lương") -> "Tài chính"
                else -> "Ý tưởng"
            }

            return AIIntentResult(
                intent = IntentType.CREATE_NOTE,
                confidence = 0.88f,
                explanation = "Phát hiện ý định tạo Ghi chú từ giọng nói/văn bản.",
                noteData = ParsedNoteData(
                    title = title.replaceFirstChar { it.uppercase() },
                    content = content,
                    category = category,
                    tags = listOf(category.lowercase())
                ),
                rawPrompt = prompt
            )
        }

        // 2. Detect Search / Query Tasks
        if (lower.contains("hôm nay tôi phải làm gì") ||
            lower.contains("hôm nay làm gì") ||
            lower.contains("xem công việc") ||
            lower.contains("cho tôi xem hôm nay") ||
            lower.contains("việc cần làm")
        ) {
            return AIIntentResult(
                intent = IntentType.SEARCH_TASK,
                confidence = 0.95f,
                explanation = "Tìm kiếm danh sách công việc trong ngày hôm nay.",
                searchQuery = today.format(DateTimeFormatter.ISO_LOCAL_DATE),
                rawPrompt = prompt
            )
        }

        // 3. Detect Task Intent (Default or specific)
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
            .replace(Regex("^(ngày mai|ngày kia|mai|hôm nay|thứ [2-7]|chủ nhật)\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(lúc|vào lúc)?\\s*\\d{1,2}\\s*(?:giờ|h)(?:\\s*\\d{1,2})?\\s*(sáng|trưa|chiều|tối)?", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\d{1,2}:\\d{2}", RegexOption.IGNORE_CASE), "")
            .replace(Regex("nhớ nhắc tôi|nhắc tôi|nhắc|hãy nhắc|giúp tôi", RegexOption.IGNORE_CASE), "")
            .replace(Regex("trong \\d+\\s*(tiếng|phút|giờ)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("và nhắc tôi trước \\d+\\s*(phút|tiếng)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("nhắc trước \\d+\\s*(phút|tiếng)", RegexOption.IGNORE_CASE), "")
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
