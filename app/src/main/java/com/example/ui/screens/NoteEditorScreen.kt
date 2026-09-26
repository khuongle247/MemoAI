package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.NoteEntity
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Data models for vector drawing
data class DrawingPoint(val x: Float, val y: Float)

data class DrawingStroke(
    val points: List<DrawingPoint>,
    val colorArgb: Long,
    val strokeWidth: Float,
    val isHighlighter: Boolean = false
)

object NoteDrawingSerializer {
    fun serialize(strokes: List<DrawingStroke>): String {
        if (strokes.isEmpty()) return ""
        val jsonArray = JSONArray()
        strokes.forEach { stroke ->
            val obj = JSONObject()
            obj.put("color", stroke.colorArgb)
            obj.put("width", stroke.strokeWidth.toDouble())
            obj.put("hl", stroke.isHighlighter)
            val ptsArr = JSONArray()
            stroke.points.forEach { pt ->
                val ptObj = JSONObject()
                ptObj.put("x", pt.x.toDouble())
                ptObj.put("y", pt.y.toDouble())
                ptsArr.put(ptObj)
            }
            obj.put("pts", ptsArr)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    fun deserialize(jsonStr: String): List<DrawingStroke> {
        if (jsonStr.isBlank()) return emptyList()
        val list = mutableListOf<DrawingStroke>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val color = obj.optLong("color", 0xFF1E293BL)
                val width = obj.optDouble("width", 6.0).toFloat()
                val hl = obj.optBoolean("hl", false)
                val ptsArr = obj.optJSONArray("pts") ?: JSONArray()
                val pts = mutableListOf<DrawingPoint>()
                for (j in 0 until ptsArr.length()) {
                    val ptObj = ptsArr.getJSONObject(j)
                    pts.add(DrawingPoint(ptObj.getDouble("x").toFloat(), ptObj.getDouble("y").toFloat()))
                }
                if (pts.isNotEmpty()) {
                    list.add(DrawingStroke(pts, color, width, hl))
                }
            }
        } catch (_: Exception) {}
        return list
    }
}

object NoteTableSerializer {
    fun serialize(rows: List<List<String>>): String {
        if (rows.isEmpty()) return ""
        val jsonArray = JSONArray()
        rows.forEach { row ->
            val rowArr = JSONArray()
            row.forEach { cell -> rowArr.put(cell) }
            jsonArray.put(rowArr)
        }
        return jsonArray.toString()
    }

    fun deserialize(jsonStr: String): List<List<String>> {
        if (jsonStr.isBlank()) return emptyList()
        val list = mutableListOf<List<String>>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val rowArr = jsonArray.getJSONArray(i)
                val row = mutableListOf<String>()
                for (j in 0 until rowArr.length()) {
                    row.add(rowArr.optString(j, ""))
                }
                list.add(row)
            }
        } catch (_: Exception) {}
        return list
    }
}

private val NOTE_BACKGROUND_PALETTE = listOf(
    0xFFFFFFFFL to "Mặc định (Trắng)",
    0xFFFEF3C7L to "Vàng nhạt",
    0xFFDCFCE7L to "Xanh Mint",
    0xFFE0F2FEL to "Xanh da trời",
    0xFFEDE9FEL to "Tím Lavender",
    0xFFFCE7F3L to "Hồng phấn"
)

private val NOTE_CATEGORIES = listOf(
    "Cá nhân",
    "Ghi chú viết tay",
    "Công việc",
    "Học tập",
    "Dự án",
    "Tài chính",
    "Ý tưởng",
    "Khác"
)

enum class DrawingToolMode {
    PEN, HIGHLIGHTER, ERASER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    initialNote: NoteEntity?,
    onSave: (NoteEntity) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit,
    onSummarize: (NoteEntity) -> Unit,
    onExtractTasks: (NoteEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var content by remember { mutableStateOf(initialNote?.content ?: "") }
    var category by remember { mutableStateOf(initialNote?.category ?: "Cá nhân") }
    var tags by remember { mutableStateOf(initialNote?.tags ?: "") }
    var isPinned by remember { mutableStateOf(initialNote?.isPinned ?: false) }
    var noteColor by remember { mutableStateOf(initialNote?.color ?: 0xFFFFFFFFL) }

    // Table state
    var tableRows by remember {
        mutableStateOf(
            if (!initialNote?.tableData.isNullOrBlank()) {
                NoteTableSerializer.deserialize(initialNote!!.tableData)
            } else {
                emptyList()
            }
        )
    }

    // Drawing state
    var drawingStrokes by remember {
        mutableStateOf(
            if (!initialNote?.drawingData.isNullOrBlank()) {
                NoteDrawingSerializer.deserialize(initialNote!!.drawingData)
            } else {
                emptyList()
            }
        )
    }

    // Inline Drawing Mode states (Drawing directly under text like native ColorOS/Xiaomi Notes)
    var isDrawingMode by remember { mutableStateOf(false) }
    var drawingTool by remember { mutableStateOf(DrawingToolMode.PEN) }
    var drawingColor by remember { mutableStateOf(0xFF1E293BL) }
    var drawingStrokeWidth by remember { mutableStateOf(6f) }

    var showMoreMenu by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }

    // Auto-save logic
    fun saveCurrentNote(): NoteEntity {
        val serializedTable = NoteTableSerializer.serialize(tableRows)
        val serializedDrawing = NoteDrawingSerializer.serialize(drawingStrokes)

        val updated = (initialNote ?: NoteEntity(title = "", content = "")).copy(
            title = title.trim(),
            content = content.trim(),
            category = category,
            tags = tags.trim(),
            isPinned = isPinned,
            color = noteColor,
            tableData = serializedTable,
            drawingData = serializedDrawing,
            updatedAt = System.currentTimeMillis()
        )
        onSave(updated)
        return updated
    }

    BackHandler {
        if (isDrawingMode) {
            isDrawingMode = false
        } else {
            saveCurrentNote()
            onBack()
        }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Formatted date and time
    val formattedTimestamp = remember(initialNote?.updatedAt) {
        val millis = initialNote?.updatedAt ?: System.currentTimeMillis()
        val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
        dt.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
    }

    val totalCharCount = remember(title, content, tableRows) {
        title.length + content.length + tableRows.sumOf { row -> row.sumOf { it.length } }
    }

    // Background color resolution
    val defaultSurface = MaterialTheme.colorScheme.surface
    val currentBgColor = remember(noteColor, isDark, defaultSurface) {
        if (isDark) {
            defaultSurface
        } else {
            if (noteColor == 0xFFFFFFFFL) Color.White else Color(noteColor)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("note_editor_screen"),
        containerColor = currentBgColor,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (isDrawingMode) {
                            isDrawingMode = false
                        } else {
                            saveCurrentNote()
                            onBack()
                        }
                    },
                    modifier = Modifier.testTag("note_editor_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Share icon
                    IconButton(
                        onClick = {
                            val saved = saveCurrentNote()
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, saved.title)
                                val shareText = buildString {
                                    if (saved.title.isNotBlank()) appendLine(saved.title)
                                    if (saved.content.isNotBlank()) appendLine(saved.content)
                                    if (tableRows.isNotEmpty()) {
                                        appendLine("\n[Bảng dữ liệu]:")
                                        tableRows.forEach { r -> appendLine(r.joinToString(" | ")) }
                                    }
                                }
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ ghi chú"))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Chia sẻ",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Overflow Menu (with subtle accent dot matching native UI)
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Box {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Tùy chọn khác",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .align(Alignment.TopEnd)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isPinned) "Bỏ ghim ghi chú" else "Ghim ghi chú") },
                                leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) },
                                onClick = {
                                    isPinned = !isPinned
                                    showMoreMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Đổi danh mục (${category})") },
                                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    showCategoryDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Đổi màu giấy note") },
                                leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    showColorDialog = true
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("✨ Tóm tắt bằng AI") },
                                leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    val note = saveCurrentNote()
                                    onSummarize(note)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("⚡ Trích xuất công việc") },
                                leadingIcon = { Icon(Icons.Default.PlaylistAddCheck, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    val note = saveCurrentNote()
                                    onExtractTasks(note)
                                }
                            )
                            if (initialNote != null && initialNote.id != 0L) {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Xóa ghi chú", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMoreMenu = false
                                        onDelete(initialNote.id)
                                        onBack()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (isDrawingMode) {
                // Interactive Drawing Palette directly docked at bottom
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        // Row 1: Brush mode, Undo, Clear, Done
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = drawingTool == DrawingToolMode.PEN,
                                    onClick = { drawingTool = DrawingToolMode.PEN },
                                    label = { Text("🖊️ Bút mực", fontSize = 12.sp) }
                                )
                                FilterChip(
                                    selected = drawingTool == DrawingToolMode.HIGHLIGHTER,
                                    onClick = { drawingTool = DrawingToolMode.HIGHLIGHTER },
                                    label = { Text("🖍️ Dạ quang", fontSize = 12.sp) }
                                )
                                FilterChip(
                                    selected = drawingTool == DrawingToolMode.ERASER,
                                    onClick = { drawingTool = DrawingToolMode.ERASER },
                                    label = { Text("🧹 Tẩy", fontSize = 12.sp) }
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (drawingStrokes.isNotEmpty()) {
                                            drawingStrokes = drawingStrokes.dropLast(1)
                                        }
                                    },
                                    enabled = drawingStrokes.isNotEmpty(),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = "Hoàn tác",
                                        tint = if (drawingStrokes.isNotEmpty()) MaterialTheme.colorScheme.onSurface else Color.LightGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { drawingStrokes = emptyList() },
                                    enabled = drawingStrokes.isNotEmpty(),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "Xóa tất cả",
                                        tint = if (drawingStrokes.isNotEmpty()) MaterialTheme.colorScheme.error else Color.LightGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Button(
                                    onClick = { isDrawingMode = false },
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Xong", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Row 2: Stroke width & Colors (if not eraser)
                        if (drawingTool != DrawingToolMode.ERASER) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Width chips
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(3f to "Mảnh", 6f to "Vừa", 12f to "Đậm").forEach { (w, label) ->
                                        FilterChip(
                                            selected = drawingStrokeWidth == w,
                                            onClick = { drawingStrokeWidth = w },
                                            label = { Text(label, fontSize = 10.sp) }
                                        )
                                    }
                                }

                                // Colors
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(
                                        0xFF1E293BL, // Slate Black
                                        0xFF2563EBL, // Blue
                                        0xFFDC2626L, // Red
                                        0xFF16A34AL, // Green
                                        0xFFEA580CL, // Orange
                                        0xFF9333EAL  // Purple
                                    ).forEach { c ->
                                        val isSelected = drawingColor == c
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(Color(c))
                                                .border(
                                                    width = if (isSelected) 2.5.dp else 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                    shape = CircleShape
                                                )
                                                .clickable { drawingColor = c },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(
                    color = currentBgColor,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tool 1: Checklist toggle / add
                        IconButton(
                            onClick = {
                                content = if (content.isEmpty()) "☐ " else "$content\n☐ "
                            },
                            modifier = Modifier.testTag("tool_checklist")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircleOutline,
                                contentDescription = "Tạo danh sách công việc",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // Tool 2: Handwriting Drawing Mode toggle
                        IconButton(
                            onClick = {
                                isDrawingMode = true
                                if (category == "Cá nhân" && drawingStrokes.isEmpty()) {
                                    category = "Ghi chú viết tay"
                                }
                            },
                            modifier = Modifier.testTag("tool_drawing")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gesture,
                                contentDescription = "Vẽ bằng tay",
                                tint = if (drawingStrokes.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // Tool 3: Table Creator
                        IconButton(
                            onClick = {
                                if (tableRows.isEmpty()) {
                                    tableRows = listOf(
                                        listOf("Cột 1", "Cột 2", "Cột 3"),
                                        listOf("", "", ""),
                                        listOf("", "", "")
                                    )
                                } else {
                                    val colCount = tableRows.firstOrNull()?.size ?: 3
                                    val newRow = List(colCount) { "" }
                                    tableRows = tableRows + listOf(newRow)
                                }
                            },
                            modifier = Modifier.testTag("tool_table")
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = "Tạo bảng dữ liệu",
                                tint = if (tableRows.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // Tool 4: Add more features (+)
                        IconButton(
                            onClick = { showColorDialog = true },
                            modifier = Modifier.testTag("tool_more")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircleOutline,
                                contentDescription = "Mở rộng tiện ích",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
                .verticalScroll(rememberScrollState(), enabled = !isDrawingMode)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Metadata Sub-header (Exact match to screenshot: "19:23 24/9/2026 | 84 | Ghi chú viết tay")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$formattedTimestamp  |  $totalCharCount  |  $category",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    ),
                    modifier = Modifier.clickable { showCategoryDialog = true }
                )
            }

            // Note Title (Big bold typography, borderless native document style)
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 36.sp
                ),
                decorationBox = { innerTextField ->
                    if (title.isEmpty()) {
                        Text(
                            text = "Tiêu đề",
                            style = TextStyle(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        )
                    }
                    innerTextField()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_title_editor")
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Note Body Content (Fluid, expansive, borderless)
            BasicTextField(
                value = content,
                onValueChange = { content = it },
                textStyle = TextStyle(
                    fontSize = 17.sp,
                    lineHeight = 26.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                decorationBox = { innerTextField ->
                    if (content.isEmpty() && tableRows.isEmpty() && drawingStrokes.isEmpty()) {
                        Text(
                            text = "Bắt đầu nhập nội dung...",
                            style = TextStyle(
                                fontSize = 17.sp,
                                lineHeight = 26.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        )
                    }
                    innerTextField()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_content_editor")
            )

            // Table View Component (if present)
            if (tableRows.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))

                NoteTableComponent(
                    tableRows = tableRows,
                    onUpdateTable = { tableRows = it },
                    onDeleteTable = { tableRows = emptyList() }
                )
            }

            // Handwritten Drawing Surface - Rendered directly on the note sheet under the text (No card, no header, no separate section)
            if (drawingStrokes.isNotEmpty() || isDrawingMode) {
                Spacer(modifier = Modifier.height(16.dp))

                InlineDrawingSurface(
                    strokes = drawingStrokes,
                    isDrawingMode = isDrawingMode,
                    toolMode = drawingTool,
                    selectedColor = drawingColor,
                    strokeWidth = drawingStrokeWidth,
                    onAddStroke = { stroke ->
                        drawingStrokes = drawingStrokes + stroke
                    },
                    onEraseAt = { offset ->
                        val radiusSq = 35f * 35f
                        drawingStrokes = drawingStrokes.filterNot { stroke ->
                            stroke.points.any { p ->
                                val dx = p.x - offset.x
                                val dy = p.y - offset.y
                                (dx * dx + dy * dy) <= radiusSq
                            }
                        }
                    },
                    onActivateDrawingMode = {
                        isDrawingMode = true
                    }
                )
            }

            // Extra breathing space at bottom - tapping here exits text editing & dismisses keyboard
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
            )
        }
    }

    // Category Picker Dialog
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Chọn danh mục ghi chú", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    NOTE_CATEGORIES.forEach { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    category = cat
                                    showCategoryDialog = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = category == cat,
                                onClick = {
                                    category = cat
                                    showCategoryDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(cat, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    // Color Palette Picker Dialog
    if (showColorDialog) {
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            title = { Text("Chọn màu giấy ghi chú", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NOTE_BACKGROUND_PALETTE.forEach { (colorVal, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    noteColor = colorVal
                                    showColorDialog = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorVal))
                                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (noteColor == colorVal) FontWeight.Bold else FontWeight.Normal
                            )
                            if (noteColor == colorVal) {
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorDialog = false }) {
                    Text("Xong")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// Interactive Table Component
// -------------------------------------------------------------
@Composable
fun NoteTableComponent(
    tableRows: List<List<String>>,
    onUpdateTable: (List<List<String>>) -> Unit,
    onDeleteTable: () -> Unit
) {
    val colCount = remember(tableRows) { tableRows.firstOrNull()?.size ?: 0 }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Table Header Bar: Label & Action Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TableChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Bảng dữ liệu (${tableRows.size}x$colCount)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = {
                            val newRow = List(colCount) { "" }
                            onUpdateTable(tableRows + listOf(newRow))
                        },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("+ Hàng", fontSize = 11.sp)
                    }

                    FilledTonalButton(
                        onClick = {
                            val updated = tableRows.map { row -> row + "" }
                            onUpdateTable(updated)
                        },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("+ Cột", fontSize = 11.sp)
                    }

                    IconButton(
                        onClick = onDeleteTable,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Xóa bảng",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // The Table Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp))
            ) {
                tableRows.forEachIndexed { rowIndex, row ->
                    val isHeader = rowIndex == 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isHeader) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                else MaterialTheme.colorScheme.surface
                            )
                    ) {
                        row.forEachIndexed { colIndex, cellText ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                BasicTextField(
                                    value = cellText,
                                    onValueChange = { newText ->
                                        val mutableRows = tableRows.map { it.toMutableList() }.toMutableList()
                                        mutableRows[rowIndex][colIndex] = newText
                                        onUpdateTable(mutableRows)
                                    },
                                    textStyle = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    decorationBox = { inner ->
                                        if (cellText.isEmpty()) {
                                            Text(
                                                text = if (isHeader) "Tiêu đề" else "Ô",
                                                style = TextStyle(
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                )
                                            )
                                        }
                                        inner()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Handwritten Drawing Surface (Rendered directly under note text)
// -------------------------------------------------------------
@Composable
fun InlineDrawingSurface(
    strokes: List<DrawingStroke>,
    isDrawingMode: Boolean,
    toolMode: DrawingToolMode,
    selectedColor: Long,
    strokeWidth: Float,
    onAddStroke: (DrawingStroke) -> Unit,
    onEraseAt: (Offset) -> Unit,
    onActivateDrawingMode: () -> Unit
) {
    var activePoints by remember { mutableStateOf<List<DrawingPoint>>(emptyList()) }
    val density = LocalDensity.current

    val maxStrokeY = remember(strokes) {
        strokes.flatMap { it.points }.maxOfOrNull { it.y } ?: 0f
    }

    val contentHeightDp = remember(maxStrokeY, isDrawingMode, density) {
        val yDp = with(density) { (maxStrokeY + 80f).toDp() }
        if (isDrawingMode) {
            maxOf(450.dp, yDp + 150.dp)
        } else {
            maxOf(280.dp, yDp)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(contentHeightDp)
            .then(
                if (isDrawingMode) {
                    Modifier
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .pointerInput(toolMode, selectedColor, strokeWidth) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    if (toolMode == DrawingToolMode.ERASER) {
                                        onEraseAt(offset)
                                    } else {
                                        activePoints = listOf(DrawingPoint(offset.x, offset.y))
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    if (toolMode == DrawingToolMode.ERASER) {
                                        onEraseAt(change.position)
                                    } else {
                                        activePoints = activePoints + DrawingPoint(change.position.x, change.position.y)
                                    }
                                },
                                onDragEnd = {
                                    if (toolMode != DrawingToolMode.ERASER && activePoints.size > 1) {
                                        val isHl = toolMode == DrawingToolMode.HIGHLIGHTER
                                        onAddStroke(
                                            DrawingStroke(
                                                points = activePoints,
                                                colorArgb = selectedColor,
                                                strokeWidth = if (isHl) strokeWidth * 2.5f else strokeWidth,
                                                isHighlighter = isHl
                                            )
                                        )
                                    }
                                    activePoints = emptyList()
                                },
                                onDragCancel = {
                                    activePoints = emptyList()
                                }
                            )
                        }
                } else {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onActivateDrawingMode
                    )
                }
            )
    ) {
        // Helpful subtle guide when drawing mode is on and canvas is empty
        if (isDrawingMode && strokes.isEmpty() && activePoints.isEmpty()) {
            Text(
                text = "✍️ Chạm & vẽ tay trực tiếp tại đây",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    fontSize = 13.sp
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Render all confirmed strokes directly onto the note paper
            strokes.forEach { stroke ->
                if (stroke.points.size > 1) {
                    val path = Path().apply {
                        moveTo(stroke.points[0].x, stroke.points[0].y)
                        for (i in 1 until stroke.points.size) {
                            lineTo(stroke.points[i].x, stroke.points[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(stroke.colorArgb).copy(alpha = if (stroke.isHighlighter) 0.35f else 1.0f),
                        style = Stroke(
                            width = stroke.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            // Render current active stroke while dragging
            if (activePoints.size > 1 && toolMode != DrawingToolMode.ERASER) {
                val activePath = Path().apply {
                    moveTo(activePoints[0].x, activePoints[0].y)
                    for (i in 1 until activePoints.size) {
                        lineTo(activePoints[i].x, activePoints[i].y)
                    }
                }
                val isHl = toolMode == DrawingToolMode.HIGHLIGHTER
                drawPath(
                    path = activePath,
                    color = Color(selectedColor).copy(alpha = if (isHl) 0.35f else 1.0f),
                    style = Stroke(
                        width = if (isHl) strokeWidth * 2.5f else strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}
