package com.staroscky.performance.core

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.mock.web.MockHttpServletRequest
import java.util.Base64
import java.util.UUID

class UsuarioContextTest {

    private fun criarJwt(claims: Map<String, String>): String {
        val header = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"typ":"JWT","alg":"none"}""".toByteArray())
        val payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(ObjectMapper().writeValueAsBytes(claims))
        return "$header.$payload."
    }

    @Test
    fun `deve extrair idPessoa e idConta de JWT valido`() {
        val idPessoa = "550e8400-e29b-41d4-a716-446655440000"
        val idConta = "660e8400-e29b-41d4-a716-446655440001"
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer ${criarJwt(mapOf("idPessoa" to idPessoa, "idConta" to idConta))}")

        val ctx = UsuarioContext(request)

        assertThat(ctx.idPessoa).isEqualTo(UUID.fromString(idPessoa))
        assertThat(ctx.idConta).isEqualTo(UUID.fromString(idConta))
    }

    @Test
    fun `deve lancar erro quando header Authorization ausente`() {
        val request = MockHttpServletRequest()

        assertThrows<IllegalStateException> {
            UsuarioContext(request)
        }
    }

    @Test
    fun `deve lancar erro quando claim idPessoa ausente no payload`() {
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer ${criarJwt(mapOf("idConta" to "660e8400-e29b-41d4-a716-446655440001"))}")

        assertThrows<IllegalStateException> {
            UsuarioContext(request)
        }
    }
}
