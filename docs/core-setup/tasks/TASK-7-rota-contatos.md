# TASK-7 — Rota GET /v1/contatos

**Arquivos alvo:** pacote `com.staroscky.performance.contatos` (novo)
**Referência SPEC:** Seções 5 (RF-18 a RF-22), 8.4 (Domínio de Contatos, paginação cursor), 10
**Depende de:** TASK-1, TASK-2 (UsuarioContext), TASK-3 (AuthRequestInterceptor)
**Bloqueada por:** nenhuma

---

## Contexto

GET /v1/contatos retorna todos os contatos do usuário consultando a integração `contatos-core` com paginação cursor. O service percorre todas as páginas até `x-next-cursor` ser nulo ou vazio, acumula os resultados e retorna a lista completa. Sem cache.

## O que fazer

### 1. `contatos/domain/TipoConta.kt`

```kotlin
enum class TipoConta { C, P, G }
```

### 2. `contatos/domain/DadosDestino.kt`

```kotlin
sealed interface DadosDestino {
    data class ChavePix(val chave: String) : DadosDestino
    data class AgenciaConta(
        val ag: String,
        val conta: String,
        val dac: String,
        val tipoConta: TipoConta
    ) : DadosDestino
}
```

> Nota: o campo é `conta` (não `cc` como em sugestões) — cada slice define seu próprio tipo para manter isolamento VSA.

### 3. `contatos/domain/Contato.kt`

```kotlin
data class Contato(
    val idDestino: UUID,
    val idContato: UUID,
    val nome: String,
    val apelido: String?,
    val dadosDestino: DadosDestino
)
```

### 4. `contatos/integration/ContatosCoreDto.kt`

```kotlin
data class ContatosCoreResponse(
    val data: List<ContatoCoreDto>? = null,
    @JsonProperty("x-next-cursor") val xNextCursor: String? = null
)

data class ContatoCoreDto(
    val idDestino: String? = null,
    val idContato: String? = null,
    val nome: String? = null,
    val apelido: String? = null,
    val dadosDestino: DadosDestinoDto? = null
)

data class DadosDestinoDto(
    val chave: String? = null,
    val ag: String? = null,
    val conta: String? = null,
    val dac: String? = null,
    val tipoConta: String? = null
)
```

### 5. `contatos/integration/ContatosCoreClient.kt`

```kotlin
@FeignClient(name = "contatos-core")
interface ContatosCoreClient {
    @GetMapping("/contatos-core")
    fun buscar(
        @RequestParam("idPessoa") idPessoa: String,
        @RequestParam("idConta") idConta: String,
        @RequestParam("cursor") cursor: String? = null
    ): ContatosCoreResponse
}
```

### 6. `contatos/integration/ContatosMapper.kt`

```kotlin
@Component
class ContatosMapper {

    fun toContato(dto: ContatoCoreDto): Contato? {
        val idDestino = dto.idDestino?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: return null.also { log.warn("contato descartado: idDestino nulo ou inválido") }
        val idContato = dto.idContato?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: return null.also { log.warn("contato descartado: idContato nulo ou inválido") }
        val nome = dto.nome
            ?: return null.also { log.warn("contato descartado: nome nulo") }

        val dd = dto.dadosDestino
        val dadosDestino = when {
            dd?.chave != null -> DadosDestino.ChavePix(dd.chave)
            dd?.ag != null && dd.conta != null && dd.dac != null && dd.tipoConta != null ->
                DadosDestino.AgenciaConta(
                    ag = dd.ag,
                    conta = dd.conta,
                    dac = dd.dac,
                    tipoConta = runCatching { TipoConta.valueOf(dd.tipoConta) }.getOrElse {
                        return null.also { log.warn("contato descartado: tipoConta inválido={}", dd.tipoConta) }
                    }
                )
            else -> return null.also { log.warn("contato descartado: dadosDestino insuficientes") }
        }

        return Contato(
            idDestino = idDestino,
            idContato = idContato,
            nome = nome,
            apelido = dto.apelido,
            dadosDestino = dadosDestino
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(ContatosMapper::class.java)
    }
}
```

### 7. `contatos/service/ContatosService.kt`

```kotlin
@Service
class ContatosService(
    private val client: ContatosCoreClient,
    private val mapper: ContatosMapper,
    private val usuarioContext: UsuarioContext
) {
    fun buscar(): List<Contato> {
        val acumulados = mutableListOf<ContatoCoreDto>()
        var cursor: String? = null
        do {
            val response = client.buscar(
                idPessoa = usuarioContext.idPessoa.toString(),
                idConta = usuarioContext.idConta.toString(),
                cursor = cursor
            )
            acumulados += response.data.orEmpty()
            cursor = response.xNextCursor?.takeIf { it.isNotEmpty() }
        } while (cursor != null)
        return acumulados.mapNotNull(mapper::toContato)
    }
}
```

### 8. `contatos/ContatosController.kt`

```kotlin
@RestController
@RequestMapping("/v1/contatos")
@CaronteMapping(rel = "contatos")
class ContatosController(private val service: ContatosService) {

    @GetMapping
    fun listar(): Map<String, List<Contato>> =
        mapOf("data" to service.buscar())
}
```

## Notas de implementação

- `xNextCursor?.takeIf { it.isNotEmpty() }` trata tanto `null` quanto string vazia como "sem próxima página"
- O `cursor` no `@RequestParam` tem `= null`; Feign não envia o parâmetro quando o valor é nulo — confirmar que a integração aceita ausência do parâmetro (em vez de `cursor=null` como string)
- `DadosDestino` desta slice usa `conta` (não `cc`) — diferente da slice de sugestões; tipos são independentes por design VSA
- `UsuarioContext` é `@RequestScope` — injeção direta em `ContatosService` (singleton) funciona via proxy Spring

## Critério de aceite

- [ ] GET /v1/contatos retorna `{"data": [...]}` com todos os contatos de todas as páginas
- [ ] O loop para quando `x-next-cursor` é nulo ou string vazia
- [ ] `DadosDestino` é `ChavePix` quando `chave` presente; `AgenciaConta` quando `ag`/`conta`/`dac`/`tipoConta` presentes
- [ ] Item com dados obrigatórios nulos é descartado com log warning
- [ ] Chamadas consecutivas sempre acionam a integração (sem cache)
- [ ] Teste: `deve percorrer todas as paginas ate cursor nulo`
- [ ] Teste: `deve percorrer todas as paginas ate cursor vazio`
- [ ] Teste: `deve retornar lista agregada de todas as paginas`
- [ ] Teste: `deve descartar contato com idDestino nulo`
- [ ] Teste: `deve mapear DadosDestino como ChavePix quando chave presente`
- [ ] Teste: `deve mapear DadosDestino como AgenciaConta quando ag e conta presentes`
- [ ] Build e testes passam sem erros
