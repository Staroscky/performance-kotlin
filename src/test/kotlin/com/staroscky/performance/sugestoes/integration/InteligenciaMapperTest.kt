package com.staroscky.performance.sugestoes.integration

import com.staroscky.performance.sugestoes.domain.DadosDestino
import com.staroscky.performance.sugestoes.domain.TipoConta
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InteligenciaMapperTest {

    private val mapper = InteligenciaMapper()

    @Test
    fun `deve retornar sugestoes com ChavePix quando chave presente`() {
        val dto = InteligenciaItemDto(
            idSugestao = "550e8400-e29b-41d4-a716-446655440000",
            chave = "chave@test.com"
        )

        val resultado = mapper.toSugestao(dto)

        assertThat(resultado).isNotNull
        assertThat(resultado!!.dadosDestino).isEqualTo(DadosDestino.ChavePix(chave = "chave@test.com"))
    }

    @Test
    fun `deve retornar sugestoes com AgenciaConta quando ag e cc presentes`() {
        val dto = InteligenciaItemDto(
            idSugestao = "550e8400-e29b-41d4-a716-446655440000",
            ag = "0001",
            cc = "12345",
            dac = "6",
            tipoConta = "C"
        )

        val resultado = mapper.toSugestao(dto)

        assertThat(resultado).isNotNull
        assertThat(resultado!!.dadosDestino).isEqualTo(
            DadosDestino.AgenciaConta(ag = "0001", cc = "12345", dac = "6", tipoConta = TipoConta.C)
        )
    }

    @Test
    fun `deve descartar item quando idSugestao nulo`() {
        val dto = InteligenciaItemDto(
            idSugestao = null,
            chave = "chave@test.com"
        )

        val resultado = mapper.toSugestao(dto)

        assertThat(resultado).isNull()
    }

    @Test
    fun `deve descartar item quando dados de destino insuficientes`() {
        val dto = InteligenciaItemDto(
            idSugestao = "550e8400-e29b-41d4-a716-446655440000"
        )

        val resultado = mapper.toSugestao(dto)

        assertThat(resultado).isNull()
    }
}
