---
name: virtual-threads-pinning
description: >-
  Diagnóstico e correção de pinning de virtual threads no Java 21 (Spring Boot
  com virtual threads habilitadas). USE SEMPRE que houver código com
  synchronized, locks, cache, JDBC ou I/O bloqueante sob virtual threads; quando
  investigar degradação de throughput/latência sob carga; quando revisar
  concorrência; ou quando propagação de contexto (MDC) entre threads estiver em
  jogo — mesmo sem a palavra "pinning" no pedido.
---

# Pinning de virtual threads

Pinning = a virtual thread fica presa ao carrier thread (platform thread) e não
pode ser desmontada, matando o ganho de escala. Sob carga, vira gargalo.

## Causas (Java 21)

- **`synchronized` em volta de I/O ou de seção que bloqueia.** Enquanto dentro de
  bloco/método `synchronized`, a VT não desmonta.
- **Métodos nativos / chamadas a código nativo** que bloqueiam.

## Culpados escondidos

- `ConcurrentHashMap.computeIfAbsent` com função que faz I/O (sincroniza no bin).
- Caches caseiros com `synchronized`.
- Drivers JDBC antigos que ainda usam `synchronized` internamente.

## Detecção e correção

Comando de trace, eventos JFR e correções estão em
`references/deteccao-correcao.md`. Leia esse arquivo ao investigar um caso real
ou montar a instrumentação.

## Correções (resumo)

- Troque `synchronized` por `ReentrantLock` quando o lock envolve I/O.
- Use `Semaphore` para limitar concorrência em vez de pool de virtual threads —
  **não** se faz pooling de VT.
- Atualize drivers JDBC.

## MDC + virtual threads

MDC **não** propaga automaticamente para VTs filhas criadas dentro do request.
Garanta propagação explícita (wrapper de contexto/`ScopedValue` ou cópia do MDC)
ao disparar trabalho paralelo, ou os logs perdem `X-Correlation-Id`/`X-Flow-Id`.

## Nota — JDK 24 / JEP 491

No JDK 24, o JEP 491 resolve o pinning causado por `synchronized`: a VT passa a
desmontar mesmo dentro de bloco sincronizado. Em JDK 21 a mitigação manual acima
ainda é necessária.
