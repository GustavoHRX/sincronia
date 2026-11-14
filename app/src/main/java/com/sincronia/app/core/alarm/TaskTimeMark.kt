package com.sincronia.app.core.alarm

import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * The Tasks API keeps no time, so the alarm time lives in the last line of the notes as exactly
 * `⏰ HH:mm` (spec 7.3). It is stripped before saving to Room and rewritten on upload.
 */
object TaskTimeMark {
    private val regex = Regex("\\n?⏰ ([01]\\d|2[0-3]):([0-5]\\d)\\s*$")
    private val format = DateTimeFormatter.ofPattern("HH:mm")

    data class Parsed(val notes: String?, val time: LocalTime?)

    fun parse(notes: String?): Parsed {
        if (notes == null) return Parsed(null, null)
        val match = regex.find(notes) ?: return Parsed(notes, null)
        val time = LocalTime.of(match.groupValues[1].toInt(), match.groupValues[2].toInt())
        return Parsed(notes.removeRange(match.range).ifEmpty { null }, time)
    }

    /** Replaces any mark; null [time] removes it. Never appends a second one. */
    fun write(notes: String?, time: LocalTime?): String? {
        val clean = parse(notes).notes
        if (time == null) return clean
        val mark = "⏰ ${time.format(format)}"
        return if (clean.isNullOrEmpty()) mark else "$clean\n$mark"
    }
}
