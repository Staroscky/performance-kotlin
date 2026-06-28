---
name: vertical-slice
description: >-
  Padrão de Vertical Slice Architecture (VSA) do projeto: como organizar uma
  feature em camadas controller/domain/service/integration, com encapsulamento
  por interface entre módulos e integrações externas isoladas. USE SEMPRE que
  for criar uma nova feature, endpoint, slice ou módulo; quando precisar decidir
  onde colocar uma classe; quando estiver mexendo em estrutura de pacotes; ou
  quando avaliar se uma mudança está respeitando o isolamento entre slices —
  mesmo que a palavra "vertical slice" não seja dita explicitamente.
---

# Vertical Slice Architecture (VSA)

Cada feature é uma fatia vertical autocontida, do controller à integração, sem
depender de outras fatias. O acoplamento entre slices só acontece por contrato
explícito (interface), nunca por acesso direto a classes internas.

## Camadas de uma slice

- **controller** — borda HTTP. Recebe/valida request, delega ao service, monta a
  resposta. Sem regra de negócio.
- **domain** — modelos, sealed interfaces de resultado, regras puras. Sem
  dependência de framework nem de I/O.
- **service** — orquestra o caso de uso: chama domain, coordena integrations,
  aplica resiliência. É o "miolo" da slice.
- **integration** — adapters para o mundo externo (Feign clients, cache, fila).
  Tradução entre o contrato externo e o modelo de domínio.

Detalhe da estrutura de pastas e exemplos em `references/estrutura.md`.

## Regras de ouro

1. Uma slice **não** importa classes internas de outra slice. Se precisa de algo
   de outra, é por interface compartilhada e estável.
2. **Nada** atravessa o limite da slice sem contrato: schemas, DTOs e tipos
   compartilhados ficam num módulo comum explícito, não vazam por acaso.
3. `domain` é puro — não conhece Spring, Feign, nem nada de I/O.
4. Integrações externas ficam **isoladas** em `integration`, com tradução
   explícita para o domínio. O resto da slice nunca vê o DTO cru do upstream.

As regras arquiteturais completas (e como reforçá-las com ArchUnit) estão em
`references/regras-arquiteturais.md`. Leia esse arquivo quando for avaliar
conformidade de uma slice ou desenhar a estrutura de pacotes.
