package com.staroscky.performance.core.auth

import org.assertj.core.api.Assertions.assertThat
import org.cache2k.CacheManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AuthTokenServiceTest {

    private val stsProperties = StsProperties(
        url = "http://sts-test",
        client = StsProperties.Client(id = "client-id", secret = "client-secret"),
        appId = "app-id"
    )

    @AfterEach
    fun limpar() {
        CacheManager.getInstance().getCache<String, String>("auth-token").close()
    }

    @Test
    fun `deve chamar StsClient quando cache estiver vazio`() {
        val stsClient = mock<StsClient>()
        whenever(stsClient.authenticate(any(), any(), any()))
            .thenReturn(StsTokenResponse(accessToken = "token-abc", expiresIn = 3600))
        val service = AuthTokenService(stsProperties, stsClient)

        service.getToken()

        verify(stsClient, times(1)).authenticate(any(), any(), any())
    }

    @Test
    fun `deve retornar token cacheado na segunda chamada sem acionar StsClient`() {
        val stsClient = mock<StsClient>()
        whenever(stsClient.authenticate(any(), any(), any()))
            .thenReturn(StsTokenResponse(accessToken = "token-abc", expiresIn = 3600))
        val service = AuthTokenService(stsProperties, stsClient)

        service.getToken()
        val token = service.getToken()

        assertThat(token).isEqualTo("token-abc")
        verify(stsClient, times(1)).authenticate(any(), any(), any())
    }

    @Test
    fun `deve renovar token quando cache expirar`() {
        val stsClient = mock<StsClient>()
        whenever(stsClient.authenticate(any(), any(), any()))
            .thenReturn(StsTokenResponse(accessToken = "token-renovado", expiresIn = 31))
        val service = AuthTokenService(stsProperties, stsClient)

        service.getToken()
        Thread.sleep(1500)
        val token = service.getToken()

        assertThat(token).isEqualTo("token-renovado")
        verify(stsClient, times(2)).authenticate(any(), any(), any())
    }
}
