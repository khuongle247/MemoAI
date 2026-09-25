package com.example.data

import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskPriority
import com.example.data.local.entity.TaskStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object RecurrenceHelper {

    const val NONE = "NONE"
    const val DAILY = "DAILY"
    const val WEEKDAYS = "WEEKDAYS"
    const val WEEKLY = "WEEKLY"
    const val MONTHLY = "MONTHLY"

    /**
     * Format a recurrence rule into user-friendly Vietnamese text.
     */
    fun formatRuleLabel(rule: String): String {
        return when {
            rule.isBlank() || rule == NONE -> "Không lặp lại"
            rule == DAILY -> "Hàng ngày"
            rule == WEEKDAYS -> "Ngày trong tuần (T2 - T6)"
            rule == WEEKLY -> "Hàng tuần"
            rule == MONTHLY -> "Hàng tháng"
            rule.startsWith("CUSTOM:") -> {
                val parts = rule.split(":")
                if (parts.size == 3) {
                    val count = parts[1].toIntOrNull() ?: 1
                    val unit = parts[2]
                    when (unit.uppercase()) {
                        "DAYS" -> if (count == 1) "Hàng ngày" else "Mỗi $count ngày"
                        "WEEKS" -> if (count == 1) "Hàng tuần" else "Mỗi $count tuần"
                        "MONTHS" -> if (count == 1) "Hàng tháng" else "Mỗi $count tháng"
                        else -> "Tùy chỉnh: $count $unit"
                    }
                } else {
                    "Tùy chỉnh"
                }
            }
            else -> rule
        }
    }

    /**
     * Compute the next due date based on the current due date and repeat rule.
     * Guarantees returning a valid YYYY-MM-DD string, advancing to present/future if needed.
     */
    fun computeNextDueDate(currentDueDateStr: String, rule: String): String {
        if (rule.isBlank() || rule == NONE) return currentDueDateStr

        val baseDate = try {
            LocalDate.parse(currentDueDateStr)
        } catch (e: Exception) {
            LocalDate.now()
        }

        val today = LocalDate.now()
        var nextDate: LocalDate = when {
            rule == DAILY -> baseDate.plusDays(1)
            rule == WEEKDAYS -> {
                var candidate = baseDate.plusDays(1)
                while (candidate.dayOfWeek == DayOfWeek.SATURDAY || candidate.dayOfWeek == DayOfWeek.SUNDAY) {
                    candidate = candidate.plusDays(1)
                }
                candidate
            }
            rule == WEEKLY -> baseDate.plusWeeks(1)
            rule == MONTHLY -> baseDate.plusMonths(1)
            rule.startsWith("CUSTOM:") -> {
                val parts = rule.split(":")
                if (parts.size == 3) {
                    val count = (parts[1].toIntOrNull() ?: 1).toLong().coerceAtLeast(1L)
                    when (parts[2].uppercase()) {
                        "DAYS" -> baseDate.plusDays(count)
                        "WEEKS" -> baseDate.plusWeeks(count)
                        "MONTHS" -> baseDate.plusMonths(count)
                        else -> baseDate.plusDays(1)
                    }
                } else {
                    baseDate.plusDays(1)
                }
            }
            else -> baseDate.plusDays(1)
        }

        // If nextDate is strictly in the past relative to today, keep stepping forward until it's today or later
        if (nextDate.isBefore(today)) {
            val stepDays = when {
                rule == DAILY -> 1L
                rule == WEEKLY -> 7L
                rule.startsWith("CUSTOM:") -> {
                    val parts = rule.split(":")
                    if (parts.size == 3 && parts[2].uppercase() == "DAYS") {
                        (parts[1].toIntOrNull() ?: 1).toLong().coerceAtLeast(1L)
                    } else if (parts.size == 3 && parts[2].uppercase() == "WEEKS") {
                        (parts[1].toIntOrNull() ?: 1).toLong().coerceAtLeast(1L) * 7L
                    } else {
                        1L
                    }
                }
                else -> 1L
            }

            while (nextDate.isBefore(today)) {
                nextDate = when {
                    rule == MONTHLY -> nextDate.plusMonths(1)
                    rule.startsWith("CUSTOM:") && rule.split(":").getOrNull(2)?.uppercase() == "MONTHS" -> {
                        val mCount = (rule.split(":")[1].toIntOrNull() ?: 1).toLong().coerceAtLeast(1L)
                        nextDate.plusMonths(mCount)
                    }
                    rule == WEEKDAYS -> {
                        var candidate = nextDate.plusDays(1)
                        while (candidate.dayOfWeek == DayOfWeek.SATURDAY || candidate.dayOfWeek == DayOfWeek.SUNDAY) {
                            candidate = candidate.plusDays(1)
                        }
                        candidate
                    }
                    else -> nextDate.plusDays(stepDays)
                }
            }
        }

        return nextDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    /**
     * Create the next instance for a recurring task.
     * Returns null if repeatRule is NONE or empty.
     */
    fun createNextRecurringTask(completedTask: TaskEntity): TaskEntity? {
        if (completedTask.repeatRule.isBlank() || completedTask.repeatRule == NONE) {
            return null
        }

        val nextDueDate = computeNextDueDate(completedTask.dueDate, completedTask.repeatRule)
        val now = System.currentTimeMillis()

        return completedTask.copy(
            id = 0,
            status = TaskStatus.TODO,
            dueDate = nextDueDate,
            createdAt = now,
            updatedAt = now,
            completedAt = null
        )
    }
}
