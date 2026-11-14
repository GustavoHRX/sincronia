package com.sincronia.app.core.network

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Token source for the HTTP layer, implemented by GoogleAuthManager (spec 6.4, steps 4 and 5). */
interface AccessTokenProvider {
    /** Token kept in memory, or null before the first authorization of this process. */
    fun currentToken(): String?

    /** Drops [rejected] and authorizes again silently; null when consent is needed. */
    suspend fun refresh(rejected: String?): String?
}

val googleJson = Json { ignoreUnknownKeys = true; explicitNulls = false }

/** Adds `Authorization: Bearer`. Never logs the token. */
class BearerInterceptor(private val tokens: AccessTokenProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokens.currentToken() ?: return chain.proceed(chain.request())
        return chain.proceed(chain.request().newBuilder().header("Authorization", "Bearer $token").build())
    }
}

/** 401: clearToken, authorize again and repeat the request only once. */
class TokenAuthenticator(private val tokens: AccessTokenProvider) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return null
        val rejected = response.request.header("Authorization")?.removePrefix("Bearer ")
        val fresh = runBlocking { tokens.refresh(rejected) } ?: return null
        return response.request.newBuilder().header("Authorization", "Bearer $fresh").build()
    }
}

object GoogleHttp {
    const val BASE_URL = "https://www.googleapis.com/"

    fun client(tokens: AccessTokenProvider): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(BearerInterceptor(tokens))
        .authenticator(TokenAuthenticator(tokens))
        .build()

    fun retrofit(client: OkHttpClient, baseUrl: String = BASE_URL): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(googleJson.asConverterFactory("application/json".toMediaType()))
        .build()
}
