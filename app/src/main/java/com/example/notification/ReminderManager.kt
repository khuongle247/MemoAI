package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.entity.TaskEntity
import com.example.receiver.ReminderBroadcastReceiver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ReminderManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val CHANNEL_ID = "task_reminders_channel"
        const val CHANNEL_NAME = "Nhắc nhở công việc"
        const val ACTION_REMINDER = "com.example.memoai.ACTION_TASK_REMINDER"
        const val ACTION_COMPLETE = "com.example.memoai.ACTION_TASK_COMPLETE"
        const val ACTION_SNOOZE = "com.example.memoai.ACTION_TASK_SNOOZE"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_TIME = "extra_task_time"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo nhắc nhở công việc và hạn chót"
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleTaskReminder(task: TaskEntity) {
        val triggerMillis = calculateTriggerMillis(task.dueDate, task.dueTime, task.reminderMinutesBefore)
        if (triggerMillis == null || triggerMillis <= System.currentTimeMillis()) {
            Log.d("ReminderManager", "Trigger time is in the past or invalid: $triggerMillis")
            return
        }

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TASK_TITLE, task.title)
            putExtra(EXTRA_TASK_TIME, task.dueTime ?: "")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
            Log.d("ReminderManager", "Scheduled reminder for task '${task.title}' at $triggerMillis")
        } catch (e: SecurityException) {
            Log.w("ReminderManager", "Cannot schedule exact alarm: ${e.message}")
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } catch (e: Exception) {
            Log.e("ReminderManager", "Error scheduling alarm", e)
        }
    }

    fun cancelTaskReminder(taskId: Long) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("ReminderManager", "Cancelled reminder for task ID $taskId")
        }
    }

    fun snoozeTask(taskId: Long, title: String, snoozeMinutes: Int = 10) {
        val triggerMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, title)
            putExtra(EXTRA_TASK_TIME, "+$snoozeMinutes phút")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }

    private fun calculateTriggerMillis(dueDateStr: String, dueTimeStr: String?, minutesBefore: Int): Long? {
        return try {
            val date = LocalDate.parse(dueDateStr, DateTimeFormatter.ISO_LOCAL_DATE)
            val time = if (!dueTimeStr.isNullOrBlank()) {
                LocalTime.parse(dueTimeStr, DateTimeFormatter.ofPattern("HH:mm"))
            } else {
                LocalTime.of(9, 0)
            }
            val dateTime = LocalDateTime.of(date, time).minusMinutes(minutesBefore.toLong())
            dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            Log.w("ReminderManager", "Failed to parse date/time: $dueDateStr $dueTimeStr", e)
            null
        }
    }
}
