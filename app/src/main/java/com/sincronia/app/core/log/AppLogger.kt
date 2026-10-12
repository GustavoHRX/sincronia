package com.sincronia.app.core.log

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Log do app: Logcat + arquivo local (`files/logs/app.log`), usado depois pelo
 * "Exportar diagnóstico". Nunca registrar token, e-mail nem título de evento ou tarefa;
 * use ids locais e contagens.
 */
@Singleton
class AppLogger @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val writer = LogFileWriter(File(context.filesDir, "logs/app.log"))
    private val io = Executors.newSingleThreadExecutor()
    private val timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    fun d(tag: String, message: String) = log(Log.DEBUG, "D", tag, message, null)
    fun i(tag: String, message: String) = log(Log.INFO, "I", tag, message, null)
    fun w(tag: String, message: String, error: Throwable? = null) = log(Log.WARN, "W", tag, message, error)
    fun e(tag: String, message: String, error: Throwable? = null) = log(Log.ERROR, "E", tag, message, error)

    private fun log(priority: Int, level: String, tag: String, message: String, error: Throwable?) {
        val text = if (error != null) "$message: ${error.javaClass.simpleName}: ${error.message}" else message
        Log.println(priority, "SincronIA/$tag", text)
        val line = "${LocalDateTime.now().format(timestamp)} $level/$tag: $text"
        io.execute {
            runCatching { writer.append(line) }
                .onFailure { Log.w("SincronIA/AppLogger", "Falha ao gravar o log em arquivo", it) }
        }
    }
}
