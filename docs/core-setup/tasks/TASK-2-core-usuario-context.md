# TASK-2 — Core: UsuarioContext

**Arquivo alvo:** `src/main/kotlin/com/staroscky/performance/core/UsuarioContext.kt` (novo)
**Referência SPEC:** Seções 5 (RF-01), 8.4 (UsuarioContext)
**Depende de:** TASK-1
**Bloqueada por:** nenhuma

---

## Contexto

Todas as rotas que precisam da identidade do usuário (sugestões, contatos) dependem de `idPessoa` e `idConta` extraídos do JWT do header `Authorization`. Este bean `@RequestScope` centraliza essa extração para todas as slices.

## O que fazer

Criar `com.staroscky.performance.core.UsuarioContext`:

```kotlin
@Component
@RequestScope
class UsuarioContext(request: HttpServletRequest) {

    val idPessoa: UUID
    val idConta: UUID

    init {
        val token = request.getHeader("Authorization")
            ?.removePrefix("Bearer ")
            ?: error("Header Authorization ausente")
        val payloadJson = String(
            Base64.getUrlDecoder().decode(token.split(".").getOrElse(1) { "" })
        )
        val claims = ObjectMapper().readValue(payloadJson, Map::class.java)
        idPessoa = UUID.fromString(
            claims["idPessoa"] as? String ?: error("Claim idPessoa ausente")
        )
        idConta = UUID.fromString(
            claims["idConta"] as? String ?: error("Claim idConta ausente")
        )
    }
}
```

## Notas de implementação

- Sem biblioteca JWT — Base64.getUrlDecoder() da JDK é suficiente para decodificar o payload; a assinatura não é verificada aqui (responsabilidade do gateway)
- `error()` em `init` lança `IllegalStateException`, que Spring mapeia como 5xx por padrão — se preferir 4xx, adicionar `@ControllerAdvice` separado (fora do escopo desta task)
- O `ObjectMapper` aqui é criado localmente; se performance for crítica, injetar o bean `ObjectMapper` do contexto Spring
- O bean tem escopo de request (`@RequestScope`) — não pode ser injetado em singletons diretamente; use `ObjectProvider<UsuarioContext>` ou injeção por construtor em beans de escopo de request/prototype

## Critério de aceite

- [ ] `UsuarioContext` está em `com.staroscky.performance.core`
- [ ] Teste: `deve extrair idPessoa e idConta de JWT válido`
- [ ] Teste: `deve lançar erro quando header Authorization está ausente`
- [ ] Teste: `deve lançar erro quando claim idPessoa está ausente no payload`
- [ ] Build e testes passam sem erros
