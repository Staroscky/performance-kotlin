package com.staroscky.performance.contatos.domain

sealed interface DadosDestino {
    data class ChavePix(val chave: String) : DadosDestino
    data class AgenciaConta(
        val ag: String,
        val conta: String,
        val dac: String,
        val tipoConta: TipoConta
    ) : DadosDestino
}
