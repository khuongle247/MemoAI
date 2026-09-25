package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ai.AIIntentResult
import com.example.ai.ExtractedTask
import com.example.ai.IntentType
import com.example.ai.NoteSummaryResult
import com.example.ai.ParsedTaskData
import com.example.ai.VoiceState
import com.example.data.RecurrenceHelper
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskPriority
import com.example.data.local.entity.TaskStatus
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun VoiceCaptureModal(
    voiceState: VoiceState,
    partialText: String,
    isAiProcessing: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSubmitPrompt: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var manualInputText by remember { mutableStateOf("") }
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    // Handle voice success automatically
    LaunchedEffect(voiceState) {
        if (voiceState is VoiceState.Success) {
            onSubmitPrompt(voiceState.recognizedText)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("voice_capture_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Sparkle",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Thu âm & Nhập liệu bằng AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Microphone pulsating button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(110.dp)
                ) {
                    if (voiceState is VoiceState.Listening) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .scale(scale)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        )
                    }

                    FilledIconButton(
                        onClick = {
                            if (voiceState is VoiceState.Listening) onStopListening() else onStartListening()
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (voiceState is VoiceState.Listening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .size(76.dp)
                            .testTag("voice_mic_button")
                    ) {
                        Icon(
                            imageVector = if (voiceState is VoiceState.Listening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Microphone",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status text
                when {
                    isAiProcessing -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI đang phân tích ý định...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    voiceState is VoiceState.Listening -> {
                        Text(
                            text = if (partialText.isNotBlank()) partialText else "🎙 Đang lắng nghe bạn nói...",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                    voiceState is VoiceState.Processing -> {
                        Text(
                            text = "Đang chuyển giọng nói thành văn bản...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    voiceState is VoiceState.Error -> {
                        Text(
                            text = (voiceState as VoiceState.Error).message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> {
                        Text(
                            text = "Nhấn mic để nói hoặc nhập văn bản bên dưới",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Examples helper
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Ví dụ câu lệnh:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "• \"Ngày mai 8 giờ sáng nhắc tôi nộp báo cáo\"\n• \"Ghi chú mật khẩu WiFi nhà mới là 123456\"\n• \"Chiều mai 3 giờ gọi cho mẹ\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Manual text input fallback
                OutlinedTextField(
                    value = manualInputText,
                    onValueChange = { manualInputText = it },
                    placeholder = { Text("Hoặc nhập câu lệnh tự nhiên...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_manual_input_field"),
                    shape = RoundedCornerShape(14.dp),
                    trailingIcon = {
                        if (manualInputText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val text = manualInputText
                                    manualInputText = ""
                                    onSubmitPrompt(text)
                                },
                                modifier = Modifier.testTag("voice_manual_submit_button")
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Gửi")
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AIConfirmationDialog(
    result: AIIntentResult,
    onConfirm: (AIIntentResult) -> Unit,
    onEditManually: (AIIntentResult) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("ai_confirmation_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AI đã hiểu yêu cầu của bạn:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = result.explanation.ifEmpty { "Độ tin cậy: ${(result.confidence * 100).toInt()}%" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detail preview
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        when (result.intent) {
                            IntentType.CREATE_TASK -> {
                                val task = result.taskData
                                if (task != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "📌",
                                            fontSize = 20.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Ngày: ${task.date}", style = MaterialTheme.typography.bodyMedium)
                                    }

                                    if (task.time != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Giờ: ${task.time}", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val remText = if (task.reminderMinutesBefore == 0) "Đúng giờ hẹn" else "Trước ${task.reminderMinutesBefore} phút"
                                        Text("Nhắc nhở: $remText", style = MaterialTheme.typography.bodyMedium)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⚡", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val prioLabel = when (task.priority) {
                                            TaskPriority.URGENT -> "Khẩn cấp"
                                            TaskPriority.HIGH -> "Cao"
                                            TaskPriority.MEDIUM -> "Trung bình"
                                            TaskPriority.LOW -> "Thấp"
                                        }
                                        Text("Ưu tiên: $prioLabel | Nhóm: ${task.category}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }

                            IntentType.CREATE_NOTE -> {
                                val note = result.noteData
                                if (note != null) {
                                    Text(
                                        text = "📝 ${note.title}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = note.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "🏷 Nhóm: ${note.category}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            else -> {
                                Text(text = result.explanation, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onEditManually(result) }) {
                        Text("Chỉnh sửa")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(result) },
                        modifier = Modifier.testTag("ai_confirm_create_button")
                    ) {
                        Text(if (result.intent == IntentType.CREATE_TASK) "Tạo công việc" else "Lưu ghi chú")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditDialog(
    taskToEdit: TaskEntity? = null,
    defaultDate: String? = null,
    onSave: (TaskEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val initialDate = taskToEdit?.dueDate ?: defaultDate ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var description by remember { mutableStateOf(taskToEdit?.description ?: "") }
    var date by remember { mutableStateOf(initialDate) }
    var time by remember { mutableStateOf(taskToEdit?.dueTime ?: "09:00") }
    var hasTime by remember { mutableStateOf(taskToEdit?.dueTime != null) }
    var reminderOffset by remember { mutableIntStateOf(taskToEdit?.reminderMinutesBefore ?: 15) }
    var priority by remember { mutableStateOf(taskToEdit?.priority ?: TaskPriority.MEDIUM) }
    var category by remember { mutableStateOf(taskToEdit?.category ?: "Công việc") }
    var repeatRule by remember { mutableStateOf(taskToEdit?.repeatRule ?: RecurrenceHelper.NONE) }
    var isCustomRecurrence by remember {
        mutableStateOf(taskToEdit?.repeatRule?.startsWith("CUSTOM:") == true)
    }
    var customCount by remember {
        val count = if (taskToEdit?.repeatRule?.startsWith("CUSTOM:") == true) {
            taskToEdit.repeatRule.split(":").getOrNull(1) ?: "2"
        } else "2"
        mutableStateOf(count)
    }
    var customUnit by remember {
        val unit = if (taskToEdit?.repeatRule?.startsWith("CUSTOM:") == true) {
            taskToEdit.repeatRule.split(":").getOrNull(2) ?: "DAYS"
        } else "DAYS"
        mutableStateOf(unit)
    }

    val categories = listOf("Công việc", "Học tập", "Cá nhân", "Dự án", "Tài chính", "Ý tưởng", "Khác")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("task_edit_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (taskToEdit == null) "Tạo công việc mới" else "Chỉnh sửa công việc",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tiêu đề công việc *") },
                    placeholder = { Text("Ví dụ: Học Spring Boot") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả / Ghi chú thêm") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Date and Time Row
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Ngày (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Giờ (HH:mm)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Priority Selection
                Text("Mức độ ưu tiên:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TaskPriority.values().forEach { p ->
                        val pLabel = when (p) {
                            TaskPriority.URGENT -> "Khẩn cấp"
                            TaskPriority.HIGH -> "Cao"
                            TaskPriority.MEDIUM -> "Vừa"
                            TaskPriority.LOW -> "Thấp"
                        }
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(pLabel, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Reminder Offset
                Text("Nhắc nhở trước:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0 to "Đúng giờ", 15 to "15p", 30 to "30p", 60 to "1h").forEach { (offset, label) ->
                        FilterChip(
                            selected = reminderOffset == offset,
                            onClick = { reminderOffset = offset },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category
                Text("Danh mục:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                ScrollableTabRow(
                    selectedTabIndex = categories.indexOf(category).coerceAtLeast(0),
                    edgePadding = 0.dp,
                    divider = {},
                    indicator = {}
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Recurrence Settings
                Text("Lặp lại định kỳ:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        RecurrenceHelper.NONE to "Không",
                        RecurrenceHelper.DAILY to "Hàng ngày",
                        RecurrenceHelper.WEEKDAYS to "T2-T6",
                        RecurrenceHelper.WEEKLY to "Hàng tuần",
                        RecurrenceHelper.MONTHLY to "Hàng tháng"
                    )
                    presets.forEach { (rule, label) ->
                        FilterChip(
                            selected = !isCustomRecurrence && repeatRule == rule,
                            onClick = {
                                isCustomRecurrence = false
                                repeatRule = rule
                            },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                FilterChip(
                    selected = isCustomRecurrence,
                    onClick = {
                        isCustomRecurrence = true
                        val count = customCount.ifEmpty { "2" }
                        repeatRule = "CUSTOM:$count:$customUnit"
                    },
                    label = { Text("Tùy chỉnh khoảng cách...", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                )

                if (isCustomRecurrence) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Cấu hình chu kỳ lặp lại tùy chỉnh:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Mỗi", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = customCount,
                                    onValueChange = { newVal ->
                                        val digits = newVal.filter { it.isDigit() }.take(2)
                                        customCount = digits
                                        val count = digits.ifEmpty { "1" }
                                        repeatRule = "CUSTOM:$count:$customUnit"
                                    },
                                    modifier = Modifier.width(64.dp),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                listOf("DAYS" to "Ngày", "WEEKS" to "Tuần", "MONTHS" to "Tháng").forEach { (unit, unitLabel) ->
                                    FilterChip(
                                        selected = customUnit == unit,
                                        onClick = {
                                            customUnit = unit
                                            val count = customCount.ifEmpty { "1" }
                                            repeatRule = "CUSTOM:$count:$unit"
                                        },
                                        label = { Text(unitLabel, fontSize = 11.sp) },
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (repeatRule != RecurrenceHelper.NONE && repeatRule.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🔄 Sẽ tự động lên lịch mới: ${RecurrenceHelper.formatRuleLabel(repeatRule)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Hủy")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val newTask = (taskToEdit ?: TaskEntity(title = "", dueDate = date)).copy(
                                    title = title.trim(),
                                    description = description.trim(),
                                    dueDate = date.trim(),
                                    dueTime = time.trim().ifEmpty { null },
                                    reminderMinutesBefore = reminderOffset,
                                    priority = priority,
                                    category = category,
                                    repeatRule = repeatRule
                                )
                                onSave(newTask)
                            }
                        },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.testTag("save_task_button")
                    ) {
                        Text("Lưu công việc")
                    }
                }
            }
        }
    }
}

@Composable
fun NoteEditDialog(
    noteToEdit: NoteEntity? = null,
    onSave: (NoteEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(noteToEdit?.title ?: "") }
    var content by remember { mutableStateOf(noteToEdit?.content ?: "") }
    var category by remember { mutableStateOf(noteToEdit?.category ?: "Cá nhân") }
    var tags by remember { mutableStateOf(noteToEdit?.tags ?: "") }

    val categories = listOf("Cá nhân", "Công việc", "Học tập", "Dự án", "Tài chính", "Ý tưởng", "Khác")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("note_edit_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (noteToEdit == null) "Tạo ghi chú mới" else "Chỉnh sửa ghi chú",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tiêu đề ghi chú *") },
                    placeholder = { Text("Ví dụ: Mật khẩu WiFi mới") },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Nội dung ghi chú *") },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .testTag("note_content_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Thẻ (cách nhau bởi dấu phẩy)") },
                    placeholder = { Text("meeting, project, ideas") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Danh mục:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                ScrollableTabRow(
                    selectedTabIndex = categories.indexOf(category).coerceAtLeast(0),
                    edgePadding = 0.dp,
                    divider = {},
                    indicator = {}
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Hủy")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val newNote = (noteToEdit ?: NoteEntity(title = "", content = "")).copy(
                                    title = title.trim(),
                                    content = content.trim(),
                                    category = category,
                                    tags = tags.trim()
                                )
                                onSave(newNote)
                            }
                        },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.testTag("save_note_button")
                    ) {
                        Text("Lưu ghi chú")
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryResultDialog(
    noteTitle: String,
    summaryResult: NoteSummaryResult,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Tóm tắt: $noteTitle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Tóm tắt nội dung:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = summaryResult.summary, style = MaterialTheme.typography.bodyMedium)

                if (summaryResult.keyPoints.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Ý chính (Key Points):", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    summaryResult.keyPoints.forEach { point ->
                        Text("• $point", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (summaryResult.actionItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Hành động cần làm (Action Items):", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    summaryResult.actionItems.forEach { item ->
                        Text("☐ $item", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onDismiss) {
                        Text("Đóng")
                    }
                }
            }
        }
    }
}

@Composable
fun ExtractTasksDialog(
    noteTitle: String,
    tasks: List<ExtractedTask>,
    onCreateTasks: (List<ExtractedTask>) -> Unit,
    onDismiss: () -> Unit
) {
    val items = remember { mutableStateListOf<ExtractedTask>().apply { addAll(tasks) } }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlaylistAddCheck, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trích xuất công việc", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "AI phát hiện các việc cần làm từ ghi chú \"$noteTitle\":",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                items.forEachIndexed { index, task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable {
                                items[index] = task.copy(isSelected = !task.isSelected)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = task.isSelected,
                            onCheckedChange = { checked ->
                                items[index] = task.copy(isSelected = checked)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(task.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Hạn: ${task.dueDate} ${task.dueTime ?: ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Bỏ qua")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onCreateTasks(items.toList()) },
                        enabled = items.any { it.isSelected }
                    ) {
                        Text("Tạo ${items.count { it.isSelected }} công việc")
                    }
                }
            }
        }
    }
}
