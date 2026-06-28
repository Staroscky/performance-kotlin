# SPEC — Core Setup: Configs Core + 4 Rotas de Performance

## 1. Contexto da solicitação

### 1.1 História ou tarefa do usuário

- Solicitante: time de desenvolvimento
- Tipo: feature (implementação inicial)
- História/tarefa: Implementar a estrutura completa da API de performance — configs core reutilizáveis (UsuarioContext, CaronteMapping, AuthClient) e quatro rotas REST com padrões de integração Feign, cache em camadas e domínio sealed em Kotlin idiomático.
- Valor esperado: ter uma API funcional que serve como referência e base de testes de performance para os padrões de integração da plataforma.

### 1.2 Problema observado

O projeto está no estado de bootstrap: apenas `PerformanceApplication.kt` e `application.yaml` com o nome da app. Nenhuma rota, configuração ou integração existe. O objetivo é implementar tudo do zero seguindo os padrões VSA, kotlin-boas-praticas e virtual-threads-pinning definidos nas skills do projeto.

### 1.3 Objetivo da entrega

- GET /entrypoint listando as rotas registradas via anotação
- GET /v1/instituicoes com cache2k 24h e personalização por ISPB
- GET /v1/sugestoes com cache Valkey 15m por idPessoa
- GET /v1/contatos com paginação cursor e sem cache
- Beans core: UsuarioContext, AuthTokenService e CaronteRegistry

---

## 2. Objetivo técnico

Construir sobre o projeto Spring Boot 3.5 (Java 21 + virtual threads) a infraestrutura de integração e as quatro slices VSA, adicionando as dependências necessárias ao `pom.xml` e todos os arquivos de configuração por ambiente.

---

## 3. Estado atual

- `pom.xml`: Spring Boot 3.5.16, Kotlin 1.9.25, Java 21, apenas `spring-boot-starter-web` + test
- `src/main/resources/application.yaml` (será renomeado para `application.yml`): apenas `spring.application.name: performance`
- `src/main/kotlin/com/staroscky/performance/PerformanceApplication.kt`: bootstrap
- Sem controllers, services, integrações, configs ou modelos de domínio

---

## 4. Escopo da solução

### 4.1 O que muda

| Área | Estado atual | Estado esperado | Impacto |
|---|---|---|---|
| `pom.xml` | Só web + test | + Feign, cache2k, Redis/Valkey, JWT, config-processor, mockito-kotlin | Alto |
| `application.yml` | `application.yaml` com apenas nome da app | Renomeado para `.yml` + todas as configs | Alto |
| `application-{local,dev,hom,prod}.yml` | Não existem | URLs e chaves por ambiente | Alto |
| `core` (pacote novo) | Não existe | UsuarioContext, AuthTokenService, CaronteRegistry, AuthInterceptor | Alto |
| `entrypoint` (pacote novo) | Não existe | EntrypointController servindo GET /entrypoint | Médio |
| `instituicoes` (pacote novo) | Não existe | Controller, domain, service, integration + cache2k | Alto |
| `sugestoes` (pacote novo) | Não existe | Controller, domain sealed, service, integration + Valkey | Alto |
| `contatos` (pacote novo) | Não existe | Controller, domain sealed, service, integration + paginação cursor | Alto |
| `resources/config/instituicoes.yml` | Não existe | Config de ISPB → nome personalizado + ícone | Médio |

### 4.2 O que não muda

- `PerformanceApplication.kt` (apenas adicionados `@EnableFeignClients` e `@EnableCaching`)
- Estrutura de módulos Maven (continua single-module)
- Não há banco de dados relacional; sem Flyway ou JPA
- Sem Spring Security — a validação do JWT é responsabilidade do gateway

### 4.3 Restrições e pressupostos

- Java 21 com virtual threads habilitadas via `spring.threads.virtual.enabled=true`
- Valkey é compatível com Redis protocol; `spring-boot-starter-data-redis` com Lettuce atende
- JWT não é re-validado — apenas decodificado (base64 do payload) para extrair claims
- Spring Cloud versão compatível com Spring Boot 3.5.x deve ser verificada em spring.io antes da implementação
- cache2k deve ser configurado sem blocos `synchronized` em código próprio para evitar pinning

---

## 5. Requisitos funcionais

| ID | Requisito funcional | Prioridade | Origem |
|---|---|---|---|
| RF-01 | Bean `UsuarioContext` (`@RequestScope`) extrai `idPessoa` e `idConta` do JWT no header `Authorization: Bearer <token>` | must | PRD §Config Core/Usuario |
| RF-02 | Anotação `@CaronteMapping(rel)` aplicável a controllers registra a rota no entrypoint | must | PRD §Config Core/Caronte |
| RF-03 | `CaronteRegistry` coleta todos os beans anotados com `@CaronteMapping` no startup, lendo `rel` da anotação e `href` do `@RequestMapping` da classe | must | PRD §Config Core/Caronte |
| RF-04 | `AuthTokenService` autentica no STS com client_id/secret, cacheia o token no cache2k e o renova quando faltarem 30s para expirar | must | PRD §Config Core/Auth |
| RF-05 | `AuthRequestInterceptor` injeta o token Bearer (via `AuthTokenService`) no header `Authorization` de todas as chamadas Feign | must | PRD §Config Core/Auth |
| RF-06 | GET /entrypoint retorna `{"data": [...{rel, href}]}` com os registros do `CaronteRegistry` | must | PRD §Rota 1 |
| RF-07 | GET /v1/instituicoes retorna lista de instituições mapeadas com `iconeUrl`, `ispb`, `nome` e `searchable` | must | PRD §Rota 2 |
| RF-08 | A lista de instituições é obtida via Feign de `GET /v1/instituicoes-financeiras` | must | PRD §Rota 2/Integração |
| RF-09 | A lista é cacheada no cache2k com TTL de 24h usando chave global (não por usuário) | must | PRD §Rota 2 |
| RF-10 | `resources/config/instituicoes.yml` configura por ISPB: nome personalizado e ícone | must | PRD §Rota 2 |
| RF-11 | Se não houver ícone configurado → `iconeUrl: null`; se não houver nome personalizado → usar `nome_reduzido` | must | PRD §Rota 2 |
| RF-12 | `searchable` é construído como `"{nome_fantasia}#{nome}#{ispb}"` | must | PRD §Rota 2 (inferido do exemplo) |
| RF-13 | GET /v1/sugestoes retorna lista de sugestões de transferência com domínio sealed `DadosDestino` | must | PRD §Rota 3 |
| RF-14 | A integração é feita via Feign em `GET /v1/inteligencia?tela=sugestao-transferencia&cliente={idPessoa}-{idConta}` | must | PRD §Rota 3/Integração |
| RF-15 | O resultado é cacheado no Valkey com TTL de 15 minutos, chave = `sugestoes:{idPessoa}` | must | PRD §Rota 3 |
| RF-16 | `DadosDestino` na slice sugestoes é `sealed interface`: `ChavePix(chave: String)` ou `AgenciaConta(ag, cc, dac, tipoConta)` | must | PRD §Rota 3 |
| RF-17 | O DTO da integração de sugestões tem todos os campos opcionais; obrigatoriedade validada no mapper | must | PRD §Rota 3 |
| RF-18 | GET /v1/contatos retorna todos os contatos do usuário (idPessoa e idConta extraídos do JWT) | must | PRD §Rota 4 |
| RF-19 | A integração usa Feign em `GET /v1/contatos-core` com paginação cursor; o service percorre todas as páginas e agrega o resultado | must | PRD §Rota 4 |
| RF-20 | Não há cache em /v1/contatos — dados sempre frescos | must | PRD §Rota 4 |
| RF-21 | `DadosDestino` na slice contatos é `sealed interface`: `ChavePix(chave: String)` ou `AgenciaConta(ag, conta, dac, tipoConta)` | must | PRD §Rota 4 |
| RF-22 | O DTO da integração de contatos tem todos os campos opcionais; obrigatoriedade validada no mapper | must | PRD §Rota 4 |

---

## 6. Cenários e fluxos esperados

### 6.1 Cenários principais

- **GET /entrypoint**: retorna exatamente os `{rel, href}` das controllers anotadas com `@CaronteMapping`, sem item hard-coded
- **GET /v1/instituicoes (cache miss)**: chama integração, aplica overrides do YAML, armazena no cache2k por 24h, retorna lista
- **GET /v1/instituicoes (cache hit)**: retorna lista do cache2k sem chamar integração
- **GET /v1/sugestoes (cache miss)**: lê idPessoa/idConta do JWT, chama integração, mapeia domínio sealed, armazena no Valkey por 15m, retorna lista
- **GET /v1/sugestoes (cache hit)**: retorna lista do Valkey sem chamar integração
- **GET /v1/contatos**: lê idPessoa/idConta do JWT, chama integração repetidamente percorrendo cursor, agrega todos os itens, retorna lista completa

### 6.2 Edge cases e falhas esperadas

- **JWT malformado ou sem claim**: `UsuarioContext` lança exceção → resposta 4xx (sem tratar no domínio, pois é erro de borda HTTP)
- **Campo obrigatório nulo na integração de sugestões/contatos**: o mapper descarta o item e loga warning; não quebra a lista
- **Instituição sem nome personalizado**: usar `nome_reduzido`; se também nulo → descartar item (integridade do ISPB)
- **Instituição sem ícone**: retornar `iconeUrl: null`
- **STS retornando erro**: `AuthTokenService` propaga exceção → Feign falha com 5xx
- **Última página do cursor (contatos)**: quando o cursor retornar `null` ou ausente na resposta, o loop para
- **Cache Valkey indisponível**: sem fallback definido no PRD → propagar erro 5xx (não silenciar)

---

## 7. Alternativas consideradas

### 7.1 Alternativa escolhida

**Decodificação manual do JWT** (Base64 do payload) para `UsuarioContext`, sem biblioteca adicional. Simples, zero dependência extra, sem overhead de verificação de assinatura (responsabilidade do gateway).

**cache2k via acesso direto** para `AuthTokenService` (não via Spring Cache `@Cacheable`), pois o TTL é dinâmico por entrada (baseado no `expires_in` do STS). O Spring Cache abstraction não suporta TTL dinâmico facilmente.

**Spring Cache (`@Cacheable`)** para a lista de instituições (TTL fixo de 24h via configuração do bean de cache2k). Mais simples do que acesso direto.

**Spring Data Redis** para Valkey com Lettuce — Lettuce é não-bloqueante, o que garante ausência de pinning de virtual threads.

### 7.2 Alternativas descartadas

| Alternativa | Vantagens | Desvantagens | Motivo da não escolha |
|---|---|---|---|
| Spring Security + JWT Resource Server | Valida assinatura, integra com `SecurityContext` | Adiciona dependência pesada, config complexa | Gateway já valida; aqui só precisamos dos claims |
| `java-jwt` (Auth0) para UsuarioContext | API fluente para claims | Dependência extra para uso mínimo | Base64 + Jackson é suficiente |
| `@Cacheable` com TTL fixo para AuthToken | Simples | TTL não reflete `expires_in` do STS | PRD exige renovar 30s antes do vencimento real |
| Spring Session + Redis para Valkey | Integração pronta | Overhead de sessão, não é cache de dados | Valkey aqui é KV de dados, não sessão |
| WebFlux / reactive | Melhor throughput teórico | Complexidade, incompatível com virtual threads direto | Java 21 + virtual threads resolve em MVC |

---

## 8. Design da solução

### 8.1 Visão geral da abordagem

```
pom.xml
  → adicionar: Spring Cloud BOM + openfeign, cache2k, spring-data-redis,
               spring-boot-starter-cache, mockito-kotlin, config-processor

application.yml
  → virtual threads, STS config, Feign config, Redis/Valkey, cache2k

PerformanceApplication.kt
  → adicionar @EnableFeignClients, @EnableCaching, @ConfigurationPropertiesScan

core/
  ├── UsuarioContext           (@RequestScope, decodifica JWT)
  ├── auth/
  │   ├── StsProperties        (@ConfigurationProperties)
  │   ├── StsClient            (@FeignClient para STS)
  │   ├── StsTokenResponse     (DTO)
  │   └── AuthTokenService     (cache2k direto, TTL dinâmico)
  ├── feign/
  │   └── AuthRequestInterceptor (RequestInterceptor)
  └── caronte/
      ├── CaronteMapping       (annotation)
      ├── CaronteItem          (data class: rel, href)
      └── CaronteRegistry      (coleta no startup via ApplicationContext)

entrypoint/
  └── EntrypointController    (GET /entrypoint, usa CaronteRegistry)

instituicoes/
  ├── InstituicoesController  (@CaronteMapping, GET /v1/instituicoes)
  ├── domain/Instituicao      (data class)
  ├── service/InstituicoesService  (@Cacheable cache2k 24h)
  ├── integration/
  │   ├── InstituicoesFinanceirasClient  (@FeignClient)
  │   ├── InstituicoesFinanceirasDto
  │   └── InstituicoesMapper
  └── config/
      ├── InstituicoesConfigProperties  (@ConfigurationProperties)
      └── resources/config/instituicoes.yml

sugestoes/
  ├── SugestoesController     (@CaronteMapping, GET /v1/sugestoes)
  ├── domain/
  │   ├── Sugestao            (data class)
  │   ├── DadosDestino        (sealed: ChavePix | AgenciaConta)
  │   └── TipoConta           (enum: C, P, G)
  ├── service/SugestoesService  (Spring Cache → Valkey, TTL 15m)
  └── integration/
      ├── InteligenciaClient  (@FeignClient)
      ├── InteligenciaDto     (todos os campos String?/UUID?)
      └── InteligenciaMapper

contatos/
  ├── ContatosController      (@CaronteMapping, GET /v1/contatos)
  ├── domain/
  │   ├── Contato             (data class)
  │   ├── DadosDestino        (sealed: ChavePix | AgenciaConta)
  │   └── TipoConta           (enum: C, P, G)
  ├── service/ContatosService  (loop cursor, sem cache)
  └── integration/
      ├── ContatosCoreClient  (@FeignClient)
      ├── ContatosCoreDto     (todos os campos String?/UUID?)
      └── ContatosMapper
```

### 8.2 Dependências a adicionar no `pom.xml`

```xml
<!-- Spring Cloud BOM (verificar versão compatível com Boot 3.5.x em spring.io) -->
<dependencyManagement>
  <dependency groupId="org.springframework.cloud"
              artifactId="spring-cloud-dependencies"
              version="${spring-cloud.version}" type="pom" scope="import"/>
</dependencyManagement>

<!-- Feign -->
<dependency>spring-cloud-starter-openfeign</dependency>

<!-- Cache2k -->
<dependency>org.cache2k:cache2k-spring:2.6.1.Final</dependency>
<dependency>spring-boot-starter-cache</dependency>

<!-- Valkey / Redis -->
<dependency>spring-boot-starter-data-redis</dependency>

<!-- Config Processor -->
<dependency>spring-boot-configuration-processor (optional=true)</dependency>

<!-- Testes -->
<dependency>org.mockito.kotlin:mockito-kotlin:5.4.0 (scope=test)</dependency>
```

### 8.3 Configuração (`application.yml`)

```yaml
spring:
  application:
    name: performance
  threads:
    virtual:
      enabled: true
  cache:
    type: cache2k
  data:
    redis:
      host: ${integration.valkey.host:localhost}
      port: ${integration.valkey.port:6379}

sts:
  url: ${integration.sts.url:http://localhost:9000/v1/oauth}
  client:
    id: ${integration.sts.client-id}
    secret: ${integration.sts.client-secret}
  app-id: d230d5c5-a270-4a9c-a93e-ac2da7ff176f

spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connectTimeout: 1500
            readTimeout: 1500
          instituicoes-financeiras:
            url: ${integration.instituicoes-financeiras.url}
            defaultRequestHeaders:
              x-api-key: ${integration.instituicoes-financeiras.api-key}
              x-app-id: ${sts.app-id}
          inteligencia:
            url: ${integration.inteligencia.url}
          contatos-core:
            url: ${integration.contatos-core.url}
```

### 8.4 Contratos, dados e interfaces

#### UsuarioContext

```kotlin
@Component
@RequestScope
class UsuarioContext(request: HttpServletRequest) {
    val idPessoa: UUID
    val idConta: UUID

    init {
        val token = request.getHeader("Authorization")
            ?.removePrefix("Bearer ")
            ?: error("Authorization header ausente")
        val payload = String(Base64.getUrlDecoder().decode(token.split(".")[1]))
        val claims = ObjectMapper().readValue(payload, Map::class.java)
        idPessoa = UUID.fromString(claims["idPessoa"] as String)
        idConta = UUID.fromString(claims["idConta"] as String)
    }
}
```

#### CaronteMapping e CaronteRegistry

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CaronteMapping(val rel: String)

data class CaronteItem(val rel: String, val href: String)

@Component
class CaronteRegistry(ctx: ApplicationContext) {
    val items: List<CaronteItem> = ctx
        .getBeansWithAnnotation(CaronteMapping::class.java)
        .values
        .mapNotNull { bean ->
            val clazz = AopUtils.getTargetClass(bean)
            val rel = clazz.getAnnotation(CaronteMapping::class.java)?.rel ?: return@mapNotNull null
            val href = clazz.getAnnotation(RequestMapping::class.java)?.value?.firstOrNull()
                ?: return@mapNotNull null
            CaronteItem(rel, href)
        }
        .sortedBy { it.rel }
}
```

#### AuthTokenService (cache2k direto, TTL dinâmico)

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
        .expiryPolicy { _, _, _, _ -> 0L } // TTL definido na inserção
        .build()

    fun getToken(): String =
        cache.get("token") ?: fetchAndCache()

    private fun fetchAndCache(): String {
        val response = stsClient.authenticate(
            clientId = stsProperties.client.id,
            clientSecret = stsProperties.client.secret
        )
        val ttlMs = (response.expiresIn - 30) * 1000L
        cache.invoke("token") {
            it.value = response.accessToken
            it.setExpiryTime(System.currentTimeMillis() + ttlMs)
        }
        return response.accessToken
    }
}
```

> **Nota**: `cache.invoke` usa a API de mutação do cache2k para definir TTL por entrada. Evitar `synchronized` em código próprio — cache2k gerencia a sincronização internamente.

#### STS Feign Client

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

data class StsTokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("expires_in") val expiresIn: Long
)
```

#### AuthRequestInterceptor

```kotlin
@Component
class AuthRequestInterceptor(private val authTokenService: AuthTokenService) : RequestInterceptor {
    override fun apply(template: RequestTemplate) {
        template.header("Authorization", "Bearer ${authTokenService.getToken()}")
    }
}
```

#### Domínio de Sugestões

```kotlin
data class Sugestao(
    val idSugestao: UUID,
    val apelido: String?,
    val dadosDestino: DadosDestino
)

sealed interface DadosDestino {
    data class ChavePix(val chave: String) : DadosDestino
    data class AgenciaConta(
        val ag: String,
        val cc: String,
        val dac: String,
        val tipoConta: TipoConta
    ) : DadosDestino
}

enum class TipoConta { C, P, G }
```

#### DTO de integração de Sugestões (todos opcionais)

```kotlin
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

#### Domínio de Contatos

```kotlin
data class Contato(
    val idDestino: UUID,
    val idContato: UUID,
    val nome: String,
    val apelido: String?,
    val dadosDestino: DadosDestino
)

sealed interface DadosDestino {
    data class ChavePix(val chave: String) : DadosDestino
    data class AgenciaConta(
        val ag: String,
        val conta: String,
        val dac: String,
        val tipoConta: TipoConta
    ) : DadosDestino
}

enum class TipoConta { C, P, G }
```

#### DTO de integração de Contatos (todos opcionais)

```kotlin
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

#### Resposta paginada de Contatos (cursor)

O campo de cursor na resposta se chama `x-next-cursor`. Quando não há mais páginas, vem `null` ou string vazia.

```kotlin
data class ContatosCoreResponse(
    val data: List<ContatoCoreDto>? = null,
    @JsonProperty("x-next-cursor") val xNextCursor: String? = null
)
```

#### Domínio de Instituições

```kotlin
data class Instituicao(
    val iconeUrl: String?,
    val ispb: String,
    val nome: String,
    val searchable: String
)
```

#### Config de Instituições (`resources/config/instituicoes.yml`)

```yaml
instituicoes-config:
  items:
    - ispb: "12345678"
      nome: "XPTO"
      icone: "ids_xxx"
    - ispb: "87654321"
      nome: null    # usa nome_reduzido da integração
      icone: null
```

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

> `@ConfigurationPropertiesScan` na `PerformanceApplication` registra automaticamente todos os `@ConfigurationProperties` do pacote raiz, incluindo este.

#### Cache de Instituições (cache2k via Spring Cache)

```kotlin
@Configuration
class Cache2kConfig {
    @Bean
    fun cacheManager(): CacheManager = SpringCache2kCacheManager()
        .addCaches {
            it.name("instituicoes").eternal(false).expireAfterWrite(24, TimeUnit.HOURS)
        }
}
```

#### Valkey (Redis) para Sugestões

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

O `SugestoesService` usa `redisTemplate.opsForValue()` diretamente com TTL explícito (não `@Cacheable`, pois a chave é dinâmica por idPessoa e o TTL é fixo em 15m):

```kotlin
@Service
class SugestoesService(
    private val inteligenciaClient: InteligenciaClient,
    private val inteligenciaMapper: InteligenciaMapper,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val usuarioContext: UsuarioContext
) {
    fun buscar(): List<Sugestao> {
        val key = "sugestoes:${usuarioContext.idPessoa}"
        @Suppress("UNCHECKED_CAST")
        val cached = redisTemplate.opsForValue().get(key) as? List<Sugestao>
        if (cached != null) return cached
        val result = inteligenciaClient.buscar(
            tela = "sugestao-transferencia",
            cliente = "${usuarioContext.idPessoa}-${usuarioContext.idConta}"
        ).data.orEmpty().mapNotNull(inteligenciaMapper::toSugestao)
        redisTemplate.opsForValue().set(key, result, Duration.ofMinutes(15))
        return result
    }
}
```

> **Alternativa**: Se a serialização do `sealed interface` via `GenericJackson2JsonRedisSerializer` causar problemas de desserialização, usar um DTO simples de resposta serializado como `List<SugestaoResponseDto>` no cache e converter para domínio na leitura. Ver seção 15.

#### Paginação cursor em ContatosService

```kotlin
@Service
class ContatosService(
    private val contatosCoreClient: ContatosCoreClient,
    private val contatosMapper: ContatosMapper,
    private val usuarioContext: UsuarioContext
) {
    fun buscar(): List<Contato> {
        val result = mutableListOf<ContatoCoreDto>()
        var cursor: String? = null
        do {
            val response = contatosCoreClient.buscar(
                idPessoa = usuarioContext.idPessoa.toString(),
                idConta = usuarioContext.idConta.toString(),
                cursor = cursor
            )
            result += response.data.orEmpty()
            cursor = response.xNextCursor?.takeIf { it.isNotEmpty() }
        } while (cursor != null)
        return result.mapNotNull(contatosMapper::toContato)
    }
}
```

---

## 9. Fluxos técnicos

```text
GET /v1/sugestoes
  → UsuarioContext decodifica JWT → idPessoa, idConta
  → SugestoesService verifica chave "sugestoes:{idPessoa}" no Valkey
    ├── HIT  → retorna lista cached
    └── MISS → InteligenciaClient.buscar(tela, cliente)
               → InteligenciaMapper.toSugestao (filtra nulos)
               → redisTemplate.set(key, result, 15m)
               → retorna lista
```

```text
GET /v1/contatos
  → UsuarioContext decodifica JWT → idPessoa, idConta
  → ContatosService inicia loop cursor
    └── ContatosCoreClient.buscar(idPessoa, idConta, cursor)
        ├── xNextCursor não nulo e não vazio → acumula data, repete
        └── xNextCursor null ou vazio → para, ContatosMapper.toContato (filtra nulos), retorna
```

```text
AuthRequestInterceptor (toda chamada Feign exceto STS)
  → AuthTokenService.getToken()
    ├── cache2k HIT e não expirado → retorna token
    └── MISS ou expirado → StsClient.authenticate() → cacheia com TTL dinâmico
```

---

## 10. Arquivos afetados

| Arquivo | Tipo | Mudança |
|---|---|---|
| `pom.xml` | Modificar | + dependências: Feign, cache2k, Redis, config-processor, mockito-kotlin |
| `src/main/resources/application.yml` | Renomear + Modificar | Renomear de `.yaml` → `.yml` + adicionar todas as configs |
| `src/main/resources/application-local.yml` | Criar | Valores locais de desenvolvimento |
| `src/main/resources/application-dev.yml` | Criar | Valores de DEV (placeholders) |
| `src/main/resources/application-hom.yml` | Criar | Valores de HOM (placeholders) |
| `src/main/resources/application-prod.yml` | Criar | Valores de PROD (placeholders) |
| `src/main/resources/config/instituicoes.yml` | Criar | Config de ISPB → nome + ícone |
| `PerformanceApplication.kt` | Modificar | + `@EnableFeignClients`, `@EnableCaching`, `@ConfigurationPropertiesScan` |
| `core/UsuarioContext.kt` | Criar | Bean @RequestScope |
| `core/auth/StsProperties.kt` | Criar | @ConfigurationProperties("sts") |
| `core/auth/StsClient.kt` | Criar | @FeignClient STS |
| `core/auth/StsTokenResponse.kt` | Criar | DTO do token |
| `core/auth/AuthTokenService.kt` | Criar | cache2k direto com TTL dinâmico |
| `core/feign/AuthRequestInterceptor.kt` | Criar | RequestInterceptor |
| `core/caronte/CaronteMapping.kt` | Criar | Anotação |
| `core/caronte/CaronteItem.kt` | Criar | data class |
| `core/caronte/CaronteRegistry.kt` | Criar | Coleta no startup |
| `core/cache/Cache2kConfig.kt` | Criar | Configura CacheManager cache2k |
| `core/cache/ValKeyConfig.kt` | Criar | Configura RedisTemplate |
| `entrypoint/EntrypointController.kt` | Criar | GET /entrypoint |
| `instituicoes/InstituicoesController.kt` | Criar | GET /v1/instituicoes |
| `instituicoes/domain/Instituicao.kt` | Criar | data class |
| `instituicoes/service/InstituicoesService.kt` | Criar | @Cacheable cache2k 24h |
| `instituicoes/integration/InstituicoesFinanceirasClient.kt` | Criar | @FeignClient |
| `instituicoes/integration/InstituicoesFinanceirasDto.kt` | Criar | DTO raw |
| `instituicoes/integration/InstituicoesMapper.kt` | Criar | DTO → domínio |
| `instituicoes/config/InstituicoesConfigProperties.kt` | Criar | @ConfigurationProperties |
| `sugestoes/SugestoesController.kt` | Criar | GET /v1/sugestoes |
| `sugestoes/domain/Sugestao.kt` | Criar | data class |
| `sugestoes/domain/DadosDestino.kt` | Criar | sealed interface |
| `sugestoes/domain/TipoConta.kt` | Criar | enum |
| `sugestoes/service/SugestoesService.kt` | Criar | Valkey manual, TTL 15m |
| `sugestoes/integration/InteligenciaClient.kt` | Criar | @FeignClient |
| `sugestoes/integration/InteligenciaDto.kt` | Criar | DTO raw (tudo opcional) |
| `sugestoes/integration/InteligenciaMapper.kt` | Criar | DTO → domínio sealed |
| `contatos/ContatosController.kt` | Criar | GET /v1/contatos |
| `contatos/domain/Contato.kt` | Criar | data class |
| `contatos/domain/DadosDestino.kt` | Criar | sealed interface |
| `contatos/domain/TipoConta.kt` | Criar | enum |
| `contatos/service/ContatosService.kt` | Criar | loop cursor |
| `contatos/integration/ContatosCoreClient.kt` | Criar | @FeignClient (GraphQL REST) |
| `contatos/integration/ContatosCoreDto.kt` | Criar | DTO raw (tudo opcional) |
| `contatos/integration/ContatosMapper.kt` | Criar | DTO → domínio sealed |

---

## 11. Requisitos não funcionais

| Categoria | Requisito não funcional | Meta ou critério |
|---|---|---|
| Performance | Virtual threads habilitadas | `spring.threads.virtual.enabled=true`; ausência de blocos `synchronized` em código próprio |
| Performance | Timeouts Feign | `connectTimeout: 1500ms`, `readTimeout: 1500ms` por padrão |
| Confiabilidade | Token STS renovado proativamente | Renovação 30s antes do vencimento; nunca usar token expirado |
| Confiabilidade | Campos nulos em integrações não quebram a lista | Mapper descarta itens inválidos com log warning |
| Segurança | Credenciais STS não em código | Apenas via `${integration.sts.*}` no YAML de ambiente |
| Segurança | x-api-key não em código | Apenas via `${integration.instituicoes-financeiras.api-key}` |
| Observabilidade | Logs com correlation ID | MDC preservado entre threads (sem executores filhos por ora) |
| Confiabilidade | Valkey indisponível → 5xx | Sem silent-fail; erro propagado ao cliente |
| Compatibilidade | Spring Cloud ↔ Boot 3.5 | Verificar versão compatível em spring.io antes de implementar |

---

## 12. Estratégia de rollout ou migração

- Projeto novo, sem dados existentes nem usuários em produção
- Não há feature flag necessária
- Ambientes configurados via perfis Spring Boot (`local`, `dev`, `hom`, `prod`)
- Valkey e STS devem estar disponíveis em cada ambiente antes do deploy

---

## 13. Estratégia de validação

- **Testes unitários**: um teste por critério de aceite; mockar Feign clients, RedisTemplate e StsClient com mockito-kotlin; exercitar domínio e mappers de verdade (sem mock)
- **Testes de integração**: fora do escopo desta feature (sem infra de testes de integração definida no PRD)
- **Testes e2e / manuais**: subir a app com perfil `local` + STS mock + Valkey local; chamar cada endpoint e verificar respostas
- **Sinais operacionais**: logs de startup do `CaronteRegistry` listando os itens registrados; log de cache miss/hit no `AuthTokenService`

---

## 14. Critérios de aceite

- [ ] GET /entrypoint retorna exatamente as rotas anotadas com `@CaronteMapping`, sem itens hard-coded
- [ ] GET /v1/instituicoes retorna lista mapeada e cacheada; segunda chamada não aciona integração
- [ ] GET /v1/instituicoes aplica nome personalizado e ícone do `instituicoes.yml`; sem ícone → `iconeUrl: null`; sem nome → usa `nome_reduzido`
- [ ] GET /v1/sugestoes retorna sugestões; segunda chamada com mesmo `idPessoa` não aciona integração; TTL 15m
- [ ] GET /v1/sugestoes domínio `DadosDestino` é `ChavePix` quando `chave` presente; `AgenciaConta` quando `ag`/`cc` presentes
- [ ] GET /v1/contatos percorre todas as páginas do cursor e retorna resultado completo
- [ ] GET /v1/contatos sem cache — chamadas consecutivas acionam a integração
- [ ] Campos nulos vindos de sugestões ou contatos não quebram a lista; item descartado com log warning
- [ ] Todas as chamadas Feign (exceto STS) têm header `Authorization: Bearer <token>`
- [ ] Token STS é renovado quando faltam 30s para expirar
- [ ] UsuarioContext com JWT inválido resulta em resposta de erro (não 200)
- [ ] Build e testes passam sem erros (`mvn test`)

---

## 15. Riscos e observações

- **Serialização do sealed interface no Valkey**: `GenericJackson2JsonRedisSerializer` inclui type hints (`@class`) no JSON, o que funciona em Kotlin mas pode gerar JSON verboso. Se houver problema de desserialização, substituir por serializar um DTO simples (`List<SugestaoResponseDto>`) no Valkey e converter para domínio na leitura.
- **Pinning no cache2k**: cache2k 2.6.x pode ter pontos de sincronização internos (`synchronized`) que pinnam virtual threads. Se detectado via `-Djdk.tracePinnedThreads=full`, avaliar upgrade de versão ou substituição por Caffeine (que é VT-safe a partir de 3.x).
- **Versão Spring Cloud**: confirmar a versão exata compatível com Spring Boot 3.5.16 em [spring.io/projects/spring-cloud](https://spring.io/projects/spring-cloud) antes de fixar no `pom.xml`.
- **Cursor de contatos**: campo confirmado como `x-next-cursor` no corpo da resposta; vem `null` ou string vazia quando não há mais páginas. Tratado com `.takeIf { it.isNotEmpty() }` no service.
- **STS offline**: sem circuit breaker configurado; falha no STS paralisa todas as rotas que dependem de Feign. Acceptable para um projeto de testes de performance, mas registrar como débito técnico.

---

## 16. Questões em aberto

Nenhuma — todas as questões anteriores foram resolvidas:
- Campo cursor confirmado: `x-next-cursor`, `null` ou empty = sem mais páginas
- `@ConfigurationPropertiesScan` na `PerformanceApplication` é a abordagem correta para Spring Boot 3.x
- Extensão dos arquivos de configuração: `.yml` (renomear `application.yaml`)
