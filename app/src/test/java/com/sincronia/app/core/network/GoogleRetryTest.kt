package com.sincronia.app.core.network

import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class GoogleRetryTest {
    private fun http(code: Int, reason: String? = null) = HttpException(Response.error<Any>(code,
        (reason?.let { """{"error":{"code":$code,"errors":[{"reason":"$it"}]}}""" } ?: "{}").toResponseBody()))

    @Test fun `transient and final errors follow spec 7_2`() {
        assertTrue(isTransient(IOException()))
        assertTrue(isTransient(http(503)))
        assertTrue(isTransient(http(429)))
        assertTrue(isTransient(http(403, "userRateLimitExceeded")))
        assertFalse(isTransient(http(403, "insufficientPermissions")))
        assertFalse(isTransient(http(400)))
        assertFalse(isTransient(http(410)))
    }

    @Test fun `retries 3 times with 1, 2 and 4 seconds and then gives up`() {
        val waits = mutableListOf<Long>()
        var calls = 0
        val result = runCatching {
            runBlocking { withGoogleRetry(sleep = { waits += it }) { calls++; throw http(503) } }
        }
        assertTrue(result.isFailure)
        assertEquals(4, calls)
        assertEquals(listOf(1_000L, 2_000L, 4_000L), waits)
    }

    @Test fun `final error is not repeated and a later success is returned`() = runBlocking {
        var calls = 0
        runCatching { withGoogleRetry(sleep = {}) { calls++; throw http(400) } }
        assertEquals(1, calls)
        var attempts = 0
        val value = withGoogleRetry(sleep = {}) { if (attempts++ == 0) throw IOException() else "ok" }
        assertEquals("ok", value)
    }
}
