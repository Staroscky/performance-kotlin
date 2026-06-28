# Project Context

## Resumo do produto

API REST de performance implementada em Kotlin + Spring Boot 3.5 no Java 21 com virtual threads habilitadas. Expõe rotas para testes de performance cobrindo padrões recorrentes de integração via Feign, cache in-process (cache2k) e cache distribuído (Valkey), extração de identidade a partir de JWT e mapeamento automático de entrypoint via anotação.

## Objetivo principal

Servir como base de testes de performance para os padrões de integração com serviços externos, autenticação OAuth2 (STS), cache em camadas e arquitetura vertical slice em Kotlin idiomático.

## Stack e runtime

- Linguagem: Kotlin 1.9.25
- Framework: Spring Boot 3.5 (Web MVC)
- Build: Maven
- Runtime: Java 21 com virtual threads (`spring.threads.virtual.enabled=true`)
- Cache in-process: cache2k 2.6.x (Spring Cache abstraction + uso direto para AuthToken)
- Cache distribuído: Valkey via Spring Data Redis + Lettuce
- HTTP client externo: Spring Cloud OpenFeign
- Autenticação de saída: STS (OAuth2 client_credentials)
- Testes: JUnit 5 + mockito-kotlin (nunca MockK)

## Arquitetura e convenções

- **Vertical Slice Architecture (VSA)**: cada rota é um pacote autônomo com `controller`, `domain`, `service` e `integration`. Não há pacotes globais por camada.
- **Pacote raiz**: `com.staroscky.performance`
- **Pacote `core`**: beans compartilhados entre slices (`UsuarioContext`, `AuthTokenService`, `CaronteRegistry`). Slices importam somente interfaces de `core`, não implementações internas umas das outras.
- **Pureza do domínio**: `domain` não importa `org.springframework`, `feign` nem clientes HTTP/Redis.
- **Sealed interfaces** para variantes de domínio (ex.: `ChavePix` vs `AgenciaConta`) e resultados de caso de uso.
- **Value classes** para tipos com semântica forte (ex.: `ISPB`).
- Configuração por ambiente: `application.yml` + `application-{local,dev,hom,prod}.yml`.
- Todas as propriedades de integração chegam via `${integration.*}` no YAML do ambiente.

## Domínio e regras recorrentes

- **UsuarioContext**: bean `@RequestScope` que decodifica o JWT do header `Authorization` e expõe `idPessoa: UUID` e `idConta: UUID` (extraídos dos claims). A assinatura do token é considerada já validada pelo gateway — aqui apenas decodificamos.
- **CaronteMapping**: anotação aplicada na controller para registrar a rota no `/entrypoint`. O `rel` vem da anotação; o `href` é lido automaticamente do `@RequestMapping` da controller. Registros coletados no startup.
- **AuthTokenService**: token OAuth2 do STS cacheado no cache2k até 30s antes do vencimento (TTL dinâmico por entrada). Injetado em todos os Feign via `RequestInterceptor`.
- **ISPB**: sempre 8 dígitos. Personalização de nome e ícone por ISPB carregada de `resources/config/instituicoes.yml`.
- **DadosDestino** (sugestões e contatos): `sealed interface` com variantes `ChavePix(chave)` e `AgenciaConta(ag, conta, dac, tipoConta)`. Cada slice define seu próprio tipo para manter isolamento VSA.
- Campos de integrações externas podem ser nulos mesmo quando obrigatórios → todos os campos do DTO de integração são opcionais (`String?`, `UUID?`). A obrigatoriedade é validada no mapeamento para o domínio da slice.

## Integrações e dependências externas

| Serviço | Protocolo | Propósito |
|---|---|---|
| STS | REST POST (OAuth2 client_credentials) | Token Bearer para demais chamadas Feign |
| instituicoes-financeiras | REST GET | Lista de instituições financeiras |
| inteligencia | REST GET | Sugestões de transferência por usuário |
| contatos-core | REST GET (GraphQL) | Contatos do usuário com paginação cursor |

Todas as URLs e chaves de API chegam via `${integration.*}` no YAML de ambiente.

## Riscos e cuidados recorrentes

- **Pinning de virtual threads**: cache2k pode usar sincronização interna; Lettuce é não-bloqueante e VT-safe. Nunca usar `synchronized` em código próprio — usar `ReentrantLock`. Monitorar com `-Djdk.tracePinnedThreads=full` em dev.
- **MDC em threads filhas**: propagar `MDC.getCopyOfContextMap()` ao lançar trabalho em outros executores para não perder correlation IDs em logs.
- **Nulidade nas integrações**: modelar DTOs de integração com todos os campos opcionais e validar obrigatoriedade no mapeamento para domínio — não propagar null para o domínio.
- **JWT sem re-validação**: o bearer token é apenas decodificado (sem verificação de assinatura), pois assume-se validação prévia por API gateway.
