package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.RecurrenceHelper
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskPriority
import com.example.data.local.entity.TaskStatus
import com.example.ui.theme.spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    tasks: List<TaskEntity>,
    onToggleTask: (TaskEntity) -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onAddNewTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Tất cả", "Hôm nay", "Sắp tới", "Lặp lại 🔄", "Đã xong", "Quá hạn")
    var selectedCategory by remember { mutableStateOf("Tất cả") }
    val categories = listOf("Tất cả", "Công việc", "Học tập", "Cá nhân", "Dự án", "Tài chính", "Ý tưởng")

    var selectedPriorityFilter by remember { mutableStateOf<TaskPriority?>(null) }

    val todayStr = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }

    val filteredTasks = remember(tasks, selectedTab, selectedCategory, selectedPriorityFilter, searchQuery) {
        tasks.filter { task ->
            val matchesTab = when (selectedTab) {
                0 -> true // All
                1 -> task.dueDate == todayStr // Today
                2 -> task.dueDate > todayStr && task.status != TaskStatus.COMPLETED // Upcoming
                3 -> task.repeatRule.isNotBlank() && task.repeatRule != RecurrenceHelper.NONE // Recurring
                4 -> task.status == TaskStatus.COMPLETED // Completed
                5 -> task.dueDate < todayStr && task.status != TaskStatus.COMPLETED // Overdue
                else -> true
            }
            val matchesCategory = if (selectedCategory == "Tất cả") true else task.category == selectedCategory
            val matchesPriority = if (selectedPriorityFilter == null) true else task.priority == selectedPriorityFilter
            val matchesSearch = if (searchQuery.isBlank()) true else {
                task.title.contains(searchQuery.trim(), ignoreCase = true) ||
                task.description.contains(searchQuery.trim(), ignoreCase = true)
            }
            matchesTab && matchesCategory && matchesPriority && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Quản lý Công việc",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                            Text(
                                text = "${tasks.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.extraSmall)
                            )
                        }
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = onAddNewTask,
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(
                            horizontal = MaterialTheme.spacing.medium,
                            vertical = MaterialTheme.spacing.extraSmall
                        ),
                        modifier = Modifier.testTag("tasks_add_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
                        Text(
                            text = "Thêm việc",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.testTag("tasks_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Tìm kiếm công việc...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Tìm kiếm",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Xóa tìm kiếm")
                        }
                    }
                },
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.spacing.large,
                        vertical = MaterialTheme.spacing.extraSmall
                    )
            )

            // Status Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = MaterialTheme.spacing.large,
                divider = {},
                containerColor = MaterialTheme.colorScheme.background
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

            // Category filter chips row
            LazyRow(
                contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.large),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

            // Priority filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.large),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedPriorityFilter == null,
                        onClick = { selectedPriorityFilter = null },
                        label = { Text("Mọi mức ưu tiên", style = MaterialTheme.typography.labelSmall) }
                    )
                }
                listOf(
                    TaskPriority.URGENT to "🔴 Khẩn cấp",
                    TaskPriority.HIGH to "🟠 Cao",
                    TaskPriority.MEDIUM to "🟡 Trung bình",
                    TaskPriority.LOW to "🟢 Thấp"
                ).forEach { (prio, label) ->
                    item {
                        FilterChip(
                            selected = selectedPriorityFilter == prio,
                            onClick = {
                                selectedPriorityFilter = if (selectedPriorityFilter == prio) null else prio
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            // Task list
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(MaterialTheme.spacing.huge),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventAvailable,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Không tìm thấy kết quả phù hợp"
                            else "Không có công việc nào trong mục này",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                        Text(
                            text = "Tạo công việc mới hoặc thử thay đổi bộ lọc",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
                        Button(
                            onClick = onAddNewTask,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                            Text("Tạo công việc ngay", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = MaterialTheme.spacing.large,
                        end = MaterialTheme.spacing.large,
                        top = MaterialTheme.spacing.extraSmall,
                        bottom = 90.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskCardItem(
                            task = task,
                            todayStr = todayStr,
                            onToggle = { onToggleTask(task) },
                            onClick = { onEditTask(task) },
                            onDelete = { onDeleteTask(task.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskCardItem(
    task: TaskEntity,
    todayStr: String,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isCompleted = task.status == TaskStatus.COMPLETED
    val isOverdue = task.dueDate < todayStr && !isCompleted

    val priorityColor = when (task.priority) {
        TaskPriority.URGENT -> Color(0xFFE11D48)
        TaskPriority.HIGH -> Color(0xFFF97316)
        TaskPriority.MEDIUM -> Color(0xFFEAB308)
        TaskPriority.LOW -> Color(0xFF10B981)
    }

    val animatedBg by animateColorAsState(
        targetValue = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(250),
        label = "taskCardBg"
    )

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = animatedBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 0.dp else 1.5.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isOverdue) Color(0xFFE11D48).copy(alpha = 0.4f)
            else if (isCompleted) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.spacing.large,
                    vertical = MaterialTheme.spacing.medium
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left priority indicator bar
            Box(
                modifier = Modifier
                    .width(MaterialTheme.spacing.extraSmall)
                    .height(40.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(if (isCompleted) priorityColor.copy(alpha = 0.4f) else priorityColor)
            )

            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))

            // Checkbox button with compact modern circle and comfortable touch target
            Box(
                modifier = Modifier
                    .size(MaterialTheme.spacing.minTouchTarget)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCompleted) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (isCompleted) MaterialTheme.colorScheme.primary else priorityColor.copy(alpha = 0.7f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Hoàn thành",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
                ) {
                    // Due Date Pill
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = if (isOverdue) Color(0xFFFFE4E6) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = MaterialTheme.spacing.small,
                                vertical = 2.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isOverdue) Icons.Default.Warning else Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = if (isOverdue) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
                            Text(
                                text = if (isOverdue) "Quá hạn: ${task.dueDate}" else task.dueDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOverdue) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    if (task.dueTime != null) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = task.dueTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(
                                    horizontal = MaterialTheme.spacing.small,
                                    vertical = 2.dp
                                )
                            )
                        }
                    }

                    // Category Pill
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = task.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                horizontal = MaterialTheme.spacing.small,
                                vertical = 2.dp
                            )
                        )
                    }

                    // Recurrence badge
                    if (task.repeatRule.isNotBlank() && task.repeatRule != RecurrenceHelper.NONE) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = MaterialTheme.spacing.small,
                                    vertical = 2.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Repeat,
                                    contentDescription = null,
                                    modifier = Modifier.size(11.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
                                Text(
                                    text = RecurrenceHelper.formatRuleLabel(task.repeatRule),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Delete action button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(MaterialTheme.spacing.minTouchTarget)
            ) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Xóa task",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
