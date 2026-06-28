# Princípios de teste

## Nomenclatura

Nome do teste descreve comportamento e expectativa. Padrão legível em português
ou backtick:

```kotlin
@Test
fun `deve recusar avaliacao quando saldo for insuficiente`() { ... }
```

## Um critério, um teste

Cada critério de aceitação da spec vira pelo menos um teste. Se um critério tem
caminhos (feliz, borda, erro), cada caminho é um teste. Isso torna a cobertura
rastreável de volta à spec.

## Estrutura AAA

```kotlin
@Test
fun `descricao`() {
    // Arrange — monte entradas e mocks
    // Act — execute a unidade
    // Assert — verifique comportamento observável
}
```

## O que mockar (e o que não)

- **Mocke** colaboradores externos: clients, repositórios, gateways.
- **Não mocke** value objects nem o próprio domínio puro — exercite-os de
  verdade.
- Evite mock de mais: se o teste precisa de muitos stubs, talvez a unidade esteja
  fazendo coisa demais (cheiro de violação de slice).

## Comportamento, não implementação

Asserte a saída e os efeitos observáveis, não a sequência interna de chamadas —
exceto quando a interação **é** o contrato (ex.: "publica evento X exatamente uma
vez"), aí `verify()` é legítimo.

## Determinismo

Sem dependência de relógio/UUID reais: injete `Clock`/geradores. Testes não
podem ser flaky.
