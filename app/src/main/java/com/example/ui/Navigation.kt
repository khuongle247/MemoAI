package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : Screen("home", "Trang chủ", Icons.Filled.Home, Icons.Outlined.Home)
    object Tasks : Screen("tasks", "Công việc", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle)
    object Notes : Screen("notes", "Ghi chú", Icons.Filled.Description, Icons.Outlined.Description)
    object Calendar : Screen("calendar", "Lịch trình", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    object Assistant : Screen("assistant", "Trợ lý AI", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome)

    object Statistics : Screen("statistics", "Thống kê", Icons.Filled.Home, Icons.Outlined.Home)
    object Settings : Screen("settings", "Cài đặt", Icons.Filled.Home, Icons.Outlined.Home)
}
