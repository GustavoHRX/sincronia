package com.sincronia.app.core.auth

/** The only two scopes the app may request, always as the full URL (spec 6.4 and rule 4). */
object GoogleScopes {
    const val CALENDAR = "https://www.googleapis.com/auth/calendar.events"
    const val TASKS = "https://www.googleapis.com/auth/tasks"
    val ALL = listOf(CALENDAR, TASKS)

    /** Scopes the user unchecked on the consent screen. */
    fun missing(granted: Collection<String>): Set<String> = ALL.toSet() - granted.toSet()
}
