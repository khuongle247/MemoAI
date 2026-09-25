package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RecurrenceHelper
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskStatus
import com.example.ui.components.TaskRowCard
import com.example.ui.theme.NoteColorHelper
import com.example.ui.theme.spacing
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

enum class CalendarPerspective {
    MONTHLY,
    WEEKLY
}

enum class AgendaFilter {
    ALL,
    TASKS_ONLY,
    NOTES_ONLY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    tasks: List<TaskEntity>,
    notes: List<NoteEntity>,
    onToggleTask: (TaskEntity) -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    onAddTaskForDate: (String) -> Unit,
    onOpenNote: (NoteEntity) -> Unit,
    onAddNoteForDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var perspective by remember { mutableStateOf(CalendarPerspective.MONTHLY) }
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentWeekStart by remember {
        mutableStateOf(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
    }
    var agendaFilter by remember { mutableStateOf(AgendaFilter.ALL) }
    var showFullWeekAgenda by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()
    val localeVi = remember { Locale.forLanguageTag("vi-VN") }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("'Tháng' MM, yyyy", localeVi) }
    val fullDateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", localeVi) }

    val selectedDateStr = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

    // Map tasks by date
    val taskDateMap = remember(tasks) {
        tasks.groupBy { it.dueDate }
    }

    // Map notes by creation date (YYYY-MM-DD)
    val noteDateMap = remember(notes) {
        notes.groupBy { note ->
            Instant.ofEpochMilli(note.createdAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DateTimeFormatter.ISO_LOCAL_DATE)
        }
    }

    // Tasks and notes for selected date
    val dayTasks = remember(tasks, selectedDateStr) {
        taskDateMap[selectedDateStr] ?: emptyList()
    }
    val dayNotes = remember(notes, selectedDateStr) {
        noteDateMap[selectedDateStr] ?: emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Lịch trình & Kế hoạch", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = if (perspective == CalendarPerspective.MONTHLY) "Góc nhìn theo Tháng" else "Góc nhìn theo Tuần",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    // Quick button to jump to today
                    TextButton(
                        onClick = {
                            val today = LocalDate.now()
                            selectedDate = today
                            currentYearMonth = YearMonth.from(today)
                            currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hôm nay", fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.testTag("calendar_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = MaterialTheme.spacing.large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            // View Mode Selector (Monthly vs Weekly)
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (perspective == CalendarPerspective.MONTHLY) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { perspective = CalendarPerspective.MONTHLY }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (perspective == CalendarPerspective.MONTHLY) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Xem theo Tháng",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (perspective == CalendarPerspective.MONTHLY) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (perspective == CalendarPerspective.WEEKLY) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    perspective = CalendarPerspective.WEEKLY
                                    currentWeekStart = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.ViewWeek,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (perspective == CalendarPerspective.WEEKLY) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Xem theo Tuần",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (perspective == CalendarPerspective.WEEKLY) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Perspectives rendering
            if (perspective == CalendarPerspective.MONTHLY) {
                // ==================== MONTHLY PERSPECTIVE ====================
                item {
                    // Month Navigation Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentYearMonth = currentYearMonth.minusMonths(1) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tháng trước")
                        }

                        Text(
                            text = currentYearMonth.format(monthFormatter).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(onClick = { currentYearMonth = currentYearMonth.plusMonths(1) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Tháng sau")
                        }
                    }
                }

                // Month Calendar Grid Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Weekday headers
                            val daysOfWeek = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
                            Row(modifier = Modifier.fillMaxWidth()) {
                                daysOfWeek.forEach { day ->
                                    Text(
                                        text = day,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Grid cells
                            val firstOfMonth = currentYearMonth.atDay(1)
                            val daysInMonth = currentYearMonth.lengthOfMonth()
                            val firstDayOfWeek = firstOfMonth.dayOfWeek.value // 1 = Mon, 7 = Sun
                            val leadingBlanks = firstDayOfWeek - 1

                            val totalCells = leadingBlanks + daysInMonth
                            val rows = (totalCells + 6) / 7

                            for (r in 0 until rows) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    for (c in 0 until 7) {
                                        val cellIndex = r * 7 + c
                                        val dayNum = cellIndex - leadingBlanks + 1

                                        if (dayNum in 1..daysInMonth) {
                                            val cellDate = currentYearMonth.atDay(dayNum)
                                            val dateIso = cellDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                            val isSelected = cellDate == selectedDate
                                            val isToday = cellDate == LocalDate.now()
                                            val taskCount = taskDateMap[dateIso]?.size ?: 0
                                            val noteCount = noteDateMap[dateIso]?.size ?: 0

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .padding(2.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        when {
                                                            isSelected -> MaterialTheme.colorScheme.primary
                                                            isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                                            else -> Color.Transparent
                                                        }
                                                    )
                                                    .clickable { selectedDate = cellDate },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = "$dayNum",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                        color = when {
                                                            isSelected -> Color.White
                                                            isToday -> MaterialTheme.colorScheme.primary
                                                            else -> MaterialTheme.colorScheme.onSurface
                                                        }
                                                    )

                                                    // Visual badges/dots for tasks and notes
                                                    if (taskCount > 0 || noteCount > 0) {
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(top = 2.dp)
                                                        ) {
                                                            if (taskCount > 0) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(5.dp)
                                                                        .clip(CircleShape)
                                                                        .background(if (isSelected) Color.White else MaterialTheme.colorScheme.primary)
                                                                )
                                                            }
                                                            if (noteCount > 0) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(5.dp)
                                                                        .clip(CircleShape)
                                                                        .background(if (isSelected) Color(0xFFFEF08A) else Color(0xFFD97706))
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ==================== WEEKLY PERSPECTIVE ====================
                item {
                    // Week Navigation Row
                    val weekEnd = currentWeekStart.plusDays(6)
                    val weekFormat = DateTimeFormatter.ofPattern("dd/MM", localeVi)
                    val weekYearFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy", localeVi)
                    val weekNumber = currentWeekStart.get(WeekFields.of(Locale.getDefault()).weekOfYear())

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentWeekStart = currentWeekStart.minusWeeks(1) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tuần trước")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Tuần $weekNumber (${currentWeekStart.format(weekFormat)} - ${weekEnd.format(weekYearFormat)})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Chạm vào ngày để xem chi tiết",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { currentWeekStart = currentWeekStart.plusWeeks(1) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Tuần sau")
                        }
                    }
                }

                // 7-day Weekly Strip Overview Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (i in 0 until 7) {
                                    val date = currentWeekStart.plusDays(i.toLong())
                                    val dateIso = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                    val isSelected = date == selectedDate
                                    val isToday = date == LocalDate.now()
                                    val dayName = when (date.dayOfWeek) {
                                        DayOfWeek.MONDAY -> "T2"
                                        DayOfWeek.TUESDAY -> "T3"
                                        DayOfWeek.WEDNESDAY -> "T4"
                                        DayOfWeek.THURSDAY -> "T5"
                                        DayOfWeek.FRIDAY -> "T6"
                                        DayOfWeek.SATURDAY -> "T7"
                                        DayOfWeek.SUNDAY -> "CN"
                                    }
                                    val taskCount = taskDateMap[dateIso]?.size ?: 0
                                    val noteCount = noteDateMap[dateIso]?.size ?: 0

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 2.dp)
                                            .clickable { selectedDate = date }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = dayName,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${date.dayOfMonth}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))

                                            // Task and note indicator chips
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (taskCount > 0) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(12.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = if (taskCount > 9) "9+" else "$taskCount",
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
                                                            )
                                                        }
                                                    }
                                                }
                                                if (noteCount > 0) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = if (isSelected) Color(0xFFFEF08A) else Color(0xFFD97706),
                                                        modifier = Modifier.size(12.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = if (noteCount > 9) "9+" else "$noteCount",
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isSelected) Color(0xFF78350F) else Color.White
                                                            )
                                                        }
                                                    }
                                                }
                                                if (taskCount == 0 && noteCount == 0) {
                                                    Box(modifier = Modifier.height(12.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Week View Toggle: Show entire week agenda or selected day
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showFullWeekAgenda) "Toàn bộ lịch trong tuần" else "Lịch ngày đã chọn",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        FilterChip(
                            selected = showFullWeekAgenda,
                            onClick = { showFullWeekAgenda = !showFullWeekAgenda },
                            label = { Text(if (showFullWeekAgenda) "Thu gọn về ngày" else "Xem cả tuần", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    if (showFullWeekAgenda) Icons.Default.FilterList else Icons.Default.ViewAgenda,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                }

                if (showFullWeekAgenda) {
                    // Render 7-day timeline breakdown
                    for (i in 0 until 7) {
                        val weekDay = currentWeekStart.plusDays(i.toLong())
                        val weekDayIso = weekDay.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        val tasksForDay = taskDateMap[weekDayIso] ?: emptyList()
                        val notesForDay = noteDateMap[weekDayIso] ?: emptyList()
                        val isDaySelected = weekDay == selectedDate

                        item(key = "weekday_$weekDayIso") {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDaySelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.surface
                                ),
                                border = if (isDaySelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedDate = weekDay
                                        showFullWeekAgenda = false
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = weekDay.format(DateTimeFormatter.ofPattern("EEEE, dd/MM", localeVi)).replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (weekDay == LocalDate.now()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )

                                        Text(
                                            text = "${tasksForDay.size} việc • ${notesForDay.size} ghi chú",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (tasksForDay.isEmpty() && notesForDay.isEmpty()) {
                                        Text(
                                            text = "Không có mục nào",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    } else {
                                        tasksForDay.take(3).forEach { t ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 4.dp)
                                            ) {
                                                Icon(
                                                    if (t.status == TaskStatus.COMPLETED) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                    contentDescription = null,
                                                    tint = if (t.status == TaskStatus.COMPLETED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = t.title,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        if (tasksForDay.size > 3) {
                                            Text(
                                                text = "+ còn ${tasksForDay.size - 3} công việc nữa",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }

                                        notesForDay.take(2).forEach { n ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 4.dp)
                                            ) {
                                                Text("📝", fontSize = 12.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = n.title,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==================== AGENDA FOR SELECTED DATE ====================
            if (!showFullWeekAgenda || perspective == CalendarPerspective.MONTHLY) {
                // Header with Date, counts, and Add buttons
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = selectedDate.format(fullDateFormatter).replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${dayTasks.size} công việc • ${dayNotes.size} ghi chú",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilledTonalButton(
                                    onClick = { onAddNoteForDate(selectedDateStr) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ghi chú", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { onAddTaskForDate(selectedDateStr) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Thêm việc", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Agenda Filter chips (All / Tasks / Notes)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = agendaFilter == AgendaFilter.ALL,
                                onClick = { agendaFilter = AgendaFilter.ALL },
                                label = { Text("Tất cả (${dayTasks.size + dayNotes.size})", fontSize = 12.sp) }
                            )
                            FilterChip(
                                selected = agendaFilter == AgendaFilter.TASKS_ONLY,
                                onClick = { agendaFilter = AgendaFilter.TASKS_ONLY },
                                label = { Text("Công việc (${dayTasks.size})", fontSize = 12.sp) }
                            )
                            FilterChip(
                                selected = agendaFilter == AgendaFilter.NOTES_ONLY,
                                onClick = { agendaFilter = AgendaFilter.NOTES_ONLY },
                                label = { Text("Ghi chú (${dayNotes.size})", fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // Empty state if nothing found
                val showTasks = agendaFilter == AgendaFilter.ALL || agendaFilter == AgendaFilter.TASKS_ONLY
                val showNotes = agendaFilter == AgendaFilter.ALL || agendaFilter == AgendaFilter.NOTES_ONLY
                val hasAnyItem = (showTasks && dayTasks.isNotEmpty()) || (showNotes && dayNotes.isNotEmpty())

                if (!hasAnyItem) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.EventAvailable,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Không có công việc hoặc ghi chú nào trong ngày này",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Nhấn nút 'Thêm việc' hoặc 'Ghi chú' để lên kế hoạch",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // Tasks List
                if (showTasks && dayTasks.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Checklist, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Công việc (${dayTasks.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    items(dayTasks, key = { "cal_task_${it.id}" }) { task ->
                        TaskRowCard(
                            task = task,
                            onToggle = { onToggleTask(task) },
                            onClick = { onEditTask(task) }
                        )
                    }
                }

                // Notes List
                if (showNotes && dayNotes.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            Text("📝", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ghi chú ngày này (${dayNotes.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    items(dayNotes, key = { "cal_note_${it.id}" }) { note ->
                        val noteStyle = NoteColorHelper.getNoteStyle(note.color, isDark)
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = noteStyle.containerColor),
                            border = androidx.compose.foundation.BorderStroke(
                                0.75.dp,
                                noteStyle.borderColor.copy(alpha = 0.5f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onOpenNote(note) }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = note.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = noteStyle.titleColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(note.category, fontSize = 10.5.sp) },
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = note.content,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 13.sp,
                                        lineHeight = 18.5.sp
                                    ),
                                    color = noteStyle.contentColor,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(90.dp))
            }
        }
    }
}
