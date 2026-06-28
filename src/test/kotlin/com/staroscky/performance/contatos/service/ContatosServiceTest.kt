package com.staroscky.performance.contatos.service

import com.staroscky.performance.contatos.domain.Contato
import com.staroscky.performance.contatos.domain.DadosDestino
import com.staroscky.performance.contatos.integration.ContatoCoreDto
import com.staroscky.performance.contatos.integration.ContatosCoreClient
import com.staroscky.performance.contatos.integration.ContatosCoreResponse
import com.staroscky.performance.contatos.integration.ContatosMapper
import com.staroscky.performance.core.UsuarioContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class ContatosServiceTest {

    private val client = mock<ContatosCoreClient>()
    private val mapper = mock<ContatosMapper>()
    private val usuarioContext = mock<UsuarioContext>()
    private val service = ContatosService(client, mapper, usuarioContext)

    @BeforeEach
    fun setup() {
        whenever(usuarioContext.idPessoa).thenReturn(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
        whenever(usuarioContext.idConta).thenReturn(UUID.fromString("660e8400-e29b-41d4-a716-446655440001"))
    }

    @Test
    fun `deve percorrer todas as paginas ate cursor nulo`() {
        whenever(client.buscar(any(), any(), isNull()))
            .thenReturn(ContatosCoreResponse(data = emptyList(), xNextCursor = "abc"))
        whenever(client.buscar(any(), any(), eq("abc")))
            .thenReturn(ContatosCoreResponse(data = emptyList(), xNextCursor = null))

        service.buscar()

        verify(client).buscar(any(), any(), isNull())
        verify(client).buscar(any(), any(), eq("abc"))
    }

    @Test
    fun `deve percorrer todas as paginas ate cursor vazio`() {
        whenever(client.buscar(any(), any(), isNull()))
            .thenReturn(ContatosCoreResponse(data = emptyList(), xNextCursor = "abc"))
        whenever(client.buscar(any(), any(), eq("abc")))
            .thenReturn(ContatosCoreResponse(data = emptyList(), xNextCursor = ""))

        service.buscar()

        verify(client).buscar(any(), any(), isNull())
        verify(client).buscar(any(), any(), eq("abc"))
    }

    @Test
    fun `deve retornar lista agregada de todas as paginas`() {
        val dto1 = ContatoCoreDto(idDestino = UUID.randomUUID().toString())
        val dto2 = ContatoCoreDto(idDestino = UUID.randomUUID().toString())
        whenever(client.buscar(any(), any(), isNull()))
            .thenReturn(ContatosCoreResponse(data = listOf(dto1), xNextCursor = "cursor1"))
        whenever(client.buscar(any(), any(), eq("cursor1")))
            .thenReturn(ContatosCoreResponse(data = listOf(dto2), xNextCursor = null))
        whenever(mapper.toContato(any())).thenAnswer { criarContatoDeTeste() }

        val resultado = service.buscar()

        assertThat(resultado).hasSize(2)
    }

    private fun criarContatoDeTeste(): Contato = Contato(
        idDestino = UUID.randomUUID(),
        idContato = UUID.randomUUID(),
        nome = "Teste",
        apelido = null,
        dadosDestino = DadosDestino.ChavePix("chave@teste.com")
    )
}
