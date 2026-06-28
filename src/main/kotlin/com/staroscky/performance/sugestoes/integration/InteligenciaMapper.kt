package com.staroscky.performance.sugestoes.integration

import com.staroscky.performance.sugestoes.domain.DadosDestino
import com.staroscky.performance.sugestoes.domain.Sugestao
import com.staroscky.performance.sugestoes.domain.TipoConta
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class InteligenciaMapper {

    fun toSugestao(dto: InteligenciaItemDto): Sugestao? {
        val idSugestao = dto.idSugestao?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: return null.also { log.warn("sugestao descartada: idSugestao nulo ou invalido") }

        val dadosDestino = when {
            dto.chave != null -> DadosDestino.ChavePix(dto.chave)
            dto.ag != null && dto.cc != null && dto.dac != null && dto.tipoConta != null ->
                DadosDestino.AgenciaConta(
                    ag = dto.ag,
                    cc = dto.cc,
                    dac = dto.dac,
                    tipoConta = runCatching { TipoConta.valueOf(dto.tipoConta) }.getOrElse {
                        return null.also { log.warn("sugestao descartada: tipoConta invalido={}", dto.tipoConta) }
                    }
                )
            else -> return null.also { log.warn("sugestao descartada: dados de destino insuficientes") }
        }

        return Sugestao(idSugestao = idSugestao, apelido = dto.apelido, dadosDestino = dadosDestino)
    }

    companion object {
        private val log = LoggerFactory.getLogger(InteligenciaMapper::class.java)
    }
}
