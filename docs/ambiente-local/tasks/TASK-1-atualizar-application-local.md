# TASK-1 — Atualizar application-local.yml para porta única

**Arquivo alvo:** `src/main/resources/application-local.yml` (existente)
**Referência SPEC:** Seção 8.3
**Depende de:** nenhuma
**Bloqueada por:** nenhuma

---

## Contexto

O `application-local.yml` atual aponta as integrações para portas diferentes (STS:9000, Inteligência:9002, Contatos Core:9003). A solução usa um único WireMock na porta 9001, então todas as integrações devem apontar para `localhost:9001`.

Instituições Financeiras já está em `localhost:9001/v1` — sem alteração.

## O que fazer

Adicionar/alterar as seguintes entradas em `application-local.yml`:

```yaml
sts:
  url: http://localhost:9001/v1/oauth

integration:
  inteligencia:
    url: http://localhost:9001/v1
  contatos-core:
    url: http://localhost:9001/v1
```

A chave `sts.url` sobrescreve o default definido em `application.yml` (`${integration.sts.url:http://localhost:9000/v1/oauth}`).

## Notas de implementação

- `StsClient` usa `url = "${sts.url}"` e `@PostMapping` sem path adicional — a URL completa vira `http://localhost:9001/v1/oauth`, que bate com o stub WireMock
- Não alterar `application.yml` nem outros profiles (`dev`, `hom`, `prod`)
- Não remover as propriedades existentes de `valkey` e `instituicoes-financeiras`

## Critério de aceite

- [ ] `application-local.yml` contém `sts.url: http://localhost:9001/v1/oauth`
- [ ] `integration.inteligencia.url` aponta para `http://localhost:9001/v1`
- [ ] `integration.contatos-core.url` aponta para `http://localhost:9001/v1`
- [ ] Nenhum outro arquivo de configuração foi alterado
