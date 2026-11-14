package com.sincronia.app.core.database

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DatabaseConverters {
    // Epoch milliseconds, so SQL comparisons and ORDER BY follow the real time order.
    @TypeConverter fun instantToMillis(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter fun millisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
    @TypeConverter fun dateToString(value: LocalDate?): String? = value?.toString()
    @TypeConverter fun stringToDate(value: String?): LocalDate? = value?.let(LocalDate::parse)
    @TypeConverter fun timeToString(value: LocalTime?): String? = value?.toString()
    @TypeConverter fun stringToTime(value: String?): LocalTime? = value?.let(LocalTime::parse)
    @TypeConverter fun intsToString(value: List<Int>): String = Json.encodeToString(value)
    @TypeConverter fun stringToInts(value: String): List<Int> = Json.decodeFromString(value)
    @TypeConverter fun stringsToString(value: List<String>): String = Json.encodeToString(value)
    @TypeConverter fun stringToStrings(value: String): List<String> = Json.decodeFromString(value)
}
