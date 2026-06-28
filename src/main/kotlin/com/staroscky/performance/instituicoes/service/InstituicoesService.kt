package com.staroscky.performance.instituicoes.service

import com.staroscky.performance.instituicoes.domain.Instituicao
import com.staroscky.performance.instituicoes.integration.InstituicoesFinanceirasClient
import com.staroscky.performance.instituicoes.integration.InstituicoesMapper
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class InstituicoesService(
    private val client: InstituicoesFinanceirasClient,
    private val mapper: InstituicoesMapper
) {
    @Cacheable("instituicoes")
    fun buscar(): List<Instituicao> =
        client.buscar().data.orEmpty().mapNotNull(mapper::toInstituicao)
}
