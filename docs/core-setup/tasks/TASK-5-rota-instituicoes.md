# TASK-5 — Rota GET /v1/instituicoes

**Arquivos alvo:** pacote `com.staroscky.performance.instituicoes` (novo)
**Referência SPEC:** Seções 5 (RF-07 a RF-12), 8.4 (Domínio de Instituições), 10
**Depende de:** TASK-1, TASK-3 (AuthRequestInterceptor deve estar no contexto)
**Bloqueada por:** nenhuma

---

## Contexto

GET /v1/instituicoes retorna a lista de instituições financeiras enriquecida com nome e ícone configuráveis por ISPB. Os dados vêm de uma integração Feign e ficam cacheados no cache2k por 24h com chave global (todos os usuários compartilham o mesmo cache).

## O que fazer

### 1. `instituicoes/domain/Instituicao.kt`

```kotlin
data class Instituicao(
    val iconeUrl: String?,
    val ispb: String,
    val nome: String,
    val searchable: String
)
```

### 2. `instituicoes/config/InstituicoesConfigProperties.kt`

```kotlin
@ConfigurationProperties("instituicoes-config")
data class InstituicoesConfigProperties(
    val items: List<InstituicaoConfig> = emptyList()
) {
    data class InstituicaoConfig(
        val ispb: String,
        val nome: String?,
        val icone: String?
    )
}
```

### 3. `src/main/resources/config/instituicoes.yml` (atualizar com exemplos reais)

Já criado na TASK-1 com estrutura mínima. Confirmar que `spring.config.import` em `application.yml` inclui este arquivo ou que ele é carregado via `@ConfigurationProperties` com path correto.

> **Atenção**: Spring Boot 3.x carrega `classpath:config/instituicoes.yml` automaticamente se adicionado em `spring.config.import` no `application.yml`:
> ```yaml
> spring:
>   config:
>     import: classpath:config/instituicoes.yml
> ```

### 4. `instituicoes/integration/InstituicoesFinanceirasDto.kt`

```kotlin
data class InstituicoesFinanceirasResponse(
    val data: List<InstituicaoFinanceiraDto>? = null
)

data class InstituicaoFinanceiraDto(
    val ispb: String? = null,
    @JsonProperty("nome_fantasia") val nomeFantasia: String? = null,
    @JsonProperty("nome_reduzido") val nomeReduzido: String? = null
)
```

### 5. `instituicoes/integration/InstituicoesFinanceirasClient.kt`

```kotlin
@FeignClient(name = "instituicoes-financeiras")
interface InstituicoesFinanceirasClient {
    @GetMapping("/instituicoes-financeiras")
    fun buscar(): InstituicoesFinanceirasResponse
}
```

### 6. `instituicoes/integration/InstituicoesMapper.kt`

```kotlin
@Component
class InstituicoesMapper(private val config: InstituicoesConfigProperties) {

    fun toInstituicao(dto: InstituicaoFinanceiraDto): Instituicao? {
        val ispb = dto.ispb ?: return null
        val nomeReduzido = dto.nomeReduzido ?: return null
        val override = config.items.find { it.ispb == ispb }
        val nome = override?.nome ?: nomeReduzido
        val searchable = "${dto.nomeFantasia}#${nome}#${ispb}"
        return Instituicao(
            iconeUrl = override?.icone,
            ispb = ispb,
            nome = nome,
            searchable = searchable
        )
    }
}
```

### 7. `instituicoes/service/InstituicoesService.kt`

```kotlin
@Service
class InstituicoesService(
    private val client: InstituicoesFinanceirasClient,
    private val mapper: InstituicoesMapper
) {
    @Cacheable("instituicoes")
    fun buscar(): List<Instituicao> =
        client.buscar().data.orEmpty().mapNotNull(mapper::toInstituicao)
}
```

> Cache `"instituicoes"` deve estar declarado no `Cache2kConfig` (criado na TASK-1 ou em `core/cache/Cache2kConfig.kt`) com TTL de 24h.

### 8. `instituicoes/InstituicoesController.kt`

```kotlin
@RestController
@RequestMapping("/v1/instituicoes")
@CaronteMapping(rel = "instituicoes")
class InstituicoesController(private val service: InstituicoesService) {

    @GetMapping
    fun listar(): Map<String, List<Instituicao>> =
        mapOf("data" to service.buscar())
}
```

## Notas de implementação

- O cache `"instituicoes"` precisa ser registrado no `CacheManager` do cache2k — verifique se `Cache2kConfig` (de TASK-1 ou separado) já registra esse nome
- `@Cacheable` não funciona em chamadas internas (auto-invocação) — use sempre via injeção
- `searchable` usa `nomeFantasia` da integração (pode ser null) e `nome` do domínio — ajustar se `nomeFantasia` puder ser null: `"${dto.nomeFantasia ?: nome}#${nome}#${ispb}"`

## Critério de aceite

- [ ] GET /v1/instituicoes retorna `{"data": [{iconeUrl, ispb, nome, searchable}, ...]}`
- [ ] Segunda chamada não aciona `InstituicoesFinanceirasClient` (cache hit)
- [ ] Instituição com ISPB configurado no `instituicoes.yml` usa nome e ícone do config
- [ ] Instituição sem configuração usa `nome_reduzido` e `iconeUrl: null`
- [ ] Item sem `ispb` ou sem `nome_reduzido` é descartado (mapNotNull)
- [ ] Teste: `deve retornar lista com nome e icone do config quando ISPB configurado`
- [ ] Teste: `deve usar nome_reduzido quando ISPB sem override`
- [ ] Teste: `deve retornar iconeUrl null quando sem icone configurado`
- [ ] Teste: `deve descartar item sem ispb`
- [ ] Build e testes passam sem erros
