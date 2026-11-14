package com.sincronia.app.core.log

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.sincronia.app.BuildConfig
import com.sincronia.app.core.database.AppDatabase
import com.sincronia.app.data.ActiveAccount
import com.sincronia.app.data.settings.SettingsRepository
import com.sincronia.app.ui.permissions.permissionStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Pure part of "Exportar diagnóstico": the text that leaves the phone (spec 12, no personal data). */
object DiagnosticsReport {
    private val email = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val bearer = Regex("(?i)bearer\\s+[A-Za-z0-9._~+/=-]+")
    private val googleToken = Regex("ya29\\.[A-Za-z0-9._-]+")

    /** Second line of defence: AppLogger never writes these, but a library message might. */
    fun redact(line: String): String = line
        .replace(email, "<e-mail>")
        .replace(bearer, "Bearer <token>")
        .replace(googleToken, "<token>")

    fun build(header: List<Pair<String, String>>, logLines: List<String>): String = buildString {
        appendLine("SincronIA · diagnóstico")
        header.forEach { (key, value) -> appendLine("$key: $value") }
        appendLine()
        appendLine("--- log (${logLines.size} linhas) ---")
        logLines.forEach { appendLine(redact(it)) }
    }
}

@Singleton
class DiagnosticsExporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val settings: SettingsRepository,
    private val account: ActiveAccount,
) {
    /** Writes the report to cache/diagnostics and returns a content:// URI for the share sheet. */
    suspend fun export(now: Instant = Instant.now()): Uri = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val accountId = withTimeoutOrNull(2_000) { account.id.firstOrNull() }
        val config = settings.current()
        val permissions = permissionStatus(context)
        val upcoming = accountId?.let { database.alarmDao().upcoming(it, now) }.orEmpty()
        val outbox = accountId?.let { database.outboxDao().blockingDownload(it) }.orEmpty()
        val time = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val header = listOf(
            "Gerado em" to now.atZone(zone).format(time),
            "Versão" to "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}, ${BuildConfig.BUILD_TYPE})",
            "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "Aparelho" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "Fuso" to zone.id,
            "Conta" to when (accountId) { null -> "nenhuma"; ActiveAccount.DEMO_ACCOUNT -> "demonstração"; else -> "Google" },
            "Última sync" to (settings.lastSyncAt.first()?.atZone(zone)?.format(time) ?: "nunca"),
            "Ajustes" to "alarme novo evento ${config.newEventAlarmMinutes} min, lembrete diário " +
                (if (config.dailyReminderEnabled) config.dailyReminderTime.toString() else "desligado") +
                ", soneca ${config.snoozeMinutes} min, sync ${config.syncIntervalMinutes} min, som " +
                (if (config.alarmSoundUri == null) "padrão" else "escolhido"),
            "Permissões" to "notificações ${permissions.notifications}, tela cheia ${permissions.fullScreen}, " +
                "alarmes exatos ${permissions.exactAlarms}",
            "Alarmes" to "${upcoming.size} futuros, ${database.alarmDao().scheduled().size} agendados no sistema, " +
                "próximo ${upcoming.firstOrNull()?.triggerAt?.atZone(zone)?.format(time) ?: "nenhum"}",
            "Fila" to outbox.groupingBy { it.state }.eachCount().entries.joinToString { "${it.key} ${it.value}" }.ifEmpty { "vazia" },
        )
        val logs = File(context.filesDir, "logs")
        val lines = listOf(File(logs, "app.log.1"), File(logs, "app.log"))
            .filter(File::exists)
            .flatMap { it.readLines() }
        val dir = File(context.cacheDir, "diagnostics").apply { mkdirs() }
        dir.listFiles()?.forEach(File::delete)
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(now.atZone(zone))
        val file = File(dir, "sincronia-diagnostico-$stamp.txt")
        file.writeText(DiagnosticsReport.build(header, lines))
        FileProvider.getUriForFile(context, "${context.packageName}.diagnostics", file)
    }
}
