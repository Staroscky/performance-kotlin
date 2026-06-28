package com.staroscky.performance.sugestoes.integration

data class InteligenciaResponse(
    val data: List<InteligenciaItemDto>? = null
)

data class InteligenciaItemDto(
    val idSugestao: String? = null,
    val chave: String? = null,
    val apelido: String? = null,
    val ag: String? = null,
    val cc: String? = null,
    val dac: String? = null,
    val tipoConta: String? = null
)
