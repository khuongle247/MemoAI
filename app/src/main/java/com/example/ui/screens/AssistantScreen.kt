package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.ui.ChatMessage
import com.example.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    messages: List<ChatMessage>,
    isThinking: Boolean,
    onSendMessage: (String) -> Unit,
    onOpenVoiceModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val quickQuestions = listOf(
        "📋 Hôm nay tôi phải làm gì?",
        "⚡ Việc nào quan trọng nhất?",
        "📝 Tóm tắt ghi chú gần đây",
        "🎯 Gợi ý sắp xếp thời gian",
        "💡 Đề xuất ý tưởng học tập"
    )

    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))

                        Column {
                            Text(
                                text = "Trợ lý MemoAI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
                                Text(
                                    text = if (isThinking) "Đang suy nghĩ..." else "Sẵn sàng hỗ trợ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    FilledTonalIconButton(
                        onClick = onOpenVoiceModal,
                        modifier = Modifier
                            .size(MaterialTheme.spacing.minTouchTarget)
                            .testTag("assistant_mic_button")
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Thu âm",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                // Quick Prompt Suggestions
                LazyRow(
                    contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.large),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = MaterialTheme.spacing.extraSmall)
                ) {
                    items(quickQuestions) { q ->
                        SuggestionChip(
                            onClick = {
                                onSendMessage(q)
                            },
                            label = { Text(q, style = MaterialTheme.typography.labelMedium) },
                            shape = MaterialTheme.shapes.small
                        )
                    }
                }

                // Input Bar at bottom
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = MaterialTheme.spacing.large,
                                vertical = MaterialTheme.spacing.small
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    "Nhập câu hỏi hoặc yêu cầu...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("assistant_input_field"),
                            maxLines = 3,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Send,
                                capitalization = KeyboardCapitalization.Sentences
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (inputText.isNotBlank()) {
                                        val text = inputText
                                        inputText = ""
                                        onSendMessage(text)
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        )

                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))

                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    val text = inputText
                                    inputText = ""
                                    onSendMessage(text)
                                } else {
                                    onOpenVoiceModal()
                                }
                            },
                            modifier = Modifier
                                .size(MaterialTheme.spacing.minTouchTarget)
                                .clip(CircleShape)
                                .background(
                                    if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.secondaryContainer
                                )
                                .testTag("assistant_chat_send_button")
                        ) {
                            Icon(
                                imageVector = if (inputText.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                                contentDescription = if (inputText.isNotBlank()) "Gửi" else "Thu âm giọng nói",
                                tint = if (inputText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier.testTag("assistant_screen")
    ) { innerPadding ->
        // Chat message list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = MaterialTheme.spacing.large,
                vertical = MaterialTheme.spacing.medium
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
                // Empty state greeting
                if (messages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = MaterialTheme.spacing.extraLarge),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.tertiaryContainer
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

                            Text(
                                text = "Xin chào! Tôi có thể giúp gì cho bạn?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                            Text(
                                text = "Bạn có thể hỏi về kế hoạch hôm nay, tóm tắt các ghi chú hoặc nhờ tôi sắp xếp công việc.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)
                            )
                        }
                    }
                }

                items(messages) { msg ->
                    ChatBubbleItem(message = msg)
                }

                if (isThinking) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = MaterialTheme.spacing.extraSmall)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                            Text(
                                text = "MemoAI đang phân tích...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

@Composable
fun ChatBubbleItem(message: ChatMessage) {
    val isUser = message.sender == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isUser) {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                            )
                        )
                    }
                )
                .padding(
                    horizontal = MaterialTheme.spacing.large,
                    vertical = MaterialTheme.spacing.medium
                )
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isUser) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}
