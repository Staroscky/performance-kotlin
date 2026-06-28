package com.staroscky.performance.contatos.service

import com.staroscky.performance.contatos.domain.Contato
import com.staroscky.performance.contatos.integration.ContatoCoreDto
import com.staroscky.performance.contatos.integration.ContatosCoreClient
import com.staroscky.performance.contatos.integration.ContatosMapper
import com.staroscky.performance.core.UsuarioContext
import org.springframework.stereotype.Service

@Service
class ContatosService(
    private val client: ContatosCoreClient,
    private val mapper: ContatosMapper,
    private val usuarioContext: UsuarioContext
) {
    fun buscar(): List<Contato> {
        val acumulados = mutableListOf<ContatoCoreDto>()
        var cursor: String? = null
        do {
            val response = client.buscar(
                idPessoa = usuarioContext.idPessoa.toString(),
                idConta = usuarioContext.idConta.toString(),
                cursor = cursor
            )
            acumulados += response.data.orEmpty()
            cursor = response.xNextCursor?.takeIf { it.isNotEmpty() }
        } while (cursor != null)
        return acumulados.mapNotNull(mapper::toContato)
    }
}
