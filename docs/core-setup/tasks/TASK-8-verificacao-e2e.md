# TASK-8 — Verificação E2E

**Arquivo alvo:** nenhum (verificação manual e revisão de build)
**Referência SPEC:** Seções 13 (Estratégia de validação), 14 (Critérios de aceite)
**Depende de:** TASK-1, TASK-2, TASK-3, TASK-4, TASK-5, TASK-6, TASK-7
**Bloqueada por:** todas as tasks anteriores

---

## Contexto

Após todas as slices implementadas, verificar que a aplicação sobe, todas as rotas respondem corretamente e o build passa limpo. Também verificar riscos documentados na SPEC (pinning de VT, serialização sealed no Valkey).

## O que fazer

### 1. Build limpo

```bash
mvn clean test
```

Todos os testes devem passar sem erros.

### 2. Inicialização com perfil local

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Verificar no log de startup:
- `CaronteRegistry: 3 itens registrados → [contatos, instituicoes, sugestoes]`
- Nenhuma exceção de configuração (`NoSuchBeanDefinitionException`, `BeanCreationException`)

### 3. Smoke test dos endpoints

Usando um JWT de teste com claims `idPessoa` e `idConta` válidos (UUIDs):

```bash
TOKEN="Bearer eyJ..."  # JWT com claims idPessoa e idConta

# Entrypoint
curl -s http://localhost:8080/entrypoint | jq .
# Esperado: {"data":[{rel:contatos, ...},{rel:instituicoes,...},{rel:sugestoes,...}]}

# Instituições (sem JWT necessário)
curl -s http://localhost:8080/v1/instituicoes | jq .
# Esperado: {"data":[{iconeUrl, ispb, nome, searchable}]}

# Segunda chamada de instituições — deve vir do cache (verificar ausência de log de integração)
curl -s http://localhost:8080/v1/instituicoes | jq .

# Sugestões (requer JWT)
curl -s -H "Authorization: $TOKEN" http://localhost:8080/v1/sugestoes | jq .

# Segunda chamada — deve vir do Valkey
curl -s -H "Authorization: $TOKEN" http://localhost:8080/v1/sugestoes | jq .

# Contatos (requer JWT)
curl -s -H "Authorization: $TOKEN" http://localhost:8080/v1/contatos | jq .
```

### 4. Verificação de pinning de virtual threads

Adicionar temporariamente em `application-local.yml` para dev:

```yaml
spring:
  jvm:
    args: -Djdk.tracePinnedThreads=full
```

Ou iniciar com:

```bash
JAVA_TOOL_OPTIONS="-Djdk.tracePinnedThreads=full" mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Fazer chamadas e observar o console. Se aparecer `VirtualThread[...] pinned at ...` proveniente de `cache2k`, registrar como issue e avaliar migração para Caffeine.

### 5. Verificação de serialização sealed no Valkey

Após chamar GET /v1/sugestoes, acessar o Valkey diretamente:

```bash
redis-cli get "sugestoes:<idPessoa>"
```

Confirmar que o JSON armazenado é legível e contém os type hints `@class`. Fazer nova chamada após reiniciar a aplicação e confirmar que o cache ainda é desserializado corretamente (ou se há erro de desserialização do sealed).

### 6. Verificação da tabela do entrypoint

Confirmar que o GET /entrypoint lista exatamente:
- `{"rel": "contatos", "href": "/v1/contatos"}`
- `{"rel": "instituicoes", "href": "/v1/instituicoes"}`
- `{"rel": "sugestoes", "href": "/v1/sugestoes"}`

## Critério de aceite

- [ ] `mvn clean test` passa sem erros
- [ ] App sobe com perfil `local` sem exceções
- [ ] Log de startup mostra os 3 itens do `CaronteRegistry`
- [ ] GET /entrypoint retorna os 3 itens esperados
- [ ] GET /v1/instituicoes retorna lista e segunda chamada não aciona integração
- [ ] GET /v1/sugestoes retorna lista com domínio sealed e segunda chamada usa Valkey
- [ ] GET /v1/contatos retorna lista agregada de todas as páginas
- [ ] Nenhum pinning de virtual threads detectado em chamadas normais (ou issue registrado se detectado)
- [ ] Serialização do sealed interface no Valkey funciona corretamente após restart (ou alternativa de DTO documentada)
