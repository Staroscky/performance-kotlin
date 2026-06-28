package com.staroscky.performance.instituicoes.integration

import com.staroscky.performance.instituicoes.config.InstituicoesConfigProperties
import com.staroscky.performance.instituicoes.domain.Instituicao
import org.springframework.stereotype.Component

@Component
class InstituicoesMapper(private val config: InstituicoesConfigProperties) {

    fun toInstituicao(dto: InstituicaoFinanceiraDto): Instituicao? {
        val ispb = dto.ispb ?: return null
        val nomeReduzido = dto.nomeReduzido ?: return null
        val override = config.items.find { it.ispb == ispb }
        val nome = override?.nome ?: nomeReduzido
        val searchable = "${dto.nomeFantasia ?: nome}#${nome}#${ispb}"
        return Instituicao(
            iconeUrl = override?.icone,
            ispb = ispb,
            nome = nome,
            searchable = searchable
        )
    }
}
