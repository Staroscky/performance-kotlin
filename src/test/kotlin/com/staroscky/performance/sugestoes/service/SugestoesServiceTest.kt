package com.staroscky.performance.sugestoes.service

import com.staroscky.performance.core.UsuarioContext
import com.staroscky.performance.sugestoes.integration.InteligenciaClient
import com.staroscky.performance.sugestoes.integration.InteligenciaMapper
import com.staroscky.performance.sugestoes.integration.InteligenciaResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import java.util.UUID

class SugestoesServiceTest {

    private val client = mock<InteligenciaClient>()
    private val mapper = mock<InteligenciaMapper>()
    private val redisTemplate = mock<RedisTemplate<String, Any>>()
    private val opsForValue = mock<ValueOperations<String, Any>>()
    private val usuarioContext = mock<UsuarioContext>()

    private val service = SugestoesService(client, mapper, redisTemplate, usuarioContext)

    private val idPessoa = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
    private val idConta = UUID.fromString("660e8400-e29b-41d4-a716-446655440001")
    private val cacheKey = "sugestoes:$idPessoa"

    init {
        whenever(redisTemplate.opsForValue()).thenReturn(opsForValue)
        whenever(usuarioContext.idPessoa).thenReturn(idPessoa)
        whenever(usuarioContext.idConta).thenReturn(idConta)
    }

    @Test
    fun `deve retornar cache quando ja existir no Valkey`() {
        whenever(opsForValue.get(cacheKey)).thenReturn(listOf<Any>())

        val resultado = service.buscar()

        assertThat(resultado).isEmpty()
        verify(client, never()).buscar(any(), any())
    }

    @Test
    fun `deve salvar resultado no Valkey com TTL 15 minutos`() {
        whenever(opsForValue.get(cacheKey)).thenReturn(null)
        whenever(client.buscar(any(), any())).thenReturn(InteligenciaResponse(data = emptyList()))

        service.buscar()

        verify(opsForValue).set(
            eq(cacheKey),
            any(),
            eq(Duration.ofMinutes(15))
        )
    }
}
