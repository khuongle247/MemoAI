package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.ExtractedTask
import com.example.ai.NoteSummaryResult
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.TaskEntity
import com.example.ui.ChatMessage
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val isDarkTheme = when (settings.darkModeOption) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                MainAppContent(
                    viewModel = viewModel,
                    intent = intent
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun MainAppContent(
    viewModel: MainViewModel,
    intent: Intent?
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // State collections
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val todayTasks by viewModel.todayTasks.collectAsStateWithLifecycle()
    val upcomingTasks by viewModel.upcomingTasks.collectAsStateWithLifecycle()
    val activeNotes by viewModel.activeNotes.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val partialText by viewModel.partialVoiceText.collectAsStateWithLifecycle()
    val aiIntentResult by viewModel.aiIntentResult.collectAsStateWithLifecycle()
    val isAiProcessing by viewModel.isAiProcessing.collectAsStateWithLifecycle()
    val userFeedback by viewModel.userFeedbackMessage.collectAsStateWithLifecycle()

    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isAssistantThinking by viewModel.isAssistantThinking.collectAsStateWithLifecycle()

    // Navigation state
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    // Dialog & Sheet States
    var showVoiceModal by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }
    var showTaskDialog by remember { mutableStateOf(false) }
    var defaultDateForTask by remember { mutableStateOf<String?>(null) }

    var noteToEdit by remember { mutableStateOf<NoteEntity?>(null) }
    var showNoteDialog by remember { mutableStateOf(false) }

    var summaryDialogData by remember { mutableStateOf<Pair<String, NoteSummaryResult>?>(null) }
    var extractTasksData by remember { mutableStateOf<Pair<NoteEntity, List<ExtractedTask>>?>(null) }

    var showFabMenu by remember { mutableStateOf(false) }

    // User feedback snackbar
    LaunchedEffect(userFeedback) {
        userFeedback?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    // Permission Launchers
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceInput()
        } else {
            // Permission denied
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Handled */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Deep link or notification extra check
    LaunchedEffect(intent) {
        val taskIdExtra = intent?.getLongExtra("taskId", -1L) ?: -1L
        val dataUri = intent?.data
        val deepLinkId = dataUri?.getQueryParameter("id")?.toLongOrNull() ?: taskIdExtra
        if (deepLinkId != -1L) {
            val target = allTasks.firstOrNull { it.id == deepLinkId }
            if (target != null) {
                taskToEdit = target
                showTaskDialog = true
            }
        }
    }

    val navItems = listOf(
        Screen.Home,
        Screen.Tasks,
        Screen.Notes,
        Screen.Calendar,
        Screen.Assistant
    )

    if (showNoteDialog) {
        NoteEditorScreen(
            initialNote = noteToEdit,
            onSave = { note ->
                viewModel.saveNote(note)
            },
            onDelete = { noteId ->
                viewModel.deleteNote(noteId)
                showNoteDialog = false
                noteToEdit = null
            },
            onBack = {
                showNoteDialog = false
                noteToEdit = null
            },
            onSummarize = { note ->
                viewModel.summarizeNote(note) { result ->
                    summaryDialogData = Pair(note.title, result)
                }
            },
            onExtractTasks = { note ->
                viewModel.extractTasksFromNote(note) { tasks ->
                    extractTasksData = Pair(note, tasks)
                }
            }
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentScreen != Screen.Statistics && currentScreen != Screen.Settings) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    navItems.forEach { screen ->
                        val selected = currentScreen == screen
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentScreen = screen },
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title, fontSize = 11.sp) },
                            modifier = Modifier.testTag("nav_${screen.route}")
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentScreen != Screen.Statistics && currentScreen != Screen.Settings && currentScreen != Screen.Assistant) {
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedVisibility(
                        visible = showFabMenu,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            // Option 1: AI Voice
                            ExtendedFloatingActionButton(
                                text = { Text("🎙 Thu âm bằng AI") },
                                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                                onClick = {
                                    showFabMenu = false
                                    showVoiceModal = true
                                },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.testTag("fab_ai_voice")
                            )

                            // Option 2: Add Task
                            ExtendedFloatingActionButton(
                                text = { Text("✅ Thêm công việc") },
                                icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                                onClick = {
                                    showFabMenu = false
                                    taskToEdit = null
                                    defaultDateForTask = null
                                    showTaskDialog = true
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.testTag("fab_add_task")
                            )

                            // Option 3: Add Note
                            ExtendedFloatingActionButton(
                                text = { Text("📝 Thêm ghi chú") },
                                icon = { Icon(Icons.Default.Description, contentDescription = null) },
                                onClick = {
                                    showFabMenu = false
                                    noteToEdit = null
                                    showNoteDialog = true
                                },
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.testTag("fab_add_note")
                            )
                        }
                    }

                    FloatingActionButton(
                        onClick = { showFabMenu = !showFabMenu },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.testTag("main_fab")
                    ) {
                        Icon(
                            imageVector = if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Thao tác nhanh"
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                Screen.Home -> {
                    HomeScreen(
                        userName = settings.userName,
                        todayTasks = todayTasks,
                        upcomingTasks = upcomingTasks,
                        pinnedNotes = activeNotes.filter { it.isPinned },
                        onToggleTask = { viewModel.toggleTaskComplete(it) },
                        onEditTask = {
                            taskToEdit = it
                            showTaskDialog = true
                        },
                        onOpenNote = {
                            noteToEdit = it
                            showNoteDialog = true
                        },
                        onOpenVoiceModal = { showVoiceModal = true },
                        onOpenStats = { currentScreen = Screen.Statistics },
                        onOpenSettings = { currentScreen = Screen.Settings }
                    )
                }

                Screen.Tasks -> {
                    TasksScreen(
                        tasks = allTasks,
                        onToggleTask = { viewModel.toggleTaskComplete(it) },
                        onEditTask = {
                            taskToEdit = it
                            showTaskDialog = true
                        },
                        onDeleteTask = { viewModel.deleteTask(it) },
                        onAddNewTask = {
                            taskToEdit = null
                            defaultDateForTask = null
                            showTaskDialog = true
                        }
                    )
                }

                Screen.Notes -> {
                    NotesScreen(
                        notes = activeNotes,
                        onOpenNote = {
                            noteToEdit = it
                            showNoteDialog = true
                        },
                        onTogglePin = { viewModel.toggleNotePin(it) },
                        onDeleteNote = { viewModel.deleteNote(it) },
                        onSummarizeNote = { note ->
                            viewModel.summarizeNote(note) { result ->
                                summaryDialogData = note.title to result
                            }
                        },
                        onExtractTasks = { note ->
                            viewModel.extractTasksFromNote(note) { extracted ->
                                extractTasksData = note to extracted
                            }
                        },
                        onAddNewNote = {
                            noteToEdit = null
                            showNoteDialog = true
                        }
                    )
                }

                Screen.Calendar -> {
                    CalendarScreen(
                        tasks = allTasks,
                        notes = activeNotes,
                        onToggleTask = { viewModel.toggleTaskComplete(it) },
                        onEditTask = {
                            taskToEdit = it
                            showTaskDialog = true
                        },
                        onOpenNote = {
                            noteToEdit = it
                            showNoteDialog = true
                        },
                        onAddTaskForDate = { dateStr ->
                            taskToEdit = null
                            defaultDateForTask = dateStr
                            showTaskDialog = true
                        },
                        onAddNoteForDate = { _ ->
                            noteToEdit = null
                            showNoteDialog = true
                        }
                    )
                }

                Screen.Assistant -> {
                    AssistantScreen(
                        messages = chatMessages,
                        isThinking = isAssistantThinking,
                        onSendMessage = { viewModel.sendChatMessage(it) },
                        onOpenVoiceModal = { showVoiceModal = true },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                            .consumeWindowInsets(PaddingValues(bottom = innerPadding.calculateBottomPadding()))
                    )
                }

                Screen.Statistics -> {
                    StatisticsScreen(
                        tasks = allTasks,
                        notes = activeNotes,
                        onBack = { currentScreen = Screen.Home }
                    )
                }

                Screen.Settings -> {
                    SettingsScreen(
                        settings = settings,
                        onUpdateDarkMode = { viewModel.settingsRepository.updateDarkMode(it) },
                        onUpdateAutoCreate = { viewModel.settingsRepository.updateAutoCreate(it) },
                        onUpdateSound = { viewModel.settingsRepository.updateSound(it) },
                        onUpdateVibration = { viewModel.settingsRepository.updateVibration(it) },
                        onUpdateUserName = { viewModel.settingsRepository.updateUserName(it) },
                        onExportData = { viewModel.exportDataJson() },
                        onClearAllData = { viewModel.clearAllData() },
                        onBack = { currentScreen = Screen.Home }
                    )
                }
            }
        }
    }
}

    // Voice Capture Modal
    if (showVoiceModal) {
        VoiceCaptureModal(
            voiceState = voiceState,
            partialText = partialText,
            isAiProcessing = isAiProcessing,
            onStartListening = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    viewModel.startVoiceInput()
                } else {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onStopListening = { viewModel.stopVoiceInput() },
            onSubmitPrompt = { prompt ->
                showVoiceModal = false
                viewModel.processSpokenText(prompt)
            },
            onDismiss = {
                showVoiceModal = false
                viewModel.speechRecognizerHelper.reset()
            }
        )
    }

    // AI Confirmation Dialog
    aiIntentResult?.let { result ->
        AIConfirmationDialog(
            result = result,
            onConfirm = { confirmed ->
                viewModel.executeIntentAction(confirmed)
            },
            onEditManually = { editItem ->
                if (editItem.taskData != null) {
                    val t = editItem.taskData
                    taskToEdit = TaskEntity(
                        title = t.title,
                        description = t.description,
                        dueDate = t.date,
                        dueTime = t.time,
                        reminderMinutesBefore = t.reminderMinutesBefore,
                        priority = t.priority,
                        category = t.category
                    )
                    showTaskDialog = true
                } else if (editItem.noteData != null) {
                    val n = editItem.noteData
                    noteToEdit = NoteEntity(
                        title = n.title,
                        content = n.content,
                        category = n.category,
                        tags = n.tags.joinToString(",")
                    )
                    showNoteDialog = true
                }
                viewModel.dismissAiPreview()
            },
            onDismiss = { viewModel.dismissAiPreview() }
        )
    }

    // Task Create / Edit Dialog
    if (showTaskDialog) {
        TaskEditDialog(
            taskToEdit = taskToEdit,
            defaultDate = defaultDateForTask,
            onSave = { task ->
                viewModel.saveTask(task)
                showTaskDialog = false
                taskToEdit = null
                defaultDateForTask = null
            },
            onDismiss = {
                showTaskDialog = false
                taskToEdit = null
                defaultDateForTask = null
            }
        )
    }

    // AI Note Summary Dialog
    summaryDialogData?.let { (title, summary) ->
        SummaryResultDialog(
            noteTitle = title,
            summaryResult = summary,
            onDismiss = { summaryDialogData = null }
        )
    }

    // AI Extract Tasks Dialog
    extractTasksData?.let { (note, extracted) ->
        ExtractTasksDialog(
            noteTitle = note.title,
            tasks = extracted,
            onCreateTasks = { selectedTasks ->
                viewModel.createBatchTasksFromNote(selectedTasks, note.id)
                extractTasksData = null
            },
            onDismiss = { extractTasksData = null }
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
