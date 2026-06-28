# TASK-6 — Rota GET /v1/sugestoes

**Arquivos alvo:** pacote `com.staroscky.performance.sugestoes` (novo)
**Referência SPEC:** Seções 5 (RF-13 a RF-17), 8.4 (Domínio de Sugestões), 8.1 (ValKeyConfig), 10
**Depende de:** TASK-1, TASK-2 (UsuarioContext), TASK-3 (AuthRequestInterceptor)
**Bloqueada por:** nenhuma

---

## Contexto

GET /v1/sugestoes retorna sugestões de transferência do usuário obtidas da integração `/v1/inteligencia`. O resultado é cacheado no Valkey (Redis) com TTL de 15 minutos, usando `idPessoa` como chave (isolamento por usuário). O domínio usa `sealed interface` para diferenciar ChavePix de AgenciaConta.

## O que fazer

### 1. `core/cache/ValKeyConfig.kt` (ou `sugestoes/config/ValKeyConfig.kt`)

Criar configuração do RedisTemplate. Coloque em `core/cache/` pois será reutilizado pela slice:

```kotlin
@Configuration
class ValKeyConfig {
    @Bean
    fun redisTemplate(factory: RedisConnectionFactory): RedisTemplate<String, Any> =
        RedisTemplate<String, Any>().apply {
            setConnectionFactory(factory)
            keySerializer = StringRedisSerializer()
            valueSerializer = GenericJackson2JsonRedisSerializer()
        }
}
```

### 2. `sugestoes/domain/TipoConta.kt`

```kotlin
enum class TipoConta { C, P, G }
```

### 3. `sugestoes/domain/DadosDestino.kt`

```kotlin
sealed interface DadosDestino {
    data class ChavePix(val chave: String) : DadosDestino
    data class AgenciaConta(
        val ag: String,
        val cc: String,
        val dac: String,
        val tipoConta: TipoConta
    ) : DadosDestino
}
```

### 4. `sugestoes/domain/Sugestao.kt`

```kotlin
data class Sugestao(
    val idSugestao: UUID,
    val apelido: String?,
    val dadosDestino: DadosDestino
)
```

### 5. `sugestoes/integration/InteligenciaDto.kt`

```kotlin
data class InteligenciaResponse(
    val data: List<InteligenciaItemDto>? = null
)

data class InteligenciaItemDto(
    val idSugestao: String? = null,
    val chave: String? = null,
    val apelido: String? = null,
    val ag: String? = null,
    val cc: String? = null,
    val dac: String? = null,
    val tipoConta: String? = null
)
```

### 6. `sugestoes/integration/InteligenciaClient.kt`

```kotlin
@FeignClient(name = "inteligencia")
interface InteligenciaClient {
    @GetMapping("/inteligencia")
    fun buscar(
        @RequestParam("tela") tela: String,
        @RequestParam("cliente") cliente: String
    ): InteligenciaResponse
}
```

### 7. `sugestoes/integration/InteligenciaMapper.kt`

```kotlin
@Component
class InteligenciaMapper {

    fun toSugestao(dto: InteligenciaItemDto): Sugestao? {
        val idSugestao = dto.idSugestao?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: return null.also { log.warn("sugestao descartada: idSugestao nulo ou inválido") }

        val dadosDestino = when {
            dto.chave != null -> DadosDestino.ChavePix(dto.chave)
            dto.ag != null && dto.cc != null && dto.dac != null && dto.tipoConta != null ->
                DadosDestino.AgenciaConta(
                    ag = dto.ag,
                    cc = dto.cc,
                    dac = dto.dac,
                    tipoConta = runCatching { TipoConta.valueOf(dto.tipoConta) }.getOrElse {
                        return null.also { log.warn("sugestao descartada: tipoConta inválido={}", dto.tipoConta) }
                    }
                )
            else -> return null.also { log.warn("sugestao descartada: dados de destino insuficientes") }
        }

        return Sugestao(idSugestao = idSugestao, apelido = dto.apelido, dadosDestino = dadosDestino)
    }

    companion object {
        private val log = LoggerFactory.getLogger(InteligenciaMapper::class.java)
    }
}
```

### 8. `sugestoes/service/SugestoesService.kt`

```kotlin
@Service
class SugestoesService(
    private val client: InteligenciaClient,
    private val mapper: InteligenciaMapper,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val usuarioContext: UsuarioContext
) {
    fun buscar(): List<Sugestao> {
        val key = "sugestoes:${usuarioContext.idPessoa}"
        @Suppress("UNCHECKED_CAST")
        val cached = redisTemplate.opsForValue().get(key) as? List<Sugestao>
        if (cached != null) return cached

        val result = client.buscar(
            tela = "sugestao-transferencia",
            cliente = "${usuarioContext.idPessoa}-${usuarioContext.idConta}"
        ).data.orEmpty().mapNotNull(mapper::toSugestao)

        redisTemplate.opsForValue().set(key, result, Duration.ofMinutes(15))
        return result
    }
}
```

### 9. `sugestoes/SugestoesController.kt`

```kotlin
@RestController
@RequestMapping("/v1/sugestoes")
@CaronteMapping(rel = "sugestoes")
class SugestoesController(private val service: SugestoesService) {

    @GetMapping
    fun listar(): Map<String, List<Sugestao>> =
        mapOf("data" to service.buscar())
}
```

## Notas de implementação

- `GenericJackson2JsonRedisSerializer` inclui `@class` no JSON para desserialização polimórfica do `sealed interface`. Se causar problemas no Redis (ex.: desserialização após reinicialização), usar um DTO de resposta simples (`SugestaoDto`) no cache e mapear para domínio ao ler
- `UsuarioContext` é `@RequestScope` — injete-o diretamente no `SugestoesService` (que é singleton); Spring resolve via proxy automático
- O cast `as? List<Sugestao>` com `@Suppress` é necessário por type erasure do generics em runtime
- `runCatching { TipoConta.valueOf(...) }` evita exceção para `tipoConta` inválido

## Critério de aceite

- [ ] GET /v1/sugestoes retorna `{"data": [...]}` com domínio sealed correto
- [ ] Segunda chamada com mesmo `idPessoa` retorna cache do Valkey (sem acionar integração)
- [ ] `DadosDestino` é `ChavePix` quando `chave` presente; `AgenciaConta` quando `ag`/`cc`/`dac`/`tipoConta` presentes
- [ ] Item com dados insuficientes é descartado com log warning
- [ ] Teste: `deve retornar sugestoes com ChavePix quando chave presente`
- [ ] Teste: `deve retornar sugestoes com AgenciaConta quando ag e cc presentes`
- [ ] Teste: `deve descartar item quando idSugestao nulo`
- [ ] Teste: `deve retornar cache quando ja existir no Valkey`
- [ ] Teste: `deve salvar resultado no Valkey com TTL 15 minutos`
- [ ] Build e testes passam sem erros
