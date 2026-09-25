package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskPriority
import com.example.data.local.entity.TaskStatus
import com.example.ui.theme.spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    tasks: List<TaskEntity>,
    notes: List<NoteEntity> = emptyList(),
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val total = tasks.size
    val completed = tasks.count { it.status == TaskStatus.COMPLETED }
    val pending = total - completed
    val todayStr = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }
    val overdue = tasks.count { it.dueDate < todayStr && it.status != TaskStatus.COMPLETED }
    val rate = if (total > 0) completed.toFloat() / total else 0f

    val categoryCounts = remember(tasks) {
        tasks.groupBy { it.category }.mapValues { it.value.size }
    }

    val priorityCounts = remember(tasks) {
        tasks.groupBy { it.priority }.mapValues { it.value.size }
    }

    // Productivity rating
    val (grade, gradeDesc, gradeColor) = when {
        total == 0 -> Triple("Mới", "Hãy bắt đầu tạo công việc đầu tiên!", MaterialTheme.colorScheme.primary)
        rate >= 0.8f -> Triple("Xuất sắc 🏆", "Bạn đang hoàn thành công việc rất kỷ luật và hiệu quả!", Color(0xFF10B981))
        rate >= 0.5f -> Triple("Khá tốt 👍", "Tiếp tục duy trì đà làm việc năng suất này nhé!", MaterialTheme.colorScheme.primary)
        else -> Triple("Cần cố gắng ⏳", "Còn nhiều công việc đang chờ bạn giải quyết.", Color(0xFFF97316))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Thống kê & Năng suất",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(MaterialTheme.spacing.minTouchTarget)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.testTag("statistics_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = MaterialTheme.spacing.large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large)
        ) {
            // Hero Performance Score Card
            item {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.large)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Đánh giá Hiệu suất",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                                Text(
                                    text = grade,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = gradeColor
                                )
                            }

                            // Circular Progress Indicator with center percentage
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { rate },
                                    modifier = Modifier.size(68.dp),
                                    strokeWidth = 7.dp,
                                    color = gradeColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Text(
                                    text = "${(rate * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                        Text(
                            text = gradeDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                        LinearProgressIndicator(
                            progress = { rate },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(MaterialTheme.shapes.extraSmall),
                            color = gradeColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            // 4 Grid Stats Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                ) {
                    MetricStatCard(
                        title = "Tổng công việc",
                        value = "$total",
                        subtitle = "Toàn bộ",
                        icon = Icons.Default.ListAlt,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "Đã hoàn thành",
                        value = "$completed",
                        subtitle = "${(rate * 100).toInt()}% thành công",
                        icon = Icons.Default.CheckCircle,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                ) {
                    MetricStatCard(
                        title = "Đang xử lý",
                        value = "$pending",
                        subtitle = "Chờ hoàn thành",
                        icon = Icons.Default.PendingActions,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "Quá hạn",
                        value = "$overdue",
                        subtitle = if (overdue > 0) "Cần xử lý gấp!" else "Không có việc trễ",
                        icon = Icons.Default.Warning,
                        tint = if (overdue > 0) Color(0xFFE11D48) else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Category Breakdown Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.large)
                    ) {
                        Text(
                            text = "Phân bổ theo Danh mục",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                        if (categoryCounts.isEmpty()) {
                            Text(
                                "Chưa có dữ liệu danh mục",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            categoryCounts.forEach { (cat, count) ->
                                val pct = if (total > 0) count.toFloat() / total else 0f
                                Column(modifier = Modifier.padding(vertical = MaterialTheme.spacing.extraSmall)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = cat,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "$count (${(pct * 100).toInt()}%)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                                    LinearProgressIndicator(
                                        progress = { pct },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(MaterialTheme.shapes.extraSmall),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Priority Breakdown Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.large)
                    ) {
                        Text(
                            text = "Phân bổ theo Mức độ Ưu tiên",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                        val priorities = listOf(
                            Triple(TaskPriority.URGENT, "Khẩn cấp", Color(0xFFE11D48)),
                            Triple(TaskPriority.HIGH, "Cao", Color(0xFFF97316)),
                            Triple(TaskPriority.MEDIUM, "Trung bình", Color(0xFFEAB308)),
                            Triple(TaskPriority.LOW, "Thấp", Color(0xFF10B981))
                        )

                        priorities.forEach { (prio, label, color) ->
                            val count = priorityCounts[prio] ?: 0
                            val pct = if (total > 0) count.toFloat() / total else 0f

                            Column(modifier = Modifier.padding(vertical = MaterialTheme.spacing.extraSmall)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                        )
                                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        text = "$count việc",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(MaterialTheme.shapes.extraSmall),
                                    color = color,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.huge))
            }
        }
    }
}

@Composable
fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.large)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
