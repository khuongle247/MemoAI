package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
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

    fun getSoundUri(): Uri {
        val savedUri = context.getSharedPreferences("memo_ai_prefs", Context.MODE_PRIVATE)
            .getString("sound_uri", null)
        return if (!savedUri.isNullOrBlank()) {
            try {
                Uri.parse(savedUri)
            } catch (_: Exception) {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }

    fun isSoundEnabled(): Boolean {
        return context.getSharedPreferences("memo_ai_prefs", Context.MODE_PRIVATE)
            .getBoolean("sound_enabled", true)
    }

    fun isVibrationEnabled(): Boolean {
        return context.getSharedPreferences("memo_ai_prefs", Context.MODE_PRIVATE)
            .getBoolean("vibration_enabled", true)
    }

    fun updateChannelSound() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            try {
                notificationManager.deleteNotificationChannel(CHANNEL_ID)
            } catch (_: Exception) {}
        }
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = getSoundUri()
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo nhắc nhở công việc và hạn chót kèm chuông báo"
                enableVibration(isVibrationEnabled())
                vibrationPattern = longArrayOf(0, 350, 200, 350)
                enableLights(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setBypassDnd(true)
                if (isSoundEnabled()) {
                    setSound(soundUri, audioAttributes)
                } else {
                    setSound(null, null)
                }
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTestNotification() {
        val soundUri = getSoundUri()
        val soundEnabled = isSoundEnabled()
        val vibrationEnabled = isVibrationEnabled()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Play notification sound
        if (soundEnabled) {
            try {
                val ringtone = RingtoneManager.getRingtone(context, soundUri)
                ringtone?.play()
            } catch (e: Exception) {
                Log.w("ReminderManager", "Failed to play sound directly: ${e.message}")
            }
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            9999,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("🔔 MemoAI: Kiểm tra thông báo & âm thanh")
            .setContentText("Chuông báo đang hoạt động hoàn hảo! ✨")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (soundEnabled) {
            builder.setSound(soundUri)
        }
        if (vibrationEnabled) {
            builder.setVibrate(longArrayOf(0, 350, 200, 350))
        }

        notificationManager.notify(9999, builder.build())
    }

    fun scheduleTaskReminder(task: TaskEntity) {
        var triggerMillis = calculateTriggerMillis(task.dueDate, task.dueTime, task.reminderMinutesBefore)
        if (triggerMillis == null) {
            Log.d("ReminderManager", "Trigger time cannot be calculated for task ${task.id}")
            return
        }

        val now = System.currentTimeMillis()
        if (triggerMillis <= now) {
            // If scheduled for current minute (within last 60 seconds), fire in 1.5 seconds so notification is delivered without delay
            if (now - triggerMillis <= 60_000L) {
                triggerMillis = now + 1_500L
            } else {
                Log.d("ReminderManager", "Trigger time is in the past: $triggerMillis (now: $now)")
                return
            }
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

        // Target intent for alarm clock interaction
        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("taskId", task.id)
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            (task.id + 50000).toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 1. Primary: setAlarmClock() guarantees exact-to-the-second delivery and bypasses Doze & batching
        try {
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d("ReminderManager", "Scheduled exact AlarmClock for '${task.title}' at $triggerMillis")
            return
        } catch (e: Exception) {
            Log.w("ReminderManager", "setAlarmClock failed, falling back to setExactAndAllowWhileIdle: ${e.message}")
        }

        // 2. Fallback: setExactAndAllowWhileIdle
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
        try {
            val showIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("taskId", taskId)
            }
            val showPendingIntent = PendingIntent.getActivity(
                context,
                (taskId + 50000).toInt(),
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (_: Exception) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
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
