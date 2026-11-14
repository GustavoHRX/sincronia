package com.sincronia.app.data

import android.content.Context
import androidx.room.Room
import com.sincronia.app.core.database.AppDatabase
import com.sincronia.app.core.database.AlarmDao
import com.sincronia.app.core.database.EventDao
import com.sincronia.app.core.database.MIGRATION_1_2
import com.sincronia.app.data.edit.SyncRequester
import com.sincronia.app.data.sync.SyncScheduler
import com.sincronia.app.core.database.OutboxDao
import com.sincronia.app.core.database.SyncMetaDao
import com.sincronia.app.core.database.TaskDao
import com.sincronia.app.core.database.TaskListDao
import com.sincronia.app.data.calendar.CalendarSource
import com.sincronia.app.data.demo.FakeCalendarSource
import com.sincronia.app.data.demo.FakeTaskSource
import com.sincronia.app.data.tasks.TaskSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "sincronia.db")
            .addMigrations(MIGRATION_1_2).build()

    @Provides fun eventDao(database: AppDatabase): EventDao = database.eventDao()
    @Provides fun taskDao(database: AppDatabase): TaskDao = database.taskDao()
    @Provides fun taskListDao(database: AppDatabase): TaskListDao = database.taskListDao()
    @Provides fun alarmDao(database: AppDatabase): AlarmDao = database.alarmDao()
    @Provides fun outboxDao(database: AppDatabase): OutboxDao = database.outboxDao()
    @Provides fun syncMetaDao(database: AppDatabase): SyncMetaDao = database.syncMetaDao()

    @Provides fun syncRequester(@ApplicationContext context: Context): SyncRequester =
        SyncRequester { SyncScheduler.enqueue(context) }

    @Provides fun calendarSource(@ApplicationContext context: Context): CalendarSource = FakeCalendarSource(context)
    @Provides fun taskSource(@ApplicationContext context: Context): TaskSource = FakeTaskSource(context)
}
