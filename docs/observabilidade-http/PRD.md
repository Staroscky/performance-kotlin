# PRD — Observabilidade HTTP

## Resumo

Adicionar propagação de `x-correlationId` nas requisições de entrada e saída, além de logging automático de requests e responses HTTP via Logbook Zalando para controllers e clientes Feign.

## Problema

A aplicação não possui rastreabilidade de requisições entre serviços. Quando ocorrem erros ou degradação de performance, não é possível correlacionar logs de um mesmo fluxo nem inspecionar o que foi enviado/recebido nas chamadas HTTP — tanto nas entradas (controllers) quanto nas saídas (Feign clients).

## Objetivo

Garantir que:
1. Cada requisição tenha um `x-correlationId` propagado no MDC e visível nos logs.
2. O `x-correlationId` recebido seja encaminhado automaticamente nos headers das chamadas Feign de saída.
3. Requests e responses HTTP (controllers e Feign) sejam logados automaticamente via Logbook Zalando.

## Escopo

### Inclui

- Filtro Servlet para extrair `x-correlationId` do header de entrada (ou gerar UUID v4 quando ausente) e popular o MDC.
- Interceptor Feign para propagar o `x-correlationId` como header nas chamadas de saída.
- Dependência e configuração do Logbook Zalando para logging de request/response nas controllers.
- Dependência e configuração do Logbook Zalando para logging de request/response nos clientes Feign.
- Configuração do logback para incluir `correlationId` nos logs estruturados.

### Não inclui

- Rastreamento distribuído com OpenTelemetry ou Micrometer Tracing.
- Rastreamento distribuído com OpenTelemetry ou Micrometer Tracing.
- Mascaramento de campos sensíveis nos logs do Logbook.
- Alteração de regras de negócio ou rotas existentes.

## Fluxo esperado

1. Cliente envia request com ou sem header `x-correlationId`.
2. Filtro extrai o UUID recebido ou gera um UUID v4, popula o MDC com a chave `correlationId`.
3. Todos os logs gerados durante a request incluem `correlationId` no padrão de log.
4. Logbook registra automaticamente o request e o response das controllers.
5. Ao acionar um Feign client, o interceptor injeta `x-correlationId` no header de saída.
6. Logbook registra automaticamente o request e o response das chamadas Feign.
7. Ao fim da request, o MDC é limpo para evitar vazamento entre threads.

## Critérios de sucesso

- `correlationId` presente em todos os logs de uma mesma requisição.
- Header `x-correlationId` presente nas chamadas Feign de saída (exceto STS/auth).
- Requests e responses das controllers aparecem nos logs via Logbook.
- Requests e responses dos Feign clients aparecem nos logs via Logbook.
- Nenhuma regressão nas rotas existentes após a mudança.

## Restrições ou observações

- Virtual threads habilitadas: Logback usa `InheritableThreadLocal`, MDC propaga automaticamente para VTs filhas. Um `MdcTaskDecorator` reutilizável será entregue para uso em futuros `ThreadPoolTaskExecutor`.
- STS client lida com credentials — avaliar se deve ser excluído do logbook ou ter body mascarado.
- Timeouts curtos (1500ms) — o logging não deve adicionar latência perceptível em operação normal.
