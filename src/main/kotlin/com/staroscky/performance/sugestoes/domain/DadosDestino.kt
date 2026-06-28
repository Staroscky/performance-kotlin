package com.staroscky.performance.sugestoes.domain

sealed interface DadosDestino {
    data class ChavePix(val chave: String) : DadosDestino
    data class AgenciaConta(
        val ag: String,
        val cc: String,
        val dac: String,
        val tipoConta: TipoConta
    ) : DadosDestino
}
