package com.sincronia.app.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.Instant

private class InstantTable(
    val name: String,
    val createSql: String,
    val indices: List<String>,
    val instantColumns: List<String>,
)

private val instantTables = listOf(
    InstantTable(
        name = "events",
        createSql = "CREATE TABLE IF NOT EXISTS `events_new` (`localId` TEXT NOT NULL, `accountId` TEXT NOT NULL, " +
            "`googleId` TEXT, `calendarId` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT, `location` TEXT, " +
            "`allDay` INTEGER NOT NULL, `startAt` INTEGER, `endAt` INTEGER, `startDate` TEXT, `endDate` TEXT, " +
            "`timeZone` TEXT, `status` TEXT NOT NULL, `eventType` TEXT NOT NULL, `recurringEventId` TEXT, " +
            "`originalStartTime` TEXT, `organizerSelf` INTEGER NOT NULL, `guestsCanModify` INTEGER NOT NULL, " +
            "`selfResponseStatus` TEXT, `reminderUseDefault` INTEGER NOT NULL, `reminderMinutes` TEXT NOT NULL, " +
            "`colorId` TEXT, `htmlLink` TEXT, `etag` TEXT, `remoteUpdated` INTEGER, `updatedAt` INTEGER NOT NULL, " +
            "`deleted` INTEGER NOT NULL, `syncState` TEXT NOT NULL, PRIMARY KEY(`accountId`, `localId`))",
        indices = listOf(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_events_accountId_googleId` ON `events` (`accountId`, `googleId`)",
            "CREATE INDEX IF NOT EXISTS `index_events_accountId_startAt` ON `events` (`accountId`, `startAt`)",
            "CREATE INDEX IF NOT EXISTS `index_events_accountId_startDate` ON `events` (`accountId`, `startDate`)",
        ),
        instantColumns = listOf("startAt", "endAt", "remoteUpdated", "updatedAt"),
    ),
    InstantTable(
        name = "tasks",
        createSql = "CREATE TABLE IF NOT EXISTS `tasks_new` (`localId` TEXT NOT NULL, `accountId` TEXT NOT NULL, " +
            "`googleId` TEXT, `taskListId` TEXT NOT NULL, `remoteTaskListId` TEXT, `title` TEXT NOT NULL, `notes` TEXT, " +
            "`dueDate` TEXT, `status` TEXT NOT NULL, `completedAt` INTEGER, `parentId` TEXT, `position` TEXT, " +
            "`hidden` INTEGER NOT NULL, `webViewLink` TEXT, `etag` TEXT, `remoteUpdated` INTEGER, " +
            "`updatedAt` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, `syncState` TEXT NOT NULL, " +
            "PRIMARY KEY(`accountId`, `localId`))",
        indices = listOf(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_tasks_accountId_googleId` ON `tasks` (`accountId`, `googleId`)",
            "CREATE INDEX IF NOT EXISTS `index_tasks_accountId_taskListId_dueDate` ON `tasks` " +
                "(`accountId`, `taskListId`, `dueDate`)",
            "CREATE INDEX IF NOT EXISTS `index_tasks_accountId_parentId` ON `tasks` (`accountId`, `parentId`)",
        ),
        instantColumns = listOf("completedAt", "remoteUpdated", "updatedAt"),
    ),
    InstantTable(
        name = "task_lists",
        createSql = "CREATE TABLE IF NOT EXISTS `task_lists_new` (`accountId` TEXT NOT NULL, `googleId` TEXT NOT NULL, " +
            "`title` TEXT NOT NULL, `isDefault` INTEGER NOT NULL, `etag` TEXT, `remoteUpdated` INTEGER, " +
            "PRIMARY KEY(`accountId`, `googleId`))",
        indices = emptyList(),
        instantColumns = listOf("remoteUpdated"),
    ),
    InstantTable(
        name = "alarms",
        createSql = "CREATE TABLE IF NOT EXISTS `alarms_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`accountId` TEXT NOT NULL, `itemType` TEXT NOT NULL, `itemLocalId` TEXT NOT NULL, `mode` TEXT NOT NULL, " +
            "`offsetMinutes` INTEGER, `localTime` TEXT, `triggerAt` INTEGER, `scheduled` INTEGER NOT NULL, " +
            "`snoozedUntil` INTEGER, `dismissedTriggerAt` INTEGER, `soundUri` TEXT, `vibrate` INTEGER NOT NULL)",
        indices = listOf(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_alarms_accountId_itemType_itemLocalId` ON `alarms` " +
                "(`accountId`, `itemType`, `itemLocalId`)",
            "CREATE INDEX IF NOT EXISTS `index_alarms_triggerAt` ON `alarms` (`triggerAt`)",
        ),
        instantColumns = listOf("triggerAt", "snoozedUntil", "dismissedTriggerAt"),
    ),
    InstantTable(
        name = "outbox",
        createSql = "CREATE TABLE IF NOT EXISTS `outbox_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`accountId` TEXT NOT NULL, `itemType` TEXT NOT NULL, `itemLocalId` TEXT NOT NULL, " +
            "`operation` TEXT NOT NULL, `changedFields` TEXT NOT NULL, `state` TEXT NOT NULL, " +
            "`attempts` INTEGER NOT NULL, `nextAttemptAt` INTEGER, `lastError` TEXT, `createdAt` INTEGER NOT NULL)",
        indices = listOf(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_outbox_accountId_itemType_itemLocalId` ON `outbox` " +
                "(`accountId`, `itemType`, `itemLocalId`)",
        ),
        instantColumns = listOf("nextAttemptAt", "createdAt"),
    ),
)

/**
 * v1 stored every Instant as ISO-8601 text; v2 stores epoch milliseconds. SQLite cannot change a
 * column type in place, so each table is rebuilt. The copy keeps the ISO text (an INTEGER column
 * leaves non-numeric text untouched) and each distinct value is then parsed here, which is exact
 * for any fraction length that Instant.toString() produced.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        for (table in instantTables) {
            val name = table.name
            db.execSQL(table.createSql)
            db.execSQL("INSERT INTO `${name}_new` SELECT * FROM `$name`")
            db.execSQL("DROP TABLE `$name`")
            db.execSQL("ALTER TABLE `${name}_new` RENAME TO `$name`")
            for (column in table.instantColumns) {
                val texts = mutableListOf<String>()
                db.query("SELECT DISTINCT `$column` FROM `$name` WHERE typeof(`$column`) = 'text'").use { cursor ->
                    while (cursor.moveToNext()) texts += cursor.getString(0)
                }
                for (text in texts) {
                    db.execSQL(
                        "UPDATE `$name` SET `$column` = ? WHERE `$column` = ?",
                        arrayOf<Any>(Instant.parse(text).toEpochMilli(), text),
                    )
                }
            }
            table.indices.forEach(db::execSQL)
        }
    }
}
