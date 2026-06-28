package com.staroscky.performance.contatos.integration

import com.staroscky.performance.contatos.domain.Contato
import com.staroscky.performance.contatos.domain.DadosDestino
import com.staroscky.performance.contatos.domain.TipoConta
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ContatosMapper {

    fun toContato(dto: ContatoCoreDto): Contato? {
        val idDestino = dto.idDestino?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: return null.also { log.warn("contato descartado: idDestino nulo ou invalido") }
        val idContato = dto.idContato?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: return null.also { log.warn("contato descartado: idContato nulo ou invalido") }
        val nome = dto.nome
            ?: return null.also { log.warn("contato descartado: nome nulo") }

        val dd = dto.dadosDestino
        val dadosDestino = when {
            dd?.chave != null -> DadosDestino.ChavePix(dd.chave)
            dd?.ag != null && dd.conta != null && dd.dac != null && dd.tipoConta != null ->
                DadosDestino.AgenciaConta(
                    ag = dd.ag,
                    conta = dd.conta,
                    dac = dd.dac,
                    tipoConta = runCatching { TipoConta.valueOf(dd.tipoConta) }.getOrElse {
                        return null.also { log.warn("contato descartado: tipoConta invalido={}", dd.tipoConta) }
                    }
                )
            else -> return null.also { log.warn("contato descartado: dadosDestino insuficientes") }
        }

        return Contato(
            idDestino = idDestino,
            idContato = idContato,
            nome = nome,
            apelido = dto.apelido,
            dadosDestino = dadosDestino
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(ContatosMapper::class.java)
    }
}
