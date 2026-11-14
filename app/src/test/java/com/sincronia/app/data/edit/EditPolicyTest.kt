package com.sincronia.app.data.edit

import com.sincronia.app.core.database.EventEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EditPolicyTest {
    private val base = EventEntity("l", "a", "g", title = "X")

    @Test fun `only default events organized by the user or open to guests are editable`() {
        assertNull(EditPolicy.eventReadOnly(base))
        assertNull(EditPolicy.eventReadOnly(base.copy(organizerSelf = false, guestsCanModify = true)))
        assertEquals(ReadOnlyReason.SPECIAL_TYPE, EditPolicy.eventReadOnly(base.copy(eventType = "birthday")))
        assertEquals(ReadOnlyReason.SPECIAL_TYPE, EditPolicy.eventReadOnly(base.copy(eventType = "outOfOffice")))
        assertEquals(ReadOnlyReason.RECURRING, EditPolicy.eventReadOnly(base.copy(recurringEventId = "s")))
        assertEquals(ReadOnlyReason.NOT_ORGANIZER, EditPolicy.eventReadOnly(base.copy(organizerSelf = false)))
    }
}
