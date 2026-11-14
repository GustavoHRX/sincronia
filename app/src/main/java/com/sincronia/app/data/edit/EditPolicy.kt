package com.sincronia.app.data.edit

import com.sincronia.app.core.database.EventEntity

/** Why an event cannot be edited in the app (spec 7.3); the local alarm stays editable in all cases. */
enum class ReadOnlyReason { SPECIAL_TYPE, RECURRING, NOT_ORGANIZER }

object EditPolicy {
    /** null = editable. */
    fun eventReadOnly(event: EventEntity): ReadOnlyReason? = when {
        // birthday, fromGmail, focusTime, outOfOffice, workingLocation: the API restricts changes.
        event.eventType != "default" -> ReadOnlyReason.SPECIAL_TYPE
        event.recurringEventId != null -> ReadOnlyReason.RECURRING
        !event.organizerSelf && !event.guestsCanModify -> ReadOnlyReason.NOT_ORGANIZER
        else -> null
    }
}
