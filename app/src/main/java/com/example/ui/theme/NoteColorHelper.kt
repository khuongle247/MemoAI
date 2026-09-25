package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class NoteCardStyle(
    val containerColor: Color,
    val borderColor: Color,
    val titleColor: Color,
    val contentColor: Color,
    val dateColor: Color,
    val accentTagColor: Color
)

object NoteColorHelper {

    @Composable
    fun getNoteStyle(colorLong: Long, isDark: Boolean): NoteCardStyle {
        val baseNoteColor = Color(colorLong)

        return if (isDark) {
            NoteCardStyle(
                // In dark mode: dark surface with gentle tint of note's hue
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                borderColor = baseNoteColor.copy(alpha = 0.5f),
                titleColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                dateColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                accentTagColor = MaterialTheme.colorScheme.primary
            )
        } else {
            NoteCardStyle(
                // In light mode: smooth pastel background with crisp dark slate text and gentle border
                containerColor = baseNoteColor.copy(alpha = 0.85f),
                borderColor = baseNoteColor.copy(alpha = 0.5f),
                titleColor = Color(0xFF0F172A),
                contentColor = Color(0xFF334155),
                dateColor = Color(0xFF64748B),
                accentTagColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}
