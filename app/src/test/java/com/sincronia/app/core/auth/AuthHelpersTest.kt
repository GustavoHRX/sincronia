package com.sincronia.app.core.auth

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthHelpersTest {
    private fun token(payload: String): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        return listOf("""{"alg":"RS256"}""", payload, "signature")
            .joinToString(".") { encoder.encodeToString(it.toByteArray()) }
    }

    @Test fun `sub comes from the token payload`() {
        assertEquals("1234567890", IdTokenClaims.subject(token("""{"sub":"1234567890","email":"x@y.z"}""")))
    }

    @Test fun `malformed or empty tokens give no sub`() {
        assertNull(IdTokenClaims.subject("not-a-jwt"))
        assertNull(IdTokenClaims.subject(token("""{"email":"x@y.z"}""")))
        assertNull(IdTokenClaims.subject("a.%%%.c"))
    }

    @Test fun `unchecked scope is reported as missing`() {
        assertTrue(GoogleScopes.missing(GoogleScopes.ALL).isEmpty())
        assertEquals(setOf(GoogleScopes.TASKS), GoogleScopes.missing(listOf(GoogleScopes.CALENDAR, "openid")))
    }
}
