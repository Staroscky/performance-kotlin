# SPEC — Observabilidade HTTP (CorrelationId + Logbook)

## 1. Contexto da solicitação

### 1.1 História ou tarefa do usuário

- **Solicitante:** Ederson Staroscky
- **Tipo:** melhoria de infraestrutura transversal
- **História:** Como operador da API, quero que cada requisição carregue um `x-correlationId` rastreável nos logs e propagado nas chamadas Feign de saída, e que os payloads HTTP de entrada (controllers) e saída (Feign clients) sejam logados automaticamente via Logbook Zalando, para que eu consiga correlacionar e inspecionar fluxos completos em ambientes de performance e produção.
- **Valor esperado:** Rastreabilidade de ponta a ponta por requisição, sem instrumentação manual em cada rota.

### 1.2 Problema observado

Hoje a aplicação não tem rastreabilidade entre requests. Logs de uma mesma requisição não compartilham nenhuma chave de correlação, impossibilitando correlacionar chamadas ao STS, às integrações Feign e às respostas dos controllers. Também não há visibilidade dos payloads HTTP trafegados — inviabilizando diagnóstico de performance, erros de contrato e latência em integração.

### 1.3 Objetivo da entrega

A entrega é concluída quando:
- O `x-correlationId` recebido no header de entrada (ou gerado como UUID quando ausente) aparece em todos os logs daquela requisição.
- O header `x-correlationId` é propagado nas chamadas Feign aos serviços de negócio.
- Requests e responses das controllers e dos Feign clients são logados automaticamente via Logbook.
- STS client não tem seu body logado (evita exposição de credentials).
- Um `MdcTaskDecorator` reutilizável é disponibilizado em `core` como infraestrutura para propagação de MDC em futuros `ThreadPoolTaskExecutor`.

---

## 2. Objetivo técnico

Adicionar, em `core`, três componentes transversais novos:
1. Um filtro Servlet que lê `x-correlationId` do header de entrada (ou gera UUID se ausente) e popula o MDC, limpando ao final.
2. Um `RequestInterceptor` Feign que propaga o `correlationId` do MDC como header de saída.
3. Um `MdcTaskDecorator` reutilizável para propagação de MDC em `ThreadPoolTaskExecutor` (não wired por padrão — infraestrutura defensiva para uso futuro).

Além disso, configurar o Logbook Zalando como logger HTTP para Spring MVC (entrada) e para os Feign clients de negócio (saída), excluindo o `StsClient` para não expor credentials em log.

---

## 3. Estado atual

### Feign Interceptor (`core/feign/AuthRequestInterceptor.kt`)
Único interceptor Feign existente. `@Component`, aplicado globalmente a todos os clientes. Injeta `Authorization: Bearer <token>` lendo de `AuthTokenService`.

### FeignClients registrados
| Client | Arquivo | Observação |
|---|---|---|
| `StsClient` | `core/auth/StsClient.kt` | Sem `configuration` explícita; usa URL direta `${sts.url}` |
| `InstituicoesFinanceirasClient` | `instituicoes/integration/InstituicoesFinanceirasClient.kt` | Sem `configuration` explícita |
| `InteligenciaClient` | `sugestoes/integration/InteligenciaClient.kt` | Sem `configuration` explícita |
| `ContatosCoreClient` | `contatos/integration/ContatosCoreClient.kt` | Sem `configuration` explícita |

### MDC e correlationId
Nenhum filtro, interceptor ou configuração de MDC existe. A chave `correlationId` está mencionada em `PROJECT-CONTEXT.md` como cuidado operacional, mas não implementada.

### Logbook
Nenhuma dependência ou configuração de Logbook no projeto.

### Logging
Sem `logback-spring.xml`. Padrão Spring Boot ativo. Nenhuma referência ao MDC no padrão de log atual.

### Virtual threads e MDC (`InheritableThreadLocal`)
`spring.threads.virtual.enabled: true` (em `application.yml`). **Investigação realizada:** Logback 1.5.x (usado pelo Spring Boot 3.5.x) usa `InheritableThreadLocal` em `LogbackMDCAdapter`. Virtual threads criadas via `Thread.ofVirtual()` são threads Java comuns e herdam `InheritableThreadLocal` do thread pai automaticamente. No fluxo síncrono desta aplicação (request → Feign → response no mesmo VT), MDC propaga sem instrumentação adicional.

**Exceção:** `ThreadPoolTaskExecutor` (pool fixo) não se beneficia dessa herança, pois as threads do pool são criadas antes do MDC ser populado. `SimpleAsyncTaskExecutor` com virtual threads (auto-config do Spring Boot 3.2+) não suporta `TaskDecorator` mas também não precisa — cria novo VT por task e herda MDC via `InheritableThreadLocal`. O `MdcTaskDecorator` deste PR é infraestrutura para uso explícito em futuros `ThreadPoolTaskExecutor`.

---

## 4. Escopo da solução

### 4.1 O que muda

| Área | Estado atual | Estado esperado | Impacto |
|---|---|---|---|
| `pom.xml` | Sem logbook | Adicionar `logbook-spring-boot-starter` e `logbook-feign` | Baixo |
| `core/filter/CorrelationIdFilter.kt` | Não existe | Novo filtro `OncePerRequestFilter` com MDC — gera UUID se header ausente | Baixo |
| `core/feign/CorrelationIdRequestInterceptor.kt` | Não existe | Novo `RequestInterceptor` Feign que lê MDC e injeta header | Baixo |
| `core/feign/LogbookFeignConfig.kt` | Não existe | Nova config Feign que registra `FeignLogbookLogger` | Baixo |
| `core/async/MdcTaskDecorator.kt` | Não existe | `TaskDecorator` que copia e restaura MDC — para uso em `ThreadPoolTaskExecutor` | Baixo |
| `InstituicoesFinanceirasClient.kt` | Sem `configuration` | Adicionar `configuration = [LogbookFeignConfig::class]` | Baixo |
| `InteligenciaClient.kt` | Sem `configuration` | Adicionar `configuration = [LogbookFeignConfig::class]` | Baixo |
| `ContatosCoreClient.kt` | Sem `configuration` | Adicionar `configuration = [LogbookFeignConfig::class]` | Baixo |
| `application.yml` | Sem config logbook/logLevel | Adicionar bloco `logbook:` e `loggerLevel` default/STS | Baixo |
| `application-local.yml` | Sem config de log | Adicionar padrão de log amigável com `%X{correlationId:-}` | Baixo |
| `application-dev.yml` | Sem config de log | Adicionar `logging.structured.format.console: ecs` (JSON) | Baixo |
| `application-hom.yml` | Sem config de log | Adicionar `logging.structured.format.console: ecs` (JSON) | Baixo |
| `application-prod.yml` | Sem config de log | Adicionar `logging.structured.format.console: ecs` (JSON) | Baixo |

### 4.2 O que não muda

- Nenhuma regra de negócio ou rota existente é alterada.
- `StsClient` não recebe logbook — sem modificação nessa interface.
- `AuthRequestInterceptor` não é modificado.
- `UsuarioContext` não é modificado.
- Estrutura VSA de slices não é alterada — mudanças ficam em `core/` e nos `@FeignClient` de cada slice.
- Nenhum cache ou TTL é alterado.
- Nenhuma alteração em ambiente (`application-{local,dev,hom,prod}.yml`).

### 4.3 Restrições e pressupostos

- Quando o header `x-correlationId` está ausente, o filtro gera um UUID v4 aleatório para garantir rastreabilidade mesmo em requests sem correlationId do chamador.
- Virtual threads: fluxo síncrono, MDC propaga via `InheritableThreadLocal` (Logback 1.5.x). O `MdcTaskDecorator` é infraestrutura — não substitui o fluxo padrão, mas está disponível para qualquer `ThreadPoolTaskExecutor` que seja configurado no futuro.
- `LogbookFeignConfig` não deve ser anotado com `@Component` — deve ser exclusivamente configuração de Feign client (instanciado no contexto filho do Feign), para evitar registro como bean global.
- Logbook para Spring MVC é configurado automaticamente pelo `logbook-spring-boot-starter` via `LogbookFilter` Servlet — nenhum filtro manual necessário para controllers.
- Versão do Logbook: usar 3.x (compatível com Spring Boot 3.x).

---

## 5. Requisitos funcionais

| ID | Requisito funcional | Prioridade | Origem |
|---|---|---|---|
| RF-01 | O filtro deve ler o header `x-correlationId` e popular `MDC["correlationId"]` antes de qualquer processamento da requisição | must | PRD |
| RF-02 | O MDC deve ser limpo ao final de cada requisição (bloco `finally`) | must | PRD |
| RF-03 | O padrão de log deve incluir o valor de `MDC["correlationId"]` em cada linha logada | must | PRD |
| RF-04 | O `CorrelationIdRequestInterceptor` deve ler `MDC["correlationId"]` e injetar `x-correlationId` no header de saída de todo request Feign | must | PRD |
| RF-05 | O Logbook deve logar request e response HTTP das controllers (inbound) | must | PRD |
| RF-06 | O Logbook deve logar request e response HTTP dos Feign clients de negócio (outbound): `InstituicoesFinanceirasClient`, `InteligenciaClient`, `ContatosCoreClient` | must | PRD |
| RF-07 | O `StsClient` não deve ter seus requests/responses logados pelo Logbook | must | PRD |
| RF-08 | Quando `x-correlationId` estiver ausente no header de entrada, o filtro deve gerar um UUID v4 aleatório, populá-lo no MDC e prosseguir sem erro | must | usuário |

---

## 6. Cenários e fluxos esperados

### 6.1 Cenários principais

**Cenário 1 — Request com correlationId:**
1. Cliente chama `GET /v1/instituicoes` com `x-correlationId: 550e8400-e29b-41d4-a716-446655440000`.
2. `CorrelationIdFilter` popula `MDC["correlationId"] = "550e8400-..."`.
3. Logbook registra o request de entrada.
4. `InstituicoesService` é chamado e gera logs com `[550e8400-...]` no padrão.
5. `InstituicoesFinanceirasClient` é acionado; `CorrelationIdRequestInterceptor` injeta `x-correlationId: 550e8400-...` no header.
6. Logbook registra o request e response do Feign.
7. Resposta retorna ao cliente; Logbook registra o response de saída.
8. `finally` do filtro: `MDC.remove("correlationId")`.

**Cenário 2 — Request sem correlationId:**
1. Header `x-correlationId` ausente.
2. `CorrelationIdFilter` gera `UUID.randomUUID().toString()` e popula `MDC["correlationId"]`.
3. Logs aparecem com UUID gerado. Nenhuma exceção.
4. `CorrelationIdRequestInterceptor` injeta o UUID gerado como `x-correlationId` nos Feign de saída.

### 6.2 Edge cases e falhas esperadas

- **correlationId ausente:** filtro gera UUID v4, sem erro — rastreabilidade garantida mesmo sem header.
- **correlationId inválido (não UUID):** filtro aceita e propaga como string (sem validação de formato — responsabilidade do chamador).
- **Erro dentro do handler (5xx):** MDC deve ser limpo normalmente no `finally` — o erro não afeta a limpeza.
- **STS chamado pelo `AuthTokenService`:** `CorrelationIdRequestInterceptor` é `@Component`, portanto aplica-se também ao STS. O header `x-correlationId` será enviado ao STS — isso é inofensivo (STS ignora headers desconhecidos). O Logbook, porém, NÃO está configurado para STS (sem `LogbookFeignConfig` na anotação).

---

## 7. Alternativas consideradas

### 7.1 Alternativa escolhida

**CorrelationId via `OncePerRequestFilter` + MDC + `@Component`**

Filtro Spring MVC com `@Order(Ordered.HIGHEST_PRECEDENCE)` garante execução antes de qualquer lógica de negócio. MDC é o mecanismo padrão para contexto de thread em Logback/Slf4j. Simples, zero dependência adicional, compatível com virtual threads no fluxo síncrono.

**Logbook Feign via `configuration` por `@FeignClient`**

`LogbookFeignConfig` é adicionado apenas nos clients de negócio. Mantém STS sem log de credentials. Explícito e rastreável — qualquer dev sabe que o cliente tem logbook habilitado olhando a anotação.

### 7.2 Alternativas descartadas

| Alternativa | Vantagens | Desvantagens | Motivo da não escolha |
|---|---|---|---|
| Micrometer Tracing / OpenTelemetry | Tracing distribuído completo com spans e traces | Overhead de setup, propagação via B3/W3C (não `x-correlationId`), muda contratos de header | Fora do escopo; o header é `x-correlationId` específico |
| Logbook para Feign via `logbook-httpclient5` | Intercepta no nível do Apache HC5 | Todos os clientes HC5 são logados, difícil excluir STS; menos idiomático em contexto Feign | Logbook-feign oferece exclusão por cliente mais natural |
| Logbook default global + exclusão de STS por URL | Zero mudança nos `@FeignClient` | Configuração implícita; exclusão por URL é frágil (depende de path pattern correto por ambiente) | Risco de credencial exposta se URL mudar; configuração explícita é preferível |
| `TaskDecorator` via `VirtualThreadTaskExecutor` | Decorator nativo com VT | `VirtualThreadTaskExecutor` não suporta `TaskDecorator`; MDC propaga via `InheritableThreadLocal` de qualquer forma | Não aplicável; `MdcTaskDecorator` é para `ThreadPoolTaskExecutor` apenas |

---

## 8. Design da solução

### 8.1 Visão geral da abordagem

```
pom.xml                  → adicionar logbook-spring-boot-starter + logbook-feign
core/filter/             → CorrelationIdFilter (novo — gera UUID se header ausente)
core/feign/              → CorrelationIdRequestInterceptor (novo) + LogbookFeignConfig (novo)
core/async/              → MdcTaskDecorator (novo — infraestrutura, não wired por padrão)
*Client.kt (3 arquivos)  → adicionar configuration = [LogbookFeignConfig::class]
application.yml          → logbook config + loggerLevel por client + log pattern MDC
```

### 8.2 `CorrelationIdFilter` (`core/filter/CorrelationIdFilter.kt`)

```kotlin
package com.staroscky.performance.core.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationIdFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val correlationId = request.getHeader(HEADER_NAME) ?: UUID.randomUUID().toString()
        MDC.put(MDC_KEY, correlationId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(MDC_KEY)
        }
    }

    companion object {
        const val HEADER_NAME = "x-correlationId"
        const val MDC_KEY = "correlationId"
    }
}
```

### 8.3 `CorrelationIdRequestInterceptor` (`core/feign/CorrelationIdRequestInterceptor.kt`)

```kotlin
package com.staroscky.performance.core.feign

import feign.RequestInterceptor
import feign.RequestTemplate
import org.slf4j.MDC
import org.springframework.stereotype.Component

@Component
class CorrelationIdRequestInterceptor : RequestInterceptor {

    override fun apply(template: RequestTemplate) {
        val correlationId = MDC.get(CORRELATION_MDC_KEY)
        if (!correlationId.isNullOrBlank()) {
            template.header(HEADER_NAME, correlationId)
        }
    }

    companion object {
        private const val HEADER_NAME = "x-correlationId"
        private const val CORRELATION_MDC_KEY = "correlationId"
    }
}
```

> Nota: este interceptor é `@Component` e aplica-se ao `StsClient` também. O STS ignora headers desconhecidos — sem impacto funcional.

### 8.4 `LogbookFeignConfig` (`core/feign/LogbookFeignConfig.kt`)

```kotlin
package com.staroscky.performance.core.feign

import feign.Logger
import org.zalando.logbook.Logbook
import org.zalando.logbook.openfeign.FeignLogbookLogger

class LogbookFeignConfig(private val logbook: Logbook) {
    fun feignLogger(): Logger = FeignLogbookLogger(logbook)
}
```

> **Não** deve ter `@Configuration` nem `@Component` na classe em si — é passada como parâmetro `configuration` no `@FeignClient`. Se anotada como bean global, o Spring registrará um `FeignLogbookLogger` para todos os clients, incluindo STS.

### 8.5 Anotação `@FeignClient` dos clients de negócio

```kotlin
// InstituicoesFinanceirasClient.kt
@FeignClient(name = "instituicoes-financeiras", configuration = [LogbookFeignConfig::class])

// InteligenciaClient.kt
@FeignClient(name = "inteligencia", configuration = [LogbookFeignConfig::class])

// ContatosCoreClient.kt
@FeignClient(name = "contatos-core", configuration = [LogbookFeignConfig::class])
```

`StsClient` permanece sem `configuration`.

### 8.7 `MdcTaskDecorator` (`core/async/MdcTaskDecorator.kt`)

Infraestrutura defensiva para propagação de MDC em `ThreadPoolTaskExecutor`. **Não é wired automaticamente** — deve ser passado explicitamente ao configurar um executor.

```kotlin
package com.staroscky.performance.core.async

import org.slf4j.MDC
import org.springframework.core.task.TaskDecorator
import org.springframework.stereotype.Component

@Component
class MdcTaskDecorator : TaskDecorator {

    override fun decorate(runnable: Runnable): Runnable {
        val contextMap = MDC.getCopyOfContextMap() ?: emptyMap()
        return Runnable {
            MDC.setContextMap(contextMap)
            try {
                runnable.run()
            } finally {
                MDC.clear()
            }
        }
    }
}
```

**Quando usar:** ao configurar qualquer `ThreadPoolTaskExecutor` futuro (ex.: `@Async` com pool fixo):
```kotlin
// Exemplo de uso futuro — NÃO faz parte desta feature
@Bean
fun asyncExecutor(mdcTaskDecorator: MdcTaskDecorator): ThreadPoolTaskExecutor =
    ThreadPoolTaskExecutor().apply {
        setTaskDecorator(mdcTaskDecorator)
        initialize()
    }
```

**Por que NÃO é necessário para virtual threads:** `SimpleAsyncTaskExecutor` (auto-config do Spring Boot 3.2+ com VT) cria um novo virtual thread por task. O novo VT herda `InheritableThreadLocal` do thread pai (Logback 1.5.x usa `InheritableThreadLocal` no `LogbackMDCAdapter`), então MDC propaga sem decorator. `SimpleAsyncTaskExecutor` também não suporta `TaskDecorator`.

### 8.6 Contratos, dados e interfaces

#### Dependências novas (`pom.xml`)

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>logbook-spring-boot-starter</artifactId>
    <version>${logbook.version}</version>
</dependency>
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>logbook-feign</artifactId>
    <version>${logbook.version}</version>
</dependency>
```

Adicionar property: `<logbook.version>3.10.0</logbook.version>` (ou última 3.x compatível com Spring Boot 3.5).

#### `application.yml` — adições

```yaml
logbook:
  filter:
    enabled: true
  format:
    style: http
  include:
    - /**

spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            loggerLevel: FULL   # aplica a todos os clients; novos clients herdam automaticamente
          sts:
            loggerLevel: NONE   # sobrescreve apenas o STS — evita log de credentials
```

#### `application-local.yml` — log amigável com correlationId

```yaml
logging:
  pattern:
    console: "%d{yyyy-MM-dd'T'HH:mm:ss.SSSZ} [%thread] %-5level [%X{correlationId:-}] %logger{36} - %msg%n"
```

#### `application-dev.yml`, `application-hom.yml`, `application-prod.yml` — JSON estruturado

```yaml
logging:
  structured:
    format:
      console: ecs
```

> **Por que ECS?** O formato `ecs` (Elastic Common Schema) é suportado nativamente pelo Spring Boot 3.4+ sem dependência adicional. O MDC é incluído automaticamente como campos de primeiro nível no JSON — `correlationId` aparecerá como `"correlationId": "..."` sem nenhuma config extra. Alternativa: `logstash` (mesmo mecanismo, schema diferente). Ambos são compatíveis com ferramentas de log aggregation (ELK, Datadog, Grafana Loki).

---

## 9. Fluxos técnicos

```text
Inbound (controller):

  Client ──→ [CorrelationIdFilter]
               ├─ MDC.put("correlationId", header)
               ├─ [LogbookFilter] → loga request HTTP
               ├─ Controller → Service → FeignClient
               │     └─ [CorrelationIdRequestInterceptor] → header x-correlationId outbound
               │     └─ [LogbookFeignLogger] → loga request/response Feign
               ├─ [LogbookFilter] → loga response HTTP
               └─ MDC.remove("correlationId")
```

```text
STS (auth interno):

  AuthTokenService ──→ StsClient
                          ├─ [AuthRequestInterceptor] → Authorization: Bearer ...
                          ├─ [CorrelationIdRequestInterceptor] → x-correlationId (inofensivo)
                          └─ SEM LogbookFeignLogger → body de credentials não logado
```

---

## 10. Arquivos afetados

| Arquivo | Tipo | Mudança |
|---|---|---|
| `pom.xml` | Modificar | Adicionar `logbook-spring-boot-starter`, `logbook-feign`, property `logbook.version` |
| `src/main/resources/application.yml` | Modificar | Bloco `logbook:`, `loggerLevel` default + STS |
| `src/main/resources/application-local.yml` | Modificar | Padrão de log amigável com `%X{correlationId:-}` |
| `src/main/resources/application-dev.yml` | Modificar | `logging.structured.format.console: ecs` (JSON) |
| `src/main/resources/application-hom.yml` | Modificar | `logging.structured.format.console: ecs` (JSON) |
| `src/main/resources/application-prod.yml` | Modificar | `logging.structured.format.console: ecs` (JSON) |
| `core/filter/CorrelationIdFilter.kt` | Criar | `OncePerRequestFilter` que popula/limpa MDC com `x-correlationId` |
| `core/feign/CorrelationIdRequestInterceptor.kt` | Criar | `RequestInterceptor` que lê MDC e injeta header `x-correlationId` nos Feign de saída |
| `core/feign/LogbookFeignConfig.kt` | Criar | Configuração Feign com `FeignLogbookLogger`; sem anotações de bean Spring |
| `core/async/MdcTaskDecorator.kt` | Criar | `TaskDecorator` para propagação de MDC em `ThreadPoolTaskExecutor` |
| `instituicoes/integration/InstituicoesFinanceirasClient.kt` | Modificar | Adicionar `configuration = [LogbookFeignConfig::class]` |
| `sugestoes/integration/InteligenciaClient.kt` | Modificar | Adicionar `configuration = [LogbookFeignConfig::class]` |
| `contatos/integration/ContatosCoreClient.kt` | Modificar | Adicionar `configuration = [LogbookFeignConfig::class]` |

---

## 11. Requisitos não funcionais

| Categoria | Requisito não funcional | Meta ou critério |
|---|---|---|
| Segurança | STS client não pode ter credentials logadas | Verificar ausência de `client_secret` nos logs durante testes |
| Performance | Logbook não deve impactar latência perceptível em operação normal | Overhead esperado < 1ms por request em ambiente local; validar sob carga na suite de performance |
| Confiabilidade | MDC deve ser limpo ao final de cada request, inclusive em erros 5xx | Testar via request que provoca exceção — verificar que thread seguinte não herda correlationId |
| Observabilidade | Todos os logs de uma requisição devem compartilhar o mesmo `correlationId` | Verificável por grep do UUID nos logs de uma requisição isolada |
| Compatibilidade | Funcionar com virtual threads habilitadas (`spring.threads.virtual.enabled: true`) | Logback 1.5.x usa `InheritableThreadLocal`; MDC propaga para VT filhas automaticamente. `MdcTaskDecorator` cobre cenários de `ThreadPoolTaskExecutor` |

---

## 12. Estratégia de rollout ou migração

- Nenhuma flag de feature necessária — a mudança é aditiva e não quebra contratos.
- Clientes sem o header `x-correlationId` continuam funcionando (campo vazio no MDC).
- Dependências Logbook são adicionadas sem conflito com a stack existente.
- Não há migração de dados ou schema.

---

## 13. Estratégia de validação

- **Testes unitários:**
  - `CorrelationIdFilter`: verificar que MDC é populado com o valor do header; que UUID é gerado quando header está ausente; que MDC é limpo no `finally`; que request sem header não lança exceção.
  - `CorrelationIdRequestInterceptor`: verificar que header `x-correlationId` é injetado quando MDC contém valor; que nenhum header é injetado quando MDC está vazio.

- **Testes de integração:** fora do escopo desta feature (nenhum teste de integração real com Logbook mock necessário).

- **Validação manual:**
  - Subir aplicação local e enviar `curl -H "x-correlationId: test-uuid-123" GET /v1/instituicoes`.
  - Verificar nos logs: padrão inclui `[test-uuid-123]`, request/response loggados pelo Logbook, header presente no log do Feign de saída.
  - Verificar que logs do STS (se qualquer log aparecer) não contêm `client_secret`.

- **Sinais operacionais:** grep do `correlationId` nos logs de produção para validar presença consistente.

---

## 14. Critérios de aceite

- [ ] `CorrelationIdFilter` popula `MDC["correlationId"]` com o valor do header `x-correlationId` antes do processamento.
- [ ] `CorrelationIdFilter` limpa o MDC no `finally`, mesmo em caso de exceção.
- [ ] Quando `x-correlationId` está ausente, o filtro gera um UUID v4 válido no MDC e não lança exceção.
- [ ] Padrão de log inclui `correlationId` do MDC em cada linha.
- [ ] Header `x-correlationId` está presente nos headers de saída de `InstituicoesFinanceirasClient`, `InteligenciaClient` e `ContatosCoreClient`.
- [ ] Logbook loga request e response das controllers em nível HTTP.
- [ ] Logbook loga request e response dos três Feign clients de negócio.
- [ ] Logs do `StsClient` não contêm `client_secret` nem body da autenticação.
- [ ] Build compila sem erros e testes unitários passam.
- [ ] Nenhuma regressão nas rotas existentes.

---

## 15. Riscos e observações

- **`LogbookFeignConfig` como bean global acidental:** se for anotada com `@Configuration` ou `@Component`, o Spring Boot a registra globalmente e o `StsClient` passa a ser logado. Implementador deve garantir ausência de anotações de componente.
- **Versão do Logbook:** 3.x é compatível com Spring Boot 3.x. A versão exata deve ser verificada no Maven Central para compatibilidade com Spring Cloud OpenFeign 2023.0.x na data de implementação.
- **MDC e virtual threads:** sem risco no fluxo síncrono deste PR. Logback 1.5.x usa `InheritableThreadLocal`, portanto VTs filhas herdam MDC automaticamente. Para futuros `ThreadPoolTaskExecutor`, usar o `MdcTaskDecorator` introduzido nesta feature.
- **`loggerLevel: FULL` no Feign:** necessário para o `FeignLogbookLogger` receber os dados completos de request/response. Sem este nível, o Feign não passa o body ao logger.

## 16. Questões em aberto

- Nenhuma.
