# Detecção e correção de pinning

## Detecção

### Trace de pinning

```
-Djdk.tracePinnedThreads=full
```

Imprime stack trace quando uma VT fica pinned. `full` mostra o frame culpado.
Bom para desenvolvimento; ruidoso para produção contínua.

### Evento JFR

Prefira JFR em ambiente sob carga: o evento `jdk.VirtualThreadPinned` registra
ocorrências de pinning com duração e stack, sem o ruído do trace por stdout.

```
-XX:StartFlightRecording=duration=60s,filename=pinning.jfr
```

Depois inspecione `jdk.VirtualThreadPinned` no gravado.

## Correções

### synchronized → ReentrantLock

```kotlin
// Antes: pinning enquanto faz I/O dentro do bloco
@Synchronized
fun atualizar() { chamadaRemota() }

// Depois: ReentrantLock não prende a VT ao carrier
private val lock = ReentrantLock()
fun atualizar() = lock.withLock { chamadaRemota() }
```

### Limitar concorrência com Semaphore (não pool de VT)

```kotlin
private val limite = Semaphore(50)

fun chamar() {
    limite.acquire()
    try { chamadaRemota() } finally { limite.release() }
}
```

Virtual threads são baratas: crie uma por tarefa (`Executors.newVirtualThreadPerTaskExecutor()`)
e controle pressão com semáforo, **não** com pool de tamanho fixo de VT.

### computeIfAbsent

Evite I/O dentro de `computeIfAbsent` de `ConcurrentHashMap`. Calcule fora e
insira, ou use uma estratégia de cache que não sincronize no bin durante I/O.

### JDBC

Atualize o driver para versão que não use `synchronized` em torno de operações
bloqueantes. Verifique a release note do driver quanto a compatibilidade com
virtual threads.

## MDC em trabalho paralelo

```kotlin
val contexto = MDC.getCopyOfContextMap()
executor.submit {
    MDC.setContextMap(contexto ?: emptyMap())
    try { tarefa() } finally { MDC.clear() }
}
```

Sem isso, `X-Correlation-Id`/`X-Flow-Id` somem dos logs das threads filhas e o
tracing distribuído quebra.
