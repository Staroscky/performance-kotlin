package com.staroscky.performance.contatos.integration

import com.fasterxml.jackson.annotation.JsonProperty

data class ContatosCoreResponse(
    val data: List<ContatoCoreDto>? = null,
    @JsonProperty("x-next-cursor") val xNextCursor: String? = null
)

data class ContatoCoreDto(
    val idDestino: String? = null,
    val idContato: String? = null,
    val nome: String? = null,
    val apelido: String? = null,
    val dadosDestino: DadosDestinoDto? = null
)

data class DadosDestinoDto(
    val chave: String? = null,
    val ag: String? = null,
    val conta: String? = null,
    val dac: String? = null,
    val tipoConta: String? = null
)
