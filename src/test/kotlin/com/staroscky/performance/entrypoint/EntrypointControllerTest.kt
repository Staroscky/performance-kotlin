package com.staroscky.performance.entrypoint

import com.staroscky.performance.core.caronte.CaronteItem
import com.staroscky.performance.core.caronte.CaronteRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class EntrypointControllerTest {

    private val registry = mock<CaronteRegistry>()
    private val controller = EntrypointController(registry)

    @Test
    fun `deve retornar lista de itens registrados no CaronteRegistry`() {
        val items = listOf(CaronteItem("instituicoes", "/v1/instituicoes"))
        whenever(registry.items).thenReturn(items)

        val result = controller.entrypoint()

        assertThat(result["data"]).isEqualTo(items)
    }

    @Test
    fun `deve retornar lista vazia quando nenhuma controller tem CaronteMapping`() {
        whenever(registry.items).thenReturn(emptyList())

        val result = controller.entrypoint()

        assertThat(result["data"]).isEmpty()
    }
}
