package com.staroscky.performance.sugestoes.domain

import java.util.UUID

data class Sugestao(
    val idSugestao: UUID,
    val apelido: String?,
    val dadosDestino: DadosDestino
)
