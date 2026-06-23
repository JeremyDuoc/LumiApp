package com.jeremy.lumi.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.jeremy.lumi.R

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    @StringRes val titleRes: Int
) {
    object Home     : BottomNavItem("home",     Icons.Default.Home,              R.string.nav_home)
    object Calendar : BottomNavItem("calendar", Icons.Default.CalendarMonth,     R.string.nav_calendar)
    object Insights : BottomNavItem("insights", Icons.Default.Insights,           R.string.nav_insights)
    object Chat     : BottomNavItem("chat",     Icons.Default.ChatBubbleOutline,  R.string.nav_chat)
    object Settings : BottomNavItem("settings", Icons.Default.Settings,           R.string.nav_settings)
    // Forum sigue siendo accesible pero ya no ocupa espacio en la barra principal
    object Forum    : BottomNavItem("forum",    Icons.Default.Favorite,           R.string.nav_forum)
}