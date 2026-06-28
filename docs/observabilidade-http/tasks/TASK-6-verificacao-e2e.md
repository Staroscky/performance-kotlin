# TASK-6 — Verificação E2E

**Arquivo alvo:** nenhum (verificação manual)
**Referência SPEC:** Seções 13, 14
**Depende de:** TASK-1, TASK-2, TASK-3, TASK-4, TASK-5
**Bloqueada por:** TASK-1 a TASK-5

---

## Contexto

Validação de ponta a ponta em ambiente local para confirmar que todos os critérios de aceite da SPEC são observáveis em runtime. Confirma tanto o caminho feliz (com header) quanto o fallback (sem header) e verifica que o STS não expõe credentials nos logs.

## O que fazer

### Pré-requisito

Subir a aplicação com perfil `local` e os stubs/WireMock ativos (conforme `docs/ambiente-local`).

### Cenário 1 — Request com `x-correlationId`

```bash
curl -v \
  -H "x-correlationId: 550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer <jwt-local>" \
  http://localhost:8080/v1/instituicoes
```

**Verificar nos logs:**
- [ ] Padrão de log local inclui `[550e8400-e29b-41d4-a716-446655440000]` em todas as linhas da requisição
- [ ] Logbook registra o request HTTP de entrada (método, path, headers)
- [ ] Logbook registra o response HTTP de saída (status, body)
- [ ] Logbook registra o request Feign de saída para `instituicoes-financeiras` com header `x-correlationId: 550e8400-...`
- [ ] Logbook registra o response Feign recebido de `instituicoes-financeiras`

### Cenário 2 — Request sem `x-correlationId`

```bash
curl -v \
  -H "Authorization: Bearer <jwt-local>" \
  http://localhost:8080/v1/sugestoes
```

**Verificar nos logs:**
- [ ] Um UUID v4 foi gerado e aparece nos logs de toda a requisição (não blank, formato `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`)
- [ ] O mesmo UUID gerado aparece como `x-correlationId` no request Feign de saída

### Cenário 3 — Verificação de segurança do STS

Nos logs gerados durante o startup ou durante qualquer request que acione o `AuthTokenService`:
- [ ] Nenhuma ocorrência de `client_secret` nos logs
- [ ] Nenhum body de autenticação do STS logado pelo Logbook

### Cenário 4 — Verificação do padrão de log local

```
2026-06-27T... [http-nio-8080-exec-1] INFO  [550e8400-e29b-41d4-a716-446655440000] c.s.p.instituicoes.InstituicoesController - ...
```

- [ ] Formato inclui timestamp ISO, thread, nível, `[correlationId]`, logger abreviado e mensagem

### Build final

- [ ] `mvn test` passa sem regressões

## Notas de implementação

- Se o Logbook não logar nada para Feign, verificar que `loggerLevel: FULL` está em `application.yml` e que o Feign client tem `configuration = [LogbookFeignConfig::class]`.
- Se `correlationId` não aparecer no log local, verificar que `application-local.yml` tem o `logging.pattern.console` correto e que o perfil `local` está ativo (`--spring.profiles.active=local`).
- Logs do Logbook saem em nível `TRACE` ou `DEBUG` por padrão — garantir que o nível de log para `org.zalando.logbook` está habilitado (pode ser necessário adicionar `logging.level.org.zalando.logbook: TRACE` em `application-local.yml`).
