# PRD — Ambiente Local com Docker Compose

## Resumo

Criar a pasta `meu-ambiente-local` com um `docker-compose.yml` que sobe WireMock e Valkey, e uma pasta `mocks/` com os stubs das quatro integrações externas, todos com delay de 80ms.

## Problema

Para executar a aplicação localmente, as quatro integrações externas (STS, Instituições Financeiras, Inteligência, Contatos Core) e o Valkey precisam estar disponíveis. Sem eles, a aplicação não inicializa ou falha em runtime. Atualmente não existe infraestrutura de desenvolvimento local definida no repositório.

## Objetivo

Permitir que qualquer desenvolvedor suba o ambiente local com um único `docker compose up` dentro da pasta `meu-ambiente-local`, sem dependência de ambientes externos.

## Escopo

### Inclui

- `docker-compose.yml` com 4 instâncias WireMock (uma por integração) e 1 Valkey
- Pasta `mocks/` com stubs JSON das 4 integrações: STS, Instituições Financeiras, Inteligência e Contatos Core
- Delay de 80ms em todas as respostas mockadas
- Respostas com dados sintéticos alinhados ao contrato real de cada integração

### Não inclui

- Alteração de qualquer arquivo de configuração da aplicação (`application*.yml`)
- Mocks de cenários de erro ou fallback (apenas happy path)
- Configuração de TLS no WireMock ou Valkey
- Autenticação no WireMock admin

## Fluxo esperado

1. Desenvolvedor executa `docker compose up` dentro de `meu-ambiente-local/`
2. Valkey sobe na porta 6379
3. Quatro instâncias WireMock sobem nas portas 9000 (STS), 9001 (Instituições), 9002 (Inteligência) e 9003 (Contatos Core)
4. Aplicação é iniciada com profile `local`, conecta ao Valkey e chama as integrações via WireMock
5. Chamadas às rotas da API retornam dados sintéticos com latência de 80ms nas integrações

## Critérios de sucesso

- `docker compose up` conclui sem erros
- WireMock responde nas portas 9000, 9001, 9002 e 9003 com os mocks configurados e 80ms de delay
- Valkey disponível e acessível na porta 6379
- Aplicação com profile `local` inicializa e processa requisições end-to-end usando os mocks

## Restrições ou observações

- Os mocks devem respeitar os contratos exatos dos DTOs de integração definidos no código (`StsTokenResponse`, `InstituicoesFinanceirasResponse`, `InteligenciaResponse`, `ContatosCoreResponse`)
- Contatos Core usa paginação cursor: o mock deve retornar `x-next-cursor` nulo para evitar loop infinito