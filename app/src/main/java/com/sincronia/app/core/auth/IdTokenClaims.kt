package com.sincronia.app.core.auth

import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Reads the `sub` claim locally. The token came straight from Credential Manager, so it is only
 * decoded here, not verified (there is no backend that would need to trust it).
 */
object IdTokenClaims {
    fun subject(idToken: String): String? = runCatching {
        val payload = idToken.split('.').getOrNull(1) ?: return null
        val json = String(Base64.getUrlDecoder().decode(payload.trimEnd('=')), Charsets.UTF_8)
        Json.parseToJsonElement(json).jsonObject["sub"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
    }.getOrNull()
}
