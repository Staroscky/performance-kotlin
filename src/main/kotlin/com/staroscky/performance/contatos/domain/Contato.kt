package com.staroscky.performance.contatos.domain

import java.util.UUID

data class Contato(
    val idDestino: UUID,
    val idContato: UUID,
    val nome: String,
    val apelido: String?,
    val dadosDestino: DadosDestino
)
