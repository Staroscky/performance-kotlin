package com.staroscky.performance.core.auth

import org.cache2k.Cache
import org.cache2k.Cache2kBuilder
import org.springframework.stereotype.Service

@Service
class AuthTokenService(
    private val stsProperties: StsProperties,
    private val stsClient: StsClient
) {
    private val cache: Cache<String, String> = Cache2kBuilder
        .of(String::class.java, String::class.java)
        .name("auth-token")
        .eternal(false)
        .build()

    fun getToken(): String = cache.get("token") ?: fetchAndCache()

    private fun fetchAndCache(): String {
        val response = stsClient.authenticate(
            clientId = stsProperties.client.id,
            clientSecret = stsProperties.client.secret
        )
        val expiryMs = System.currentTimeMillis() + (response.expiresIn - 30) * 1000L
        cache.invoke("token") {
            it.value = response.accessToken
            it.setExpiryTime(expiryMs)
        }
        return response.accessToken
    }
}
