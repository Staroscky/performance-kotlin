---
name: testes-unitarios
description: >-
  Padrão de testes unitários do projeto: JUnit 5 + Mockito com o wrapper
  mockito-kotlin (NÃO MockK). USE SEMPRE que for escrever, revisar ou completar
  testes; quando estiver implementando um critério de aceitação (um teste por
  critério, RED→GREEN); quando precisar mockar uma dependência em Kotlin; ou
  quando avaliar a qualidade/cobertura de um conjunto de testes — mesmo sem a
  palavra "teste" no pedido, se houver código novo sendo entregue.
---

# Testes unitários

JUnit 5 + Mockito via **mockito-kotlin**. Exemplos genéricos — aplique ao
domínio real.

## Regras

- **Stack fixa: `mockito-kotlin`, não MockK.** Use `mock()`, `whenever()`,
  `verify()`, `argumentCaptor()` do `org.mockito.kotlin`.
- **Um teste por critério de aceitação.** Cada critério da spec tem ao menos um
  teste correspondente, nomeado de forma legível.
- **TDD: RED → GREEN.** Escreva o teste do critério primeiro (falhando), depois
  implemente até passar.
- **Isole a unidade.** Mocke colaboradores; teste a lógica, não o framework.
- **Arrange / Act / Assert** explícitos; um comportamento por teste.

Princípios e nomenclatura em `references/principios.md`.
Padrões de Kotlin + Spring (mockito-kotlin, sealed, integração) em
`references/kotlin-spring.md`. Leia esse arquivo ao montar o setup de mocks ou
testar resultado sealed.

## Anti-padrões a barrar

- MockK (fora do padrão do projeto).
- Teste que sobe contexto Spring inteiro para validar lógica pura.
- Asserção em cima de detalhe de implementação em vez de comportamento.
- Critério de aceitação sem teste correspondente.
