package com.sincronia.app.core.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LogFileWriterTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun appendsLinesAndCreatesFolder() {
        val file = File(temp.root, "logs/app.log")
        val writer = LogFileWriter(file, maxBytes = 1024)

        writer.append("primeira")
        writer.append("segunda")

        assertEquals(listOf("primeira", "segunda"), file.readLines())
        assertFalse(writer.backup.exists())
    }

    @Test
    fun rotatesWhenLimitIsExceeded() {
        val file = File(temp.root, "app.log")
        val writer = LogFileWriter(file, maxBytes = 20)

        writer.append("0123456789") // 11 bytes
        writer.append("abcdefghij") // passaria de 20: rotaciona antes de gravar

        assertTrue(writer.backup.exists())
        assertEquals(listOf("0123456789"), writer.backup.readLines())
        assertEquals(listOf("abcdefghij"), file.readLines())
    }

    @Test
    fun keepsOnlyOneBackup() {
        val file = File(temp.root, "app.log")
        val writer = LogFileWriter(file, maxBytes = 12)

        writer.append("linha-1....")
        writer.append("linha-2....")
        writer.append("linha-3....")

        assertEquals(listOf("linha-2...."), writer.backup.readLines())
        assertEquals(listOf("linha-3...."), file.readLines())
        assertEquals(2, temp.root.listFiles()!!.size)
    }
}
