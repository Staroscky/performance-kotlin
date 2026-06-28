# Regras arquiteturais

Use esta lista como checklist ao revisar ou desenhar uma slice.

## Isolamento entre slices

- [ ] Nenhuma classe de uma slice importa classe **interna** de outra slice.
- [ ] Dependência entre slices só acontece via interface compartilhada e estável.
- [ ] Tipos/DTOs/schemas compartilhados vivem num módulo comum explícito, nunca
      são "pegos emprestado" de dentro de outra slice.

## Pureza do domínio

- [ ] `domain` não importa nada de `org.springframework`, `feign`, nem clientes
      HTTP/DB.
- [ ] Regras de negócio são funções/objetos puros e testáveis sem subir contexto.
- [ ] Fluxo de negócio esperado é `sealed interface`, não exceção.

## Isolamento de integrações

- [ ] DTO cru do upstream não escapa de `integration`.
- [ ] Há um mapper explícito traduzindo upstream <-> domínio.
- [ ] Resiliência (circuit breaker, retry, fallback de cache) fica na borda de
      integração/serviço, não espalhada pelo domínio.

## Reforço com ArchUnit

Trave as regras em teste para não depender de disciplina manual:

```kotlin
@AnalyzeClasses(packages = ["com.exemplo.app"])
class ArquiteturaTest {

    @ArchTest
    val dominioNaoDependeDeFramework =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "feign..")

    @ArchTest
    val slicesNaoSeImportam =
        slices()
            .matching("com.exemplo.app.(*)..")
            .should().notDependOnEachOther()
            .ignoreDependency(
                resideInAPackage("..shared.."),
                alwaysTrue()
            )
}
```

Ajuste `..shared..` para o módulo comum real do projeto. A ideia: slices não se
importam, exceto através do contrato compartilhado.
