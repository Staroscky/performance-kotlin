# SPEC — Ambiente Local com Docker Compose (WireMock + Valkey)

## 1. Contexto da solicitação

### 1.1 História ou tarefa do usuário

- Solicitante: desenvolvedor do projeto
- Tipo: infraestrutura / setup
- História: como desenvolvedor, quero executar `docker compose up` em uma pasta dedicada e ter todas as dependências locais prontas (Valkey + stubs das 4 integrações), sem precisar de ambientes externos
- Valor esperado: onboarding rápido e testes locais sem dependência de infra externa

### 1.2 Problema observado

Não existe infraestrutura de desenvolvimento local definida no repositório. As integrações STS, Instituições Financeiras, Inteligência e Contatos Core apontam para `localhost` no perfil `local` (`application-local.yml`), mas nada sobe esses serviços. O Valkey também precisa estar disponível na porta 6379.

### 1.3 Objetivo da entrega

Pasta `meu-ambiente-local/` commitada no repositório com `docker-compose.yml` e `mocks/` prontos para uso imediato.

## 2. Objetivo técnico

Criar infraestrutura Docker Compose reproduzível que espelha exatamente as URLs e portas já definidas em `src/main/resources/application-local.yml`, sem alterar nenhum arquivo existente da aplicação.

## 3. Estado atual

- `application-local.yml` define as seguintes URLs:
  - STS: `http://localhost:9000/v1/oauth` (default de `application.yml` via `${integration.sts.url:...}`)
  - Instituições Financeiras: `http://localhost:9001/v1`
  - Inteligência: `http://localhost:9002/v1`
  - Contatos Core: `http://localhost:9003/v1`
  - Valkey: `localhost:6379`
- Contratos de resposta definidos em:
  - `core/auth/StsTokenResponse.kt` → `access_token: String`, `expires_in: Long`
  - `instituicoes/integration/InstituicoesFinanceirasDto.kt` → `data: List<InstituicaoFinanceiraDto>`
  - `sugestoes/integration/InteligenciaDto.kt` → `data: List<InteligenciaItemDto>`
  - `contatos/integration/ContatosCoreDto.kt` → `data: List<ContatoCoreDto>`, `x-next-cursor: String?`
- `ContatosService` faz paginação cursor: percorre páginas enquanto `xNextCursor` não for nulo/vazio
- Não existe pasta `meu-ambiente-local/` no repositório
- As 4 integrações usam portas diferentes — será unificado em `localhost:9001` nesta entrega

## 4. Escopo da solução

### 4.1 O que muda

| Área | Estado atual | Estado esperado | Impacto |
|---|---|---|---|
| `src/main/resources/application-local.yml` | inteligencia:9002, contatos-core:9003, sts:9000 | todas as integrações → localhost:9001 | médio |
| `meu-ambiente-local/docker-compose.yml` | não existe | criado com Valkey + 1 WireMock | alto |
| `meu-ambiente-local/mocks/mappings/oauth.json` | não existe | stub do POST /v1/oauth | alto |
| `meu-ambiente-local/mocks/mappings/get-instituicoes-financeiras.json` | não existe | stub do GET /v1/instituicoes-financeiras | alto |
| `meu-ambiente-local/mocks/mappings/get-inteligencia.json` | não existe | stub do GET /v1/inteligencia | alto |
| `meu-ambiente-local/mocks/mappings/get-contatos-core.json` | não existe | stub do GET /v1/contatos-core | alto |

### 4.2 O que não muda

- `application.yml`, `application-dev.yml`, `application-hom.yml`, `application-prod.yml`
- Código-fonte da aplicação
- `docs/PROJECT-CONTEXT.md`, `docs/PRD.md` e `docs/core-setup/`

### 4.3 Restrições e pressupostos

- WireMock image: `wiremock/wiremock:3.10.0` (última estável no momento da escrita)
- Valkey image: `valkey/valkey:8`
- Cada instância WireMock escuta na porta interna 8080; o `docker-compose.yml` mapeia para a porta esperada pela aplicação
- A imagem WireMock usa `/home/wiremock` como diretório raiz; montar `./mocks/<serviço>` nesse path faz o WireMock ler `mappings/` automaticamente
- O delay de 80ms é configurado por stub via `fixedDelayMilliseconds` — não requer pós-configuração via API admin
- Os stubs cobrem apenas o happy path; cenários de erro ficam fora do escopo

## 5. Requisitos funcionais

| ID | Requisito funcional | Prioridade | Origem |
|---|---|---|---|
| RF-01 | `docker compose up` sobe todos os serviços sem erro | must | PRD |
| RF-02 | WireMock responde POST /v1/oauth com token sintético e 80ms | must | PRD |
| RF-03 | WireMock responde GET /v1/instituicoes-financeiras com lista sintética e 80ms | must | PRD |
| RF-04 | WireMock responde GET /v1/inteligencia com lista sintética e 80ms | must | PRD |
| RF-05 | WireMock responde GET /v1/contatos-core com lista sintética, `x-next-cursor: null` e 80ms | must | PRD |
| RF-06 | Valkey disponível na porta 6379 sem autenticação | must | PRD |
| RF-07 | Stubs respeitam os contratos dos DTOs de integração existentes no código | must | PRD |

## 6. Cenários e fluxos esperados

### 6.1 Cenários principais

- Desenvolvedor clona o repo, executa `docker compose up` em `meu-ambiente-local/`, inicia a app com `-Dspring.profiles.active=local` e chama `GET /v1/instituicoes` — retorna dados mockados
- App chama STS para autenticar antes de qualquer Feign call; WireMock na 9000 responde com token mock
- App chama Contatos Core com paginação; WireMock retorna `x-next-cursor: null`, encerrando o loop após a primeira página

### 6.2 Edge cases e falhas esperadas

- **Loop infinito em Contatos Core:** se `x-next-cursor` não for nulo, `ContatosService` faz chamadas infinitas. O stub deve retornar sempre `null` nesse campo.
- **WireMock não encontra mapping:** retorna 404 com corpo WireMock padrão — indica que o path do stub não bate com a chamada real; resolver alinhando o `urlPattern` com o path exato do Feign client.
- **Porta já em uso:** `docker compose up` falha com "address already in use" — desenvolvedor deve parar o processo que ocupa a porta.

## 7. Alternativas consideradas

### 7.1 Alternativa escolhida

1 container WireMock na porta 9001, com todos os stubs das 4 integrações no mesmo diretório `mocks/mappings/`. `application-local.yml` é atualizado para apontar todas as integrações para `localhost:9001`. Setup mais simples (2 containers: WireMock + Valkey) e `localhost:9001` já era a porta de Instituições Financeiras, servindo como âncora natural.

### 7.2 Alternativas descartadas

| Alternativa | Vantagens | Desvantagens | Motivo da não escolha |
|---|---|---|---|
| 4 WireMock separados (1 por integração) | Sem alteração no `application-local.yml` | 5 containers, mais memória e complexidade no compose | Mais pesado e sem ganho real para dev local |
| MockServer em vez de WireMock | API Java nativa | Menos adoção, imagem maior | WireMock é padrão de mercado e tem imagem oficial leve |
| Testcontainers no teste | Integração direta com JUnit | Escopo diferente — aqui é infra de dev, não de teste | Fora do escopo; não substitui ambiente local |

## 8. Design da solução

### 8.1 Visão geral

```
meu-ambiente-local/
├── docker-compose.yml
└── mocks/
    └── mappings/
        ├── oauth.json
        ├── get-instituicoes-financeiras.json
        ├── get-inteligencia.json
        └── get-contatos-core.json
```

### 8.2 docker-compose.yml

2 serviços: `valkey` (porta 6379) e `wiremock` (porta 9001). O WireMock monta `./mocks` em `/home/wiremock`.

### 8.3 application-local.yml — mudanças necessárias

```yaml
sts:
  url: http://localhost:9001/v1/oauth   # era: ${integration.sts.url:http://localhost:9000/v1/oauth}

integration:
  inteligencia:
    url: http://localhost:9001/v1       # era: http://localhost:9002/v1
  contatos-core:
    url: http://localhost:9001/v1       # era: http://localhost:9003/v1
  # instituicoes-financeiras já aponta para localhost:9001 — sem mudança
```

### 8.4 Stubs WireMock

Cada stub usa `urlPattern` com regex para tolerar query strings, `fixedDelayMilliseconds: 80` e `jsonBody` com dados sintéticos. O `Content-Type: application/json` é declarado explicitamente.

### 8.4 Contratos, dados e interfaces

**STS** — `POST /v1/oauth` (form-urlencoded):
```json
{
  "access_token": "mock-access-token",
  "expires_in": 3600
}
```

**Instituições Financeiras** — `GET /v1/instituicoes-financeiras`:
```json
{
  "data": [
    { "ispb": "00000000", "nome_fantasia": "Banco do Brasil", "nome_reduzido": "BB" },
    { "ispb": "60701190", "nome_fantasia": "Itaú Unibanco", "nome_reduzido": "Itaú" },
    { "ispb": "33172537", "nome_fantasia": "Santander Brasil", "nome_reduzido": "Santander" }
  ]
}
```

**Inteligência** — `GET /v1/inteligencia?tela=...&cliente=...`:
```json
{
  "data": [
    { "idSugestao": "a1b2c3d4-0000-0000-0000-000000000001", "chave": "fulano@email.com", "apelido": "Fulano" },
    { "idSugestao": "a1b2c3d4-0000-0000-0000-000000000002", "ag": "0500", "cc": "12345", "dac": "1", "tipoConta": "C" }
  ]
}
```

**Contatos Core** — `GET /v1/contatos-core?idPessoa=...&idConta=...`:
```json
{
  "data": [
    {
      "idDestino": "c1d2e3f4-0000-0000-0000-000000000001",
      "idContato": "d2e3f4a5-0000-0000-0000-000000000002",
      "nome": "João Silva",
      "apelido": "Joãozinho",
      "dadosDestino": { "chave": "47997746981", "ag": null, "conta": null, "dac": null, "tipoConta": null }
    },
    {
      "idDestino": "e3f4a5b6-0000-0000-0000-000000000003",
      "idContato": "f4a5b6c7-0000-0000-0000-000000000004",
      "nome": "Maria Souza",
      "apelido": null,
      "dadosDestino": { "chave": null, "ag": "0500", "conta": "12345", "dac": "1", "tipoConta": "C" }
    }
  ],
  "x-next-cursor": null
}
```

## 9. Fluxos técnicos

```text
Desenvolvedor
    │
    ▼
docker compose up (meu-ambiente-local/)
    ├── valkey:6379
    └── wiremock:9001
         ├── POST /v1/oauth
         ├── GET  /v1/instituicoes-financeiras
         ├── GET  /v1/inteligencia
         └── GET  /v1/contatos-core

App (profile=local)
    ├── GET /v1/instituicoes → Feign → localhost:9001/v1/instituicoes-financeiras → WireMock (80ms) → cache2k
    ├── GET /v1/sugestoes   → Feign → localhost:9001/v1/inteligencia             → WireMock (80ms) → Valkey (TTL 15m)
    ├── GET /v1/contatos    → Feign → localhost:9001/v1/contatos-core             → WireMock (80ms) → sem cache
    └── (todos os Feigns)   → STS   → localhost:9001/v1/oauth                    → WireMock (80ms) → token mock
```

## 10. Arquivos afetados

| Arquivo | Tipo | Mudança |
|---|---|---|
| `src/main/resources/application-local.yml` | Modificar | Unificar integrações em localhost:9001 |
| `meu-ambiente-local/docker-compose.yml` | Criar | Compose com Valkey + 1 WireMock |
| `meu-ambiente-local/mocks/mappings/oauth.json` | Criar | Stub POST /v1/oauth |
| `meu-ambiente-local/mocks/mappings/get-instituicoes-financeiras.json` | Criar | Stub GET /v1/instituicoes-financeiras |
| `meu-ambiente-local/mocks/mappings/get-inteligencia.json` | Criar | Stub GET /v1/inteligencia |
| `meu-ambiente-local/mocks/mappings/get-contatos-core.json` | Criar | Stub GET /v1/contatos-core |

## 11. Requisitos não funcionais

| Categoria | Requisito não funcional | Meta ou critério |
|---|---|---|
| Performance | Delay simulado de 80ms por integração | `fixedDelayMilliseconds: 80` em cada stub |
| Confiabilidade | Ambiente reproduzível entre máquinas | Imagens com tag fixa (não `latest`) |
| Compatibilidade | Funciona com Docker Compose v2 (`docker compose`) | Sintaxe sem `version:` obsoleto |

## 12. Estratégia de rollout ou migração

Nenhuma: pasta nova, nenhum arquivo existente é alterado. Pode ser adicionada ao repositório sem impacto.

## 13. Estratégia de validação

- Testes manuais: `docker compose up` em `meu-ambiente-local/`, iniciar app com profile `local`, chamar cada rota e verificar resposta
- Sinais operacionais: logs do WireMock mostram cada requisição recebida e o delay aplicado

## 14. Critérios de aceite

- [ ] `docker compose up` em `meu-ambiente-local/` conclui sem erros e todos os containers ficam healthy
- [ ] `GET http://localhost:9001/v1/instituicoes-financeiras` retorna a lista mockada com ≥ 80ms de latência
- [ ] `POST http://localhost:9001/v1/oauth` retorna `access_token` e `expires_in`
- [ ] `GET http://localhost:9001/v1/inteligencia?tela=x&cliente=y` retorna a lista de sugestões
- [ ] `GET http://localhost:9001/v1/contatos-core?idPessoa=x&idConta=y` retorna contatos com `x-next-cursor: null`
- [ ] App com profile `local` inicializa, autentica no STS mock e processa `GET /v1/instituicoes` end-to-end
- [ ] Casos de erro e fallback relevantes foram considerados (loop infinito evitado via `x-next-cursor: null`)

## 15. Riscos e observações

- **Loop infinito:** se `x-next-cursor` vier não-nulo nos contatos, `ContatosService` entra em loop. O stub deve sempre retornar `null`.
- **Versão do WireMock:** usar tag fixa evita quebras futuras; atualizar periodicamente.
- **Porta já em uso:** não há health check automático para isso; a mensagem de erro do Docker é clara o suficiente.

## 16. Questões em aberto

- Nenhuma.