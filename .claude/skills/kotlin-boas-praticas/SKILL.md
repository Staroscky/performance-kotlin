---
name: kotlin-boas-praticas
description: >-
  Boas práticas idiomáticas de Kotlin do projeto: nullability, imutabilidade,
  sealed/value classes, scope functions e modelagem de domínio. USE SEMPRE que
  for escrever ou revisar código Kotlin; quando estiver modelando um tipo de
  domínio ou um resultado de caso de uso; quando precisar decidir entre exceção
  e tipo de resultado; ou quando avaliar se um trecho está idiomático — mesmo
  sem pedido explícito de "boas práticas".
---

# Boas práticas Kotlin

Código previsível, imutável por padrão e com o domínio modelado no tipo. Os
exemplos aqui são genéricos de propósito — aplique o padrão ao domínio real.

## Núcleo

- **Imutabilidade por padrão.** `val` sobre `var`; `List`/`Map` imutáveis;
  `data class` com `copy` em vez de mutação.
- **Nullability explícita.** Nada de `!!`. Trate ausência com `?.`, `?:`,
  `requireNotNull`/`checkNotNull` na borda. Null vira tipo, não surpresa.
- **Modele no tipo.** `sealed interface` para resultados, `enum`/`value class`
  para conceitos que hoje são `String`/`Int` soltos.
- **Scope functions com intenção.** `let` para null-safe, `apply` para
  configurar, `run`/`with` para escopo, `also` para efeito colateral. Não
  empilhe por estética.

Idiomas detalhados e exemplos em `references/idiomas.md`.
Modelagem de domínio (sealed, value classes, resultados) em
`references/modelagem.md`. Leia esse arquivo ao desenhar um novo tipo de domínio
ou o resultado de um caso de uso.

## Anti-padrões a barrar

- `!!` em código de produção.
- Coleção mutável exposta na API pública de uma classe.
- `String` carregando significado de domínio que deveria ser `value class`.
- Exceção usada para fluxo de negócio esperado (use `sealed` de resultado).
- Cadeia de scope functions ilegível só para evitar uma variável nomeada.
