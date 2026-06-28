# Idiomas de Kotlin

Exemplos genéricos. Substitua `Pedido`/`Item`/etc. pelo domínio real.

## Nullability

```kotlin
// Evite
val total = pedido!!.total!!

// Prefira
val total = pedido?.total ?: BigDecimal.ZERO

// Na borda, falhe explícito
fun processar(id: String?) {
    val idValido = requireNotNull(id) { "id é obrigatório" }
    // ...
}
```

## Imutabilidade

```kotlin
data class Pedido(val id: String, val itens: List<Item>)

// Atualização sem mutação
val comDesconto = pedido.copy(itens = pedido.itens.map { it.aplicarDesconto() })
```

Exponha `List`, não `MutableList`, na API pública. Use coleção mutável apenas
como detalhe interno de implementação.

## Scope functions

```kotlin
// let: rodar bloco só se não-nulo
usuario?.email?.let { enviar(it) }

// apply: configurar e devolver o próprio objeto
val client = RestClient().apply {
    baseUrl = url
    timeout = Duration.ofSeconds(3)
}

// run/with: executar num escopo e devolver resultado
val resumo = with(relatorio) { "$titulo — $total itens" }

// also: efeito colateral sem quebrar a cadeia
return calcular().also { log.debug("resultado={}", it) }
```

## Funções de coleção

Prefira `map`/`filter`/`fold`/`associateBy` a loops imperativos quando o intento
fica mais claro. Cuidado com `computeIfAbsent` de `ConcurrentHashMap` sob virtual
threads — ver a skill `virtual-threads-pinning`.

## Expressões em vez de statements

`when`/`if` como expressão que retorna valor deixa o código mais denso e força
exaustividade quando combinado com `sealed`.

```kotlin
val faixa = when {
    valor < 100 -> Faixa.BAIXA
    valor < 1000 -> Faixa.MEDIA
    else -> Faixa.ALTA
}
```
