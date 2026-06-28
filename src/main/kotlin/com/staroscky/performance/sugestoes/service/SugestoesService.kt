package com.staroscky.performance.sugestoes.service

import com.staroscky.performance.core.UsuarioContext
import com.staroscky.performance.sugestoes.domain.Sugestao
import com.staroscky.performance.sugestoes.integration.InteligenciaClient
import com.staroscky.performance.sugestoes.integration.InteligenciaMapper
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class SugestoesService(
    private val client: InteligenciaClient,
    private val mapper: InteligenciaMapper,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val usuarioContext: UsuarioContext
) {
    fun buscar(): List<Sugestao> {
        val key = "sugestoes:${usuarioContext.idPessoa}"
        @Suppress("UNCHECKED_CAST")
        val cached = redisTemplate.opsForValue().get(key) as? List<Sugestao>
        if (cached != null) return cached

        val result = client.buscar(
            tela = "sugestao-transferencia",
            cliente = "${usuarioContext.idPessoa}-${usuarioContext.idConta}"
        ).data.orEmpty().mapNotNull(mapper::toSugestao)

        redisTemplate.opsForValue().set(key, result, Duration.ofMinutes(15))
        return result
    }
}
