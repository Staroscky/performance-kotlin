# Kotlin + Spring com mockito-kotlin

Imports do wrapper:

```kotlin
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.times
```

## Setup básico

```kotlin
class AvaliacaoServiceTest {

    private val gateway: SaldoGateway = mock()
    private val service = AvaliacaoService(gateway)

    @Test
    fun `deve permitir quando saldo cobre o valor`() {
        // Arrange
        whenever(gateway.consultar(any())).thenReturn(Saldo(centavos = 10_000))

        // Act
        val resultado = service.avaliar(Comando(valorCentavos = 5_000))

        // Assert
        assertTrue(resultado is ResultadoAvaliacao.Permitida)
    }
}
```

## Verificando interação

```kotlin
@Test
fun `deve consultar o gateway exatamente uma vez`() {
    whenever(gateway.consultar(any())).thenReturn(Saldo(0))

    service.avaliar(Comando(valorCentavos = 1))

    verify(gateway, times(1)).consultar(any())
}
```

## Capturando argumento

```kotlin
@Test
fun `deve consultar com o id do comando`() {
    val captor = argumentCaptor<String>()
    whenever(gateway.consultar(captor.capture())).thenReturn(Saldo(0))

    service.avaliar(Comando(id = "abc", valorCentavos = 1))

    assertEquals("abc", captor.firstValue)
}
```

## Resultado sealed

`when` exaustivo também no teste deixa claro qual variante é esperada:

```kotlin
val r = service.avaliar(comando)
assertTrue(r is ResultadoAvaliacao.NaoPermitida)
assertEquals(Motivo.SALDO_INSUFICIENTE, (r as ResultadoAvaliacao.NaoPermitida).motivo)
```

## Integração (quando aplicável)

Para slices que batem em upstream, o padrão do projeto é **WireMock + JSONAssert**
em teste de integração separado — não misture com o unitário. Mantenha o unitário
rápido e sem rede.
