# Estrutura de uma slice

Exemplo genérico de uma feature `<feature>` dentro do pacote raiz da aplicação.

```
com.exemplo.app
└── <feature>
    ├── <Feature>Controller.kt        # borda HTTP
    ├── domain
    │   ├── <Feature>.kt               # modelo
    │   ├── Resultado<Feature>.kt      # sealed interface de resultado
    │   └── regras/                    # regras puras, sem framework
    ├── service
    │   └── <Feature>Service.kt        # orquestra o caso de uso
    └── integration
        ├── <Upstream>Client.kt        # Feign client (contrato externo)
        ├── <Upstream>Dto.kt           # DTO cru do upstream
        └── <Upstream>Mapper.kt        # tradução DTO -> domínio
```

## Princípios da estrutura

- **Coesão por feature, não por camada técnica.** Tudo que muda junto fica
  junto. Você não tem um pacote global `controllers/` com todos os controllers;
  cada slice carrega o seu.
- **Encapsulamento por interface.** O que outra slice pode consumir é exposto
  por uma interface no limite da slice. O `internal` do Kotlin ajuda a impedir
  vazamento de implementação dentro do módulo.
- **Tradução na borda.** O DTO do upstream nasce e morre dentro de
  `integration`. O `service` e o `domain` só falam a linguagem do domínio.

## Resultado como tipo, não exceção

A saída do caso de uso é modelada como `sealed interface` (ex.: `Permitida`,
`NaoPermitida`, `Erro`), não como exceção para fluxo de negócio esperado. Isso
deixa o controller fazer `when` exaustivo e mapear cada caso para a resposta
HTTP/SDUI correta. Ver a skill `kotlin-boas-praticas` para modelagem de sealed.
