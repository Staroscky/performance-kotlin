package com.staroscky.performance.contatos.integration

import com.staroscky.performance.contatos.domain.DadosDestino
import com.staroscky.performance.contatos.domain.TipoConta
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class ContatosMapperTest {

    private val mapper = ContatosMapper()

    private val idDestinoValido = UUID.randomUUID().toString()
    private val idContatoValido = UUID.randomUUID().toString()

    @Test
    fun `deve mapear DadosDestino como ChavePix quando chave presente`() {
        val dto = ContatoCoreDto(
            idDestino = idDestinoValido,
            idContato = idContatoValido,
            nome = "Fulano",
            dadosDestino = DadosDestinoDto(chave = "chave@pix.com")
        )

        val resultado = mapper.toContato(dto)

        assertThat(resultado).isNotNull
        assertThat(resultado!!.dadosDestino).isInstanceOf(DadosDestino.ChavePix::class.java)
        assertThat((resultado.dadosDestino as DadosDestino.ChavePix).chave).isEqualTo("chave@pix.com")
    }

    @Test
    fun `deve mapear DadosDestino como AgenciaConta quando ag e conta presentes`() {
        val dto = ContatoCoreDto(
            idDestino = idDestinoValido,
            idContato = idContatoValido,
            nome = "Fulano",
            dadosDestino = DadosDestinoDto(ag = "0001", conta = "12345", dac = "6", tipoConta = "C")
        )

        val resultado = mapper.toContato(dto)

        assertThat(resultado).isNotNull
        assertThat(resultado!!.dadosDestino).isInstanceOf(DadosDestino.AgenciaConta::class.java)
        val agenciaConta = resultado.dadosDestino as DadosDestino.AgenciaConta
        assertThat(agenciaConta.ag).isEqualTo("0001")
        assertThat(agenciaConta.conta).isEqualTo("12345")
        assertThat(agenciaConta.dac).isEqualTo("6")
        assertThat(agenciaConta.tipoConta).isEqualTo(TipoConta.C)
    }

    @Test
    fun `deve descartar contato com idDestino nulo`() {
        val dto = ContatoCoreDto(
            idDestino = null,
            idContato = idContatoValido,
            nome = "Fulano",
            dadosDestino = DadosDestinoDto(chave = "chave@pix.com")
        )

        val resultado = mapper.toContato(dto)

        assertThat(resultado).isNull()
    }
}
