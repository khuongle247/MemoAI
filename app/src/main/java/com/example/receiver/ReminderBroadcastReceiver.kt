package com.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.MemoApplication
import com.example.data.local.entity.TaskStatus
import com.example.notification.ReminderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val taskId = intent.getLongExtra(ReminderManager.EXTRA_TASK_ID, -1L)
        val title = intent.getStringExtra(ReminderManager.EXTRA_TASK_TITLE) ?: "Công việc đến hạn"
        val time = intent.getStringExtra(ReminderManager.EXTRA_TASK_TIME) ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (action) {
            ReminderManager.ACTION_REMINDER -> {
                showNotification(context, taskId, title, time)
            }

            ReminderManager.ACTION_COMPLETE -> {
                notificationManager.cancel(taskId.toInt())
                val app = context.applicationContext as? MemoApplication
                if (app != null && taskId != -1L) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            app.taskRepository.toggleTaskComplete(taskId, TaskStatus.TODO)
                        } catch (e: Exception) {
                            Log.e("ReminderReceiver", "Error completing task from notification", e)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }

            ReminderManager.ACTION_SNOOZE -> {
                notificationManager.cancel(taskId.toInt())
                val app = context.applicationContext as? MemoApplication
                if (app != null && taskId != -1L) {
                    app.reminderManager.snoozeTask(taskId, title, 10)
                }
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                val app = context.applicationContext as? MemoApplication
                if (app != null) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val tasks = app.taskRepository.getAllTasksSnapshot()
                            val pending = tasks.filter { it.status != TaskStatus.COMPLETED }
                            pending.forEach { task ->
                                app.reminderManager.scheduleTaskReminder(task)
                            }
                        } catch (e: Exception) {
                            Log.e("ReminderReceiver", "Error rescheduling alarms on boot", e)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }

    private fun showNotification(context: Context, taskId: Long, title: String, time: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to open app directly to the task
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = Uri.parse("memoai://task?id=$taskId")
            putExtra("taskId", taskId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Complete action button
        val completeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderManager.ACTION_COMPLETE
            putExtra(ReminderManager.EXTRA_TASK_ID, taskId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 10 + 1).toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze action button (10 minutes)
        val snoozeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderManager.ACTION_SNOOZE
            putExtra(ReminderManager.EXTRA_TASK_ID, taskId)
            putExtra(ReminderManager.EXTRA_TASK_TITLE, title)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 10 + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeText = if (time.isNotBlank()) " lúc $time" else ""
        val notification = NotificationCompat.Builder(context, ReminderManager.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("🔔 $title")
            .setContentText("Đã đến thời gian thực hiện công việc$timeText.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Hoàn thành", completePendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "Hoãn 10 phút", snoozePendingIntent)
            .build()

        notificationManager.notify(taskId.toInt(), notification)
    }
}
