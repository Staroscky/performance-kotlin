# TASK-2 — Criar docker-compose.yml

**Arquivo alvo:** `meu-ambiente-local/docker-compose.yml` (novo)
**Referência SPEC:** Seção 8.2
**Depende de:** nenhuma
**Bloqueada por:** nenhuma

---

## Contexto

Criar o `docker-compose.yml` com dois serviços: Valkey (porta 6379) e WireMock (porta 9001). O WireMock monta a pasta `./mocks` em `/home/wiremock`, que é o diretório raiz padrão da imagem `wiremock/wiremock` — ela lê `mappings/` automaticamente a partir desse path.

## O que fazer

Criar `meu-ambiente-local/docker-compose.yml`:

```yaml
services:
  valkey:
    image: valkey/valkey:8
    ports:
      - "6379:6379"

  wiremock:
    image: wiremock/wiremock:3.10.0
    ports:
      - "9001:8080"
    volumes:
      - ./mocks:/home/wiremock
```

## Notas de implementação

- Não usar a chave `version:` — obsoleta no Compose v2 e gera warning
- Usar tags fixas (`valkey:8`, `wiremock:3.10.0`) para garantir reprodutibilidade
- O WireMock escuta internamente na porta 8080; o mapeamento `9001:8080` expõe na porta esperada pelo `application-local.yml`
- O volume `./mocks:/home/wiremock` faz o WireMock ler `./mocks/mappings/*.json` automaticamente na inicialização

## Critério de aceite

- [ ] `meu-ambiente-local/docker-compose.yml` criado com serviços `valkey` e `wiremock`
- [ ] `docker compose up` (dentro de `meu-ambiente-local/`) sobe os dois containers sem erro
- [ ] Valkey acessível em `localhost:6379`
- [ ] WireMock acessível em `http://localhost:9001/__admin/` (página admin do WireMock)
