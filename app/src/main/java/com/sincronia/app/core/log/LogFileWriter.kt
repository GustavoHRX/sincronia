package com.sincronia.app.core.log

import java.io.File

/**
 * Grava linhas de log num arquivo local com rotação simples: quando [file] passa de
 * [maxBytes], ele vira `<nome>.1` (substituindo o anterior) e um arquivo novo começa.
 * No máximo 2 × [maxBytes] ficam no aparelho.
 */
class LogFileWriter(
    private val file: File,
    private val maxBytes: Long = DEFAULT_MAX_BYTES,
) {
    val backup: File get() = File(file.parentFile, "${file.name}.1")

    @Synchronized
    fun append(line: String) {
        file.parentFile?.mkdirs()
        if (file.exists() && file.length() + line.length + 1 > maxBytes) {
            backup.delete()
            file.renameTo(backup)
        }
        file.appendText(line + "\n")
    }

    companion object {
        const val DEFAULT_MAX_BYTES = 512L * 1024
    }
}
