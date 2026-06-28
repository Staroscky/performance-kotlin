# Modelagem de domínio

## Resultado como sealed interface, não exceção

Fluxo de negócio esperado é modelado no tipo. Exceção fica para falha
inesperada/infra.

```kotlin
sealed interface ResultadoAvaliacao {
    data class Permitida(val referencia: String) : ResultadoAvaliacao
    data class NaoPermitida(val motivo: Motivo) : ResultadoAvaliacao
    data class Erro(val causa: Throwable) : ResultadoAvaliacao
}
```

`NaoPermitida` é um **resultado**, não um throw. O controller faz `when`
exaustivo e mapeia cada caso para a resposta (HTTP/SDUI) certa:

```kotlin
return when (val r = service.avaliar(comando)) {
    is Permitida    -> ok(r.referencia)
    is NaoPermitida -> modal(r.motivo)      // ex.: apresentação via YAML/SDUI
    is Erro         -> erro(r.causa)
}
```

Benefícios: o compilador cobra todos os casos; a apresentação fica separada da
regra; não há custo de stacktrace para um "não" esperado.

## Value classes para tirar significado de tipos primitivos

```kotlin
@JvmInline
value class Cpf(val valor: String) {
    init { require(valor.length == 11) { "CPF inválido" } }
}

@JvmInline
value class Valor(val centavos: Long)
```

Evita trocar parâmetros por engano (`fun pagar(cpf: Cpf, valor: Valor)`) e
centraliza validação na construção, sem custo de boxing na maioria dos casos.

## Enums e sealed para variantes

- **enum** quando o conjunto é fixo e sem dados próprios (ex.: `PIX`, `TED`,
  `TEF`).
- **sealed** quando cada variante carrega dados diferentes (ex.: motivos de
  recusa com payloads distintos).

## Cuidado com polimorfismo de desserialização

Ao desserializar hierarquias (Jackson `@JsonTypeInfo`), evite inferência
ambígua. `Id.DEDUCTION` pode resolver o subtipo errado quando duas variantes têm
shape parecido — prefira discriminador explícito (`Id.NAME` + `@JsonSubTypes`
com `property` dedicada) para não cair em ambiguidade silenciosa.
