package com.sincronia.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(events: List<EventEntity>)
    @Query("SELECT * FROM events WHERE accountId = :accountId AND deleted = 0 AND ( (allDay = 1 AND startDate <= :day AND endDate > :day) OR (allDay = 0 AND startAt < :end AND endAt > :start) ) ORDER BY allDay DESC, startAt, title")
    fun observeDay(accountId: String, day: LocalDate, start: Instant, end: Instant): Flow<List<EventEntity>>
    @Query("SELECT * FROM events WHERE accountId = :accountId AND deleted = 0 AND ( (allDay = 1 AND startDate < :toDate AND endDate > :fromDate) OR (allDay = 0 AND startAt < :end AND endAt > :start) )")
    fun observeRange(accountId: String, fromDate: LocalDate, toDate: LocalDate, start: Instant, end: Instant): Flow<List<EventEntity>>
    @Query("SELECT * FROM events WHERE accountId = :accountId AND googleId = :googleId LIMIT 1")
    suspend fun byGoogleId(accountId: String, googleId: String): EventEntity?
    @Query("SELECT * FROM events WHERE accountId = :accountId AND syncState = 'SYNCED'")
    suspend fun synced(accountId: String): List<EventEntity>
    @Query("DELETE FROM events WHERE accountId = :accountId") suspend fun deleteAccount(accountId: String)
}

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(tasks: List<TaskEntity>)
    @Query("SELECT * FROM tasks WHERE accountId = :accountId AND deleted = 0 AND dueDate = :day AND status != 'completed' ORDER BY position, title")
    fun observeDay(accountId: String, day: LocalDate): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE accountId = :accountId AND deleted = 0 AND status != 'completed' AND dueDate >= :start AND dueDate < :end")
    fun observeRange(accountId: String, start: LocalDate, end: LocalDate): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE accountId = :accountId AND deleted = 0 AND status != 'completed' AND dueDate < :today ORDER BY dueDate, position, title")
    fun observeOverdue(accountId: String, today: LocalDate): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE accountId = :accountId AND deleted = 0 AND hidden = 0 AND status = 'completed' AND completedAt >= :start AND completedAt < :end ORDER BY completedAt DESC, title")
    fun observeCompletedBetween(accountId: String, start: Instant, end: Instant): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE accountId = :accountId AND deleted = 0 AND taskListId = :listId AND (status != 'completed' OR completedAt >= :completedSince) ORDER BY status, position, title")
    fun observeList(accountId: String, listId: String, completedSince: Instant): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE accountId = :accountId AND googleId = :googleId LIMIT 1")
    suspend fun byGoogleId(accountId: String, googleId: String): TaskEntity?
    @Query("SELECT * FROM tasks WHERE accountId = :accountId") suspend fun all(accountId: String): List<TaskEntity>
    @Query("DELETE FROM tasks WHERE accountId = :accountId") suspend fun deleteAccount(accountId: String)
}

@Dao
interface TaskListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(lists: List<TaskListEntity>)
    @Query("SELECT * FROM task_lists WHERE accountId = :accountId ORDER BY isDefault DESC, title")
    fun observeAll(accountId: String): Flow<List<TaskListEntity>>
    @Query("SELECT * FROM task_lists WHERE accountId = :accountId") suspend fun all(accountId: String): List<TaskListEntity>
    @Query("DELETE FROM task_lists WHERE accountId = :accountId") suspend fun deleteAccount(accountId: String)
}

@Dao
interface AlarmDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(alarm: AlarmEntity): Long
    @Query("SELECT * FROM alarms WHERE accountId = :accountId AND itemType = :itemType AND itemLocalId = :itemLocalId LIMIT 1")
    suspend fun forItem(accountId: String, itemType: String, itemLocalId: String): AlarmEntity?
    @Query("SELECT * FROM alarms WHERE accountId = :accountId AND triggerAt > :now ORDER BY triggerAt LIMIT 50")
    suspend fun upcoming(accountId: String, now: Instant): List<AlarmEntity>
}

@Dao
interface OutboxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(entry: OutboxEntity): Long
    @Query("SELECT * FROM outbox WHERE accountId = :accountId AND state = 'PENDING' AND (nextAttemptAt IS NULL OR nextAttemptAt <= :now) ORDER BY createdAt")
    suspend fun ready(accountId: String, now: Instant): List<OutboxEntity>
    @Query("SELECT * FROM outbox WHERE accountId = :accountId AND state IN ('PENDING', 'FAILED')")
    suspend fun blockingDownload(accountId: String): List<OutboxEntity>
    @Query("SELECT * FROM outbox WHERE accountId = :accountId AND itemType = :type AND itemLocalId = :localId LIMIT 1")
    suspend fun forItem(accountId: String, type: String, localId: String): OutboxEntity?
}

@Dao
interface SyncMetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(meta: SyncMetaEntity)
    @Query("SELECT value FROM sync_meta WHERE accountId = :accountId AND key = :key LIMIT 1")
    suspend fun value(accountId: String, key: String): String?
}
