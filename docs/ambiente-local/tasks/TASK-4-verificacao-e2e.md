# TASK-4 — Verificação e2e do ambiente local

**Arquivo alvo:** nenhum (verificação manual)
**Referência SPEC:** Seção 14
**Depende de:** TASK-1, TASK-2, TASK-3
**Bloqueada por:** TASK-1, TASK-2, TASK-3

---

## Contexto

Verificar que o ambiente local completo funciona end-to-end: `docker compose up` sobe os serviços, a aplicação inicializa com profile `local` e todas as rotas retornam dados via WireMock com delay de 80ms.

## O que fazer

1. Dentro de `meu-ambiente-local/`, executar:
   ```bash
   docker compose up
   ```

2. Verificar que os dois containers estão de pé:
   - Valkey: `redis-cli -p 6379 ping` → deve retornar `PONG`
   - WireMock admin: `GET http://localhost:9001/__admin/mappings` → deve listar os 4 stubs

3. Iniciar a aplicação com profile `local`:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

4. Chamar cada rota com um JWT sintético no header (Bearer com claims `idPessoa` e `idConta` como UUIDs quaisquer):

   ```
   GET http://localhost:8080/entrypoint
   GET http://localhost:8080/v1/instituicoes
   GET http://localhost:8080/v1/sugestoes
   GET http://localhost:8080/v1/contatos
   ```

5. Verificar nos logs do WireMock que cada requisição foi atendida com 80ms de delay.

## Notas de implementação

- O JWT de entrada (header `Authorization`) deve ter os claims `idPessoa` (UUID) e `idConta` (UUID) para que `UsuarioContext` não lance exceção. Pode ser gerado em jwt.io com qualquer chave e algoritmo HS256.
- A rota `/v1/sugestoes` monta a query `cliente=<idPessoa>-<idConta>` — o stub usa `urlPattern` com `.*`, então qualquer valor é aceito.
- Se o WireMock retornar 404, verificar se o `urlPattern` do stub bate com o path exato chamado pelo Feign (conferir nos logs do WireMock qual URL chegou).

## Critério de aceite

- [ ] `docker compose up` conclui sem erros e ambos containers ficam healthy
- [ ] `GET /entrypoint` retorna a lista de rotas cadastradas via `@CaronteMapping`
- [ ] `GET /v1/instituicoes` retorna instituições transformadas (com `ispb`, `nome`, `searchable`)
- [ ] `GET /v1/sugestoes` retorna sugestões com domínio `ChavePix` ou `AgenciaConta`
- [ ] `GET /v1/contatos` retorna contatos sem entrar em loop (cursor encerra na primeira página)
- [ ] Logs do WireMock confirmam delay de 80ms em todas as chamadas
- [ ] Build e testes relevantes passam sem regressão (`./mvnw test`)
