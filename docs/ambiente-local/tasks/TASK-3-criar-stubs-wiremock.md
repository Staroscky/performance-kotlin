# TASK-3 — Criar stubs WireMock

**Arquivo alvo:** `meu-ambiente-local/mocks/mappings/` (novo)
**Referência SPEC:** Seções 5, 6, 8.4
**Depende de:** TASK-2
**Bloqueada por:** nenhuma

---

## Contexto

Criar os 4 stubs JSON que o WireMock carrega automaticamente ao iniciar. Cada stub define o path esperado, o delay de 80ms e a resposta sintética alinhada ao contrato do DTO de integração correspondente no código.

## O que fazer

### `meu-ambiente-local/mocks/mappings/oauth.json`

Stub para `StsClient` — `POST /v1/oauth` (form-urlencoded).

```json
{
  "request": {
    "method": "POST",
    "urlPattern": "/v1/oauth.*"
  },
  "response": {
    "status": 200,
    "headers": { "Content-Type": "application/json" },
    "fixedDelayMilliseconds": 80,
    "jsonBody": {
      "access_token": "mock-access-token",
      "expires_in": 3600
    }
  }
}
```

### `meu-ambiente-local/mocks/mappings/get-instituicoes-financeiras.json`

Stub para `InstituicoesFinanceirasClient` — `GET /v1/instituicoes-financeiras`.

```json
{
  "request": {
    "method": "GET",
    "urlPattern": "/v1/instituicoes-financeiras.*"
  },
  "response": {
    "status": 200,
    "headers": { "Content-Type": "application/json" },
    "fixedDelayMilliseconds": 80,
    "jsonBody": {
      "data": [
        { "ispb": "00000000", "nome_fantasia": "Banco do Brasil", "nome_reduzido": "BB" },
        { "ispb": "60701190", "nome_fantasia": "Itaú Unibanco", "nome_reduzido": "Itaú" },
        { "ispb": "33172537", "nome_fantasia": "Santander Brasil", "nome_reduzido": "Santander" }
      ]
    }
  }
}
```

### `meu-ambiente-local/mocks/mappings/get-inteligencia.json`

Stub para `InteligenciaClient` — `GET /v1/inteligencia?tela=...&cliente=...`.

```json
{
  "request": {
    "method": "GET",
    "urlPattern": "/v1/inteligencia.*"
  },
  "response": {
    "status": 200,
    "headers": { "Content-Type": "application/json" },
    "fixedDelayMilliseconds": 80,
    "jsonBody": {
      "data": [
        {
          "idSugestao": "a1b2c3d4-0000-0000-0000-000000000001",
          "chave": "fulano@email.com",
          "apelido": "Fulano"
        },
        {
          "idSugestao": "a1b2c3d4-0000-0000-0000-000000000002",
          "ag": "0500",
          "cc": "12345",
          "dac": "1",
          "tipoConta": "C"
        }
      ]
    }
  }
}
```

### `meu-ambiente-local/mocks/mappings/get-contatos-core.json`

Stub para `ContatosCoreClient` — `GET /v1/contatos-core?idPessoa=...&idConta=...`.

```json
{
  "request": {
    "method": "GET",
    "urlPattern": "/v1/contatos-core.*"
  },
  "response": {
    "status": 200,
    "headers": { "Content-Type": "application/json" },
    "fixedDelayMilliseconds": 80,
    "jsonBody": {
      "data": [
        {
          "idDestino": "c1d2e3f4-0000-0000-0000-000000000001",
          "idContato": "d2e3f4a5-0000-0000-0000-000000000002",
          "nome": "João Silva",
          "apelido": "Joãozinho",
          "dadosDestino": {
            "chave": "47997746981",
            "ag": null,
            "conta": null,
            "dac": null,
            "tipoConta": null
          }
        },
        {
          "idDestino": "e3f4a5b6-0000-0000-0000-000000000003",
          "idContato": "f4a5b6c7-0000-0000-0000-000000000004",
          "nome": "Maria Souza",
          "apelido": null,
          "dadosDestino": {
            "chave": null,
            "ag": "0500",
            "conta": "12345",
            "dac": "1",
            "tipoConta": "C"
          }
        }
      ],
      "x-next-cursor": null
    }
  }
}
```

## Notas de implementação

- `urlPattern` usa regex — o sufixo `.*` tolera query strings sem precisar de matchers específicos por parâmetro
- `x-next-cursor: null` é obrigatório no stub de contatos — sem ele, `ContatosService` entra em loop infinito
- Os campos `ispb` em Instituições Financeiras devem ter exatamente 8 dígitos (regra de domínio)
- O campo `access_token` do STS não precisa ser um JWT válido — os serviços downstream também são mockados e não validam o token

## Critério de aceite

- [ ] Os 4 arquivos JSON criados em `meu-ambiente-local/mocks/mappings/`
- [ ] `GET http://localhost:9001/v1/instituicoes-financeiras` retorna HTTP 200 com `data` preenchido
- [ ] `POST http://localhost:9001/v1/oauth` retorna HTTP 200 com `access_token` e `expires_in`
- [ ] `GET http://localhost:9001/v1/inteligencia?tela=x&cliente=y` retorna HTTP 200 com `data` preenchido
- [ ] `GET http://localhost:9001/v1/contatos-core?idPessoa=x&idConta=y` retorna HTTP 200 com `x-next-cursor: null`
- [ ] Todos os stubs respondem com latência ≥ 80ms (verificável nos logs do WireMock)
