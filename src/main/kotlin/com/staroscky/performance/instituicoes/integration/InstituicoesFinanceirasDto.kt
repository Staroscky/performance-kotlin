package com.staroscky.performance.instituicoes.integration

import com.fasterxml.jackson.annotation.JsonProperty

data class InstituicoesFinanceirasResponse(
    val data: List<InstituicaoFinanceiraDto>? = null
)

data class InstituicaoFinanceiraDto(
    val ispb: String? = null,
    @JsonProperty("nome_fantasia") val nomeFantasia: String? = null,
    @JsonProperty("nome_reduzido") val nomeReduzido: String? = null
)
