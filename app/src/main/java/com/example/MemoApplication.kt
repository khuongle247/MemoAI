package com.example

import android.app.Application
import com.example.ai.AIProvider
import com.example.ai.GeminiAIProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskPriority
import com.example.data.local.entity.TaskStatus
import com.example.data.repository.NoteRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TaskRepository
import com.example.notification.ReminderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MemoApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val taskRepository by lazy { TaskRepository(database.taskDao()) }
    val noteRepository by lazy { NoteRepository(database.noteDao()) }
    val settingsRepository by lazy { SettingsRepository(this) }
    val reminderManager by lazy { ReminderManager(this) }
    val aiProvider: AIProvider by lazy { GeminiAIProvider() }

    override fun onCreate() {
        super.onCreate()
        seedInitialDataIfEmpty()
    }

    private fun seedInitialDataIfEmpty() {
        CoroutineScope(Dispatchers.IO).launch {
            val existingTasks = taskRepository.getAllTasksSnapshot()
            if (existingTasks.isEmpty()) {
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)

                taskRepository.insertTask(
                    TaskEntity(
                        title = "Nộp báo cáo môn Java",
                        description = "Gửi file PDF và link GitHub qua hệ thống trường học",
                        status = TaskStatus.TODO,
                        priority = TaskPriority.HIGH,
                        dueDate = today,
                        dueTime = "08:00",
                        reminderMinutesBefore = 0,
                        category = "Học tập"
                    )
                )

                taskRepository.insertTask(
                    TaskEntity(
                        title = "Họp nhóm đồ án tốt nghiệp",
                        description = "Thảo luận kiến trúc hệ thống và phân chia API backend",
                        status = TaskStatus.TODO,
                        priority = TaskPriority.MEDIUM,
                        dueDate = today,
                        dueTime = "14:00",
                        reminderMinutesBefore = 15,
                        category = "Dự án"
                    )
                )

                taskRepository.insertTask(
                    TaskEntity(
                        title = "Học Spring Boot",
                        description = "Tìm hiểu Spring Security, JWT và JPA Repository",
                        status = TaskStatus.TODO,
                        priority = TaskPriority.MEDIUM,
                        dueDate = today,
                        dueTime = "19:00",
                        reminderMinutesBefore = 15,
                        category = "Học tập"
                    )
                )

                taskRepository.insertTask(
                    TaskEntity(
                        title = "Làm bài tập Android",
                        description = "Xây dựng Compose UI và kết nối Room Database",
                        status = TaskStatus.TODO,
                        priority = TaskPriority.LOW,
                        dueDate = tomorrow,
                        dueTime = "10:00",
                        category = "Học tập"
                    )
                )

                taskRepository.insertTask(
                    TaskEntity(
                        title = "Đọc tài liệu Spring Cloud & Microservices",
                        description = "Chuẩn bị cho buổi phỏng vấn tuần tới",
                        status = TaskStatus.TODO,
                        priority = TaskPriority.LOW,
                        dueDate = tomorrow,
                        dueTime = "15:30",
                        category = "Công việc"
                    )
                )
            }

            val existingNotes = noteRepository.getAllNotesSnapshot()
            if (existingNotes.isEmpty()) {
                noteRepository.insertNote(
                    NoteEntity(
                        title = "Mật khẩu WiFi & Địa chỉ nhà mới",
                        content = "Tên WiFi: HomeSweetHome_5G\nMật khẩu: Saigon@2026Secure!\nĐịa chỉ: Tòa Park 3, Căn hộ 12B, Khu đô thị Sala.",
                        category = "Cá nhân",
                        tags = "wifi,nhà ở,cá nhân",
                        isPinned = true,
                        color = 0xFFEDE9FE // Soft Purple
                    )
                )

                noteRepository.insertNote(
                    NoteEntity(
                        title = "Cuộc họp định hướng dự án MemoAI",
                        content = "Cuộc họp hôm nay: ngày mai gửi tài liệu cho anh Nam, thứ sáu hoàn thành API login, tuần sau họp lại với team.",
                        category = "Công việc",
                        tags = "họp,memoai,công việc",
                        isPinned = false,
                        color = 0xFFE0F2FE // Soft Sky
                    )
                )
            }
        }
    }
}
