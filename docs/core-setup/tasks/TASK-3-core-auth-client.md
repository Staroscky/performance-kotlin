# TASK-3 — Core: Auth Client (STS + cache2k)

**Arquivos alvo:** pacote `com.staroscky.performance.core.auth` e `core.feign` (novos)
**Referência SPEC:** Seções 5 (RF-04, RF-05), 8.4 (StsClient, AuthTokenService, AuthRequestInterceptor)
**Depende de:** TASK-1
**Bloqueada por:** nenhuma

---

## Contexto

Todas as chamadas Feign para integrações externas precisam de um Bearer token obtido do STS. O token deve ser cacheado no cache2k com TTL dinâmico (baseado no `expires_in` retornado pelo STS, renovando 30s antes do vencimento).

## O que fazer

### 1. `core/auth/StsProperties.kt`

```kotlin
@ConfigurationProperties("sts")
data class StsProperties(
    val url: String,
    val client: Client,
    val appId: String
) {
    data class Client(val id: String, val secret: String)
}
```

### 2. `core/auth/StsTokenResponse.kt`

```kotlin
data class StsTokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("expires_in") val expiresIn: Long
)
```

### 3. `core/auth/StsClient.kt`

```kotlin
@FeignClient(name = "sts", url = "\${sts.url}")
interface StsClient {
    @PostMapping(consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun authenticate(
        @RequestParam("client_id") clientId: String,
        @RequestParam("client_secret") clientSecret: String,
        @RequestParam("grant_type") grantType: String = "client_credentials"
    ): StsTokenResponse
}
```

### 4. `core/auth/AuthTokenService.kt`

```kotlin
@Service
class AuthTokenService(
    private val stsProperties: StsProperties,
    private val stsClient: StsClient
) {
    private val cache: Cache<String, String> = Cache2kBuilder
        .of(String::class.java, String::class.java)
        .name("auth-token")
        .eternal(false)
        .build()

    fun getToken(): String = cache.get("token") ?: fetchAndCache()

    private fun fetchAndCache(): String {
        val response = stsClient.authenticate(
            clientId = stsProperties.client.id,
            clientSecret = stsProperties.client.secret
        )
        val expiryMs = System.currentTimeMillis() + (response.expiresIn - 30) * 1000L
        cache.invoke("token") {
            it.value = response.accessToken
            it.setExpiryTime(expiryMs)
        }
        return response.accessToken
    }
}
```

### 5. `core/feign/AuthRequestInterceptor.kt`

```kotlin
@Component
class AuthRequestInterceptor(
    private val authTokenService: AuthTokenService
) : RequestInterceptor {
    override fun apply(template: RequestTemplate) {
        template.header("Authorization", "Bearer ${authTokenService.getToken()}")
    }
}
```

## Notas de implementação

- `cache.invoke` é a API de mutação do cache2k para definir TTL por entrada via `setExpiryTime` — não usar `synchronized` em código próprio
- O `StsClient` não deve passar pelo `AuthRequestInterceptor` (evitar loop). O interceptor é registrado como bean global, mas o Feign do STS não precisa de Bearer — verificar se é necessário excluir o client STS via configuração (`@FeignClient(configuration = [StsClientConfig::class])` com interceptor excluído) ou se o STS ignora o header Authorization extra
- Se o STS rejeitar a chamada com Bearer header, criar `StsClientConfig` com `RequestInterceptor` vazio explícito para sobrescrever o global
- Não use `synchronized` — use `cache2k` para gerenciar concorrência de cache miss

## Critério de aceite

- [ ] `AuthTokenService` busca token do STS e cacheia com TTL = `expires_in - 30` segundos
- [ ] Teste: `deve retornar token cacheado na segunda chamada sem acionar StsClient`
- [ ] Teste: `deve chamar StsClient quando cache estiver vazio`
- [ ] Teste: `deve renovar token quando cache expirar`
- [ ] `AuthRequestInterceptor` injeta `Authorization: Bearer <token>` em chamadas Feign
- [ ] Build e testes passam sem erros
