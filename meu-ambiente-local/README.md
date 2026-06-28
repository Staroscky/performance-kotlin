# Ambiente Local

Docker Compose com WireMock (stubs das integrações) e Valkey para rodar a aplicação localmente.

---

## Subir o ambiente

```bash
docker compose up
```

| Serviço  | Porta  | Descrição                        |
|----------|--------|----------------------------------|
| WireMock | `9001` | Stubs das 4 integrações externas |
| Valkey   | `6379` | Cache distribuído                |

---

## Rodar a aplicação

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

---

## JWT de teste

A aplicação extrai `idPessoa` e `idConta` do Bearer token. Use o payload abaixo em [jwt.io](https://jwt.io) com algoritmo `HS256` e qualquer secret.

**Payload:**
```json
{
  "idPessoa": "00000000-0000-0000-0000-000000000001",
  "idConta": "00000000-0000-0000-0000-000000000002"
}
```

**Token gerado (secret: `local`):**
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlkUGVzc29hIjoiZjNkYmVhZjAtMjdhOS00ZjcxLWEwYmEtMThiOTQ0YzZiZDJmIiwiaWRDb250YSI6IjZkMjMwYmZmLTA4MTUtNGM3Mi1hNGU5LTRmOTc1MzBjMWUxMCIsImlhdCI6MTc4MjY5MDQ2N30.xmt0KL9G9yYcVbFW9cj85NdfYtRV66zNenJxFUEgduI
```

> Gere um token válido em jwt.io caso a aplicação valide a assinatura. Por padrão o projeto apenas decodifica o JWT sem verificar assinatura.

---

## Chamadas de exemplo

Substitua `<TOKEN>` pelo JWT gerado acima.

### Entrypoint
```bash
curl http://localhost:8080/entrypoint \
  -H "Authorization: Bearer <TOKEN>"
```

### Instituições
```bash
curl http://localhost:8080/v1/instituicoes \
  -H "Authorization: Bearer <TOKEN>"
```

### Sugestões
```bash
curl http://localhost:8080/v1/sugestoes \
  -H "Authorization: Bearer <TOKEN>"
```

### Contatos
```bash
curl http://localhost:8080/v1/contatos \
  -H "Authorization: Bearer <TOKEN>"
```

---

## Verificar stubs carregados

```bash
curl http://localhost:9001/__admin/mappings
```

---

## Parar o ambiente

```bash
docker compose down
```
