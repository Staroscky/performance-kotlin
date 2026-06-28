# TASK-4 — Core: CaronteMapping + GET /entrypoint

**Arquivos alvo:** pacote `core.caronte` e `entrypoint` (novos)
**Referência SPEC:** Seções 5 (RF-02, RF-03, RF-06), 8.4 (CaronteMapping, CaronteRegistry, EntrypointController)
**Depende de:** TASK-1
**Bloqueada por:** nenhuma

---

## Contexto

O GET /entrypoint deve retornar a lista de rotas disponíveis sem manutenção manual. A anotação `@CaronteMapping(rel)` nas controllers, combinada com o `CaronteRegistry` que escaneia o contexto no startup, automatiza esse processo.

## O que fazer

### 1. `core/caronte/CaronteMapping.kt`

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CaronteMapping(val rel: String)
```

### 2. `core/caronte/CaronteItem.kt`

```kotlin
data class CaronteItem(val rel: String, val href: String)
```

### 3. `core/caronte/CaronteRegistry.kt`

```kotlin
@Component
class CaronteRegistry(ctx: ApplicationContext) {

    val items: List<CaronteItem> = ctx
        .getBeansWithAnnotation(CaronteMapping::class.java)
        .values
        .mapNotNull { bean ->
            val clazz = AopUtils.getTargetClass(bean)
            val rel = clazz.getAnnotation(CaronteMapping::class.java)?.rel
                ?: return@mapNotNull null
            val href = clazz.getAnnotation(RequestMapping::class.java)?.value?.firstOrNull()
                ?: return@mapNotNull null
            CaronteItem(rel = rel, href = href)
        }
        .sortedBy { it.rel }
        .also { log.info("CaronteRegistry: {} itens registrados → {}", it.size, it.map { i -> i.rel }) }

    companion object {
        private val log = LoggerFactory.getLogger(CaronteRegistry::class.java)
    }
}
```

### 4. `entrypoint/EntrypointController.kt`

```kotlin
@RestController
@RequestMapping
class EntrypointController(private val registry: CaronteRegistry) {

    @GetMapping("/entrypoint")
    fun entrypoint(): Map<String, List<CaronteItem>> =
        mapOf("data" to registry.items)
}
```

> `EntrypointController` **não** recebe `@CaronteMapping` — ela não deve registrar a si mesma no entrypoint.

## Notas de implementação

- `AopUtils.getTargetClass` desembrulha proxies CGLIB/JDK gerados pelo Spring antes de ler as anotações — necessário pois `@RestController` gera proxy
- O `CaronteRegistry` é um singleton inicializado no startup; a lista é imutável após criação
- `@RequestMapping` lido da classe (não do método) — controllers devem ter o prefixo de path na anotação de classe, não apenas nos métodos

## Critério de aceite

- [ ] GET /entrypoint retorna `{"data": [{rel, href}, ...]}` com os itens das controllers anotadas com `@CaronteMapping`
- [ ] Nenhum item é hard-coded; adicionar `@CaronteMapping` em uma nova controller reflete automaticamente
- [ ] Teste: `deve retornar lista de itens registrados no CaronteRegistry`
- [ ] Teste: `deve retornar lista vazia quando nenhuma controller tem @CaronteMapping`
- [ ] Log de startup do `CaronteRegistry` lista os itens registrados
- [ ] Build e testes passam sem erros
