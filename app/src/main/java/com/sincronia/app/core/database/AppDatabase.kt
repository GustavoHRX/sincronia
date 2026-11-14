package com.sincronia.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [EventEntity::class, TaskEntity::class, TaskListEntity::class, AlarmEntity::class,
        OutboxEntity::class, SyncMetaEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun taskDao(): TaskDao
    abstract fun taskListDao(): TaskListDao
    abstract fun alarmDao(): AlarmDao
    abstract fun outboxDao(): OutboxDao
    abstract fun syncMetaDao(): SyncMetaDao
}
