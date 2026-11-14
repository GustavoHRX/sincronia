package com.sincronia.app.core.alarm

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskTimeMarkTest {
    @Test fun `read, rewrite and remove the mark without duplicating`() {
        val parsed = TaskTimeMark.parse("Levar documentos\n⏰ 14:30")
        assertEquals("Levar documentos", parsed.notes)
        assertEquals(LocalTime.of(14, 30), parsed.time)
        assertEquals("Levar documentos\n⏰ 09:05", TaskTimeMark.write("Levar documentos\n⏰ 14:30", LocalTime.of(9, 5)))
        assertEquals("Levar documentos", TaskTimeMark.write("Levar documentos\n⏰ 14:30", null))
        assertEquals("⏰ 08:00", TaskTimeMark.write(null, LocalTime.of(8, 0)))
        assertEquals(TaskTimeMark.write("a", LocalTime.of(8, 0)),
            TaskTimeMark.write(TaskTimeMark.write("a", LocalTime.of(8, 0)), LocalTime.of(8, 0)))
    }

    @Test fun `only an exact mark on the last line counts`() {
        assertEquals(LocalTime.of(7, 0), TaskTimeMark.parse("⏰ 07:00  ").time)
        assertNull(TaskTimeMark.parse("⏰ 24:00").time)
        assertNull(TaskTimeMark.parse("⏰ 14:30\nmais texto").time)
        assertNull(TaskTimeMark.parse("às 14:30").time)
        assertNull(TaskTimeMark.parse("⏰ 7:00").time)
    }
}
