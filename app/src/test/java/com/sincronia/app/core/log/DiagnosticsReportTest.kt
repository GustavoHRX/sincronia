package com.sincronia.app.core.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsReportTest {
    @Test
    fun `redact removes e-mails and tokens`() {
        val line = "2026-11-12 W/Http: fulano.tal+x@gmail.com Authorization: Bearer abc.DEF-123 token ya29.a0AfB_xyz"
        val clean = DiagnosticsReport.redact(line)
        assertFalse(clean.contains("gmail.com"))
        assertFalse(clean.contains("abc.DEF-123"))
        assertFalse(clean.contains("ya29."))
        assertTrue(clean.contains("<e-mail>"))
    }

    @Test
    fun `redact keeps ordinary lines`() {
        val line = "2026-11-12 19:00:00.000 I/Sync: 12 events, 3 tasks, alarm 42 at 2026-11-13T09:00Z"
        assertEquals(line, DiagnosticsReport.redact(line))
    }

    @Test
    fun `build writes header and filtered log`() {
        val text = DiagnosticsReport.build(listOf("Versão" to "1.0"), listOf("a@b.com", "ok"))
        assertTrue(text.contains("Versão: 1.0"))
        assertTrue(text.contains("--- log (2 linhas) ---"))
        assertFalse(text.contains("a@b.com"))
        assertTrue(text.trimEnd().endsWith("ok"))
    }
}
