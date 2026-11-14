package com.sincronia.app.core.network

import java.io.IOException
import kotlinx.coroutines.delay
import retrofit2.HttpException

/** Spec 7.2: network failure, 5xx, 429 and 403 rate limits are transient; everything else is final. */
fun isTransient(error: Throwable): Boolean = when (error) {
    is IOException -> true
    is HttpException -> when (val code = error.code()) {
        429 -> true
        403 -> googleReason(error) in setOf("rateLimitExceeded", "userRateLimitExceeded")
        else -> code >= 500
    }
    else -> false
}

fun googleReason(error: HttpException): String? = runCatching {
    error.response()?.errorBody()?.string()?.let { googleJson.decodeFromString(GoogleErrorDto.serializer(), it).reason }
}.getOrNull()

/** Repeats a request up to 3 times (1 s, 2 s, 4 s); after that the error goes to the next sync. */
suspend fun <T> withGoogleRetry(
    delaysMillis: List<Long> = listOf(1_000, 2_000, 4_000),
    sleep: suspend (Long) -> Unit = { delay(it) },
    block: suspend () -> T,
): T {
    for (wait in delaysMillis) {
        try {
            return block()
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException || !isTransient(error)) throw error
        }
        sleep(wait)
    }
    return block()
}
