package com.staroscky.performance.instituicoes.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("instituicoes-config")
data class InstituicoesConfigProperties(
    val items: List<InstituicaoConfig> = emptyList()
) {
    data class InstituicaoConfig(
        val ispb: String,
        val nome: String?,
        val icone: String?
    )
}
