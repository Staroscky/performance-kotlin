package com.staroscky.performance.instituicoes.integration

import com.staroscky.performance.instituicoes.config.InstituicoesConfigProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InstituicoesMapperTest {

    private val config = InstituicoesConfigProperties(
        items = listOf(
            InstituicoesConfigProperties.InstituicaoConfig(
                ispb = "12345678",
                nome = "Override",
                icone = "ids_xxx"
            )
        )
    )
    private val mapper = InstituicoesMapper(config)

    @Test
    fun `deve retornar nome e icone do config quando ISPB configurado`() {
        val dto = InstituicaoFinanceiraDto(ispb = "12345678", nomeReduzido = "Original")

        val resultado = mapper.toInstituicao(dto)

        assertThat(resultado?.nome).isEqualTo("Override")
        assertThat(resultado?.iconeUrl).isEqualTo("ids_xxx")
    }

    @Test
    fun `deve usar nome_reduzido quando ISPB sem override`() {
        val dto = InstituicaoFinanceiraDto(ispb = "99999999", nomeReduzido = "XPTO")

        val resultado = mapper.toInstituicao(dto)

        assertThat(resultado?.nome).isEqualTo("XPTO")
        assertThat(resultado?.iconeUrl).isNull()
    }

    @Test
    fun `deve retornar iconeUrl null quando sem icone configurado`() {
        val configSemIcone = InstituicoesConfigProperties(
            items = listOf(
                InstituicoesConfigProperties.InstituicaoConfig(
                    ispb = "12345678",
                    nome = "Override",
                    icone = null
                )
            )
        )
        val mapperSemIcone = InstituicoesMapper(configSemIcone)
        val dto = InstituicaoFinanceiraDto(ispb = "12345678", nomeReduzido = "Original")

        val resultado = mapperSemIcone.toInstituicao(dto)

        assertThat(resultado?.iconeUrl).isNull()
    }

    @Test
    fun `deve descartar item sem ispb`() {
        val dto = InstituicaoFinanceiraDto(ispb = null, nomeReduzido = "Sem ISPB")

        val resultado = mapper.toInstituicao(dto)

        assertThat(resultado).isNull()
    }
}
