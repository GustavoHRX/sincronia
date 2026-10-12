package com.sincronia.app.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.sincronia.app.R

enum class TopLevelDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    TODAY("today", R.string.nav_today, R.drawable.ic_nav_today),
    AGENDA("agenda", R.string.nav_agenda, R.drawable.ic_nav_agenda),
    TASKS("tasks", R.string.nav_tasks, R.drawable.ic_nav_tasks),
    SETTINGS("settings", R.string.nav_settings, R.drawable.ic_nav_settings),
}
