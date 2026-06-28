# Objetivo
Criar algumas rotas para um teste de performance.

# Descrição técnica
Criar 4 rotas e configs core, seguindo a skill de boas praticas do kotlin e vertical slice.

Os feigns devem ser configurados em application.yml, as rotas devem ser criadas por ambiente (dev,hom,prod e local), exemplo:

application.yml:
```yml
sts:
  url: ${integration.sts.url:http://localhost:9000/v1/oauth}
  client:
    id: ${integration.sts.client-id}
    secret: ${integration.sts.client-secret}
  app-id: d230d5c5-a270-4a9c-a93e-ac2da7ff176f

spring.cloud:
  feign:
    client:
      config:
        default:
          connectTimeout: 1500
          readTimeout: 1500
          auth-client: sts
        instituicoes-financeiras:
          url: ${integration.instituicoes-financeiras.url}
          default-headers:
            x-api-key: ${integration.instituicoes-financeiras.api-key}
            x-app-id: ${sts.app-id}
```

application-local.yml:
```yml
sts.client:
  id: b7d1ab21-5f8c-42a0-b07c-5150fd59f1cb
  secret: 9773ac44-072b-41a7-b6a8-ccb051047ac0

integration:
  instituicoes-financeiras:
    url: http://localhost:9001/v1
    api-key: 1234567890
```
## Config Core
### Usuario
Recebemos requisições com o header Authorization contendo um bearer token (JWT). Existem duas informações que usamos desse token, recuperadas do claim. idPessoa (UUID) e idConta (UUID).

Preciso que crie um bean com request escope para recuperar esses dados do token, nome de UsuarioContext, deve ter um objeto com esses valores idPessoa e idConta.

### Caronte Mapping
Quero simplificar o mapeamento de rotas para uma funcionalidade. Minha aplicação deve ter uma rota /entrypoint que retorna uma lista de items rel e href. Não quero que fique manual esse processo de incluir items.

Crie uma anotação que eu possa informar na controller o rel e já pegue a informação da rota.
Essas informações da anotação devem ser utilizadas no GET /entrypoint. Carregadas no startup da aplicação.

### Auth Client
Deve existir um bean de auth client, que recebe o client id e secret do application.yml, e faz a autenticação no sts para recuperar o token de acesso. Esse token deve ser utilizado para todas as integrações feign.
Deve ficar cacheado com cache2k pelo tempo de expiração do token retornado pelo sts, renovando o token quando faltar 30s para expirar.

## Rota 1:
ROTA: GET /entrypoint
Response:
```json
{
    "data": [
        {
            "rel": "instituicoes",
            "href": "/v1/instituicoes"
        },
        {
            "rel": "contatos",
            "href": "/v1/contatos"
        },
        {
            "rel": "sugestoes",
            "href": "/v1/sugestoes"
        }
    ]
}
```

## Rota 2
ROTA: GET /v1/instituicoes
Deve realizar uma integração FEIGN transformar no dado que iremos retornar e salvar num cache2k por 24h, a chave do cache é para todos os usuarios.

Deve existir um arquivo /resources/config/instituicoes.yml, para configurar por ISPB nome personalizado e icones. Ao não ter icone é enviado null, ao não ter nome personalizado deve utilizar nome_reduzido

Resposta:
```json
{
    "data": [
        {
            "iconeUrl": "ids_xxx",
            "ispb": "12345678",
            "nome": "XPTO",
            "searchable": "Banco XPTO#XPTO#12345678"
        }
    ]
}
```

### Integração
rota: GET /v1/instituicoes-financeiras
response:
```json
{
    "data": [
        {
            "ispb": "12345678", // Sempre 8 digitos
            "nome_fantasia": "Banco XPTO",
            "nome_reduzido": "XPTO"
        },
        ...
    ]
}
```


## Rota 3
Deve realizar uma consulta para a integração e salvar dados num valkey, ttl de 15m, o dado chave do cache deve ser o idPessoa do claim do JWT de requisição.

O valkey deve ser configurado com lettuce.
Os dados precisam ser tratados e terem dominios sealed, para chave pix e agencia e conta.
A integração pode ocorrer problemas em que alguns campos obrigatórios são retornados null. Então o dominio da integração todos devem ser opcionais e a regra de obrigatóriedade no dominio da fatia.

### integração
ROTA: GET /v1/inteligencia
Query Parameter:
- tela: sugestao-transferencia
- cliente: idPessoa-idConta

Retorno:
```json
{
    "data": [
        {
            "idSugestao": "UUID",
            "chave": "email@gmail.com", // quando via chave pix
            "apelido": "Fulano", // Opcional quando existe apelido
            // Presentes quando via agencia e conta
            "ag": "0500",
            "cc": "12345",
            "dac": "1",
            "tipoConta": "C" // C = Corrente, P = Poupanca e G = Pagamento
        }
    ]
}
```

## Rota 4
Consulta de contatos, é retornado o dado da integração com paginação cursor.
Devemos percorrer até finalizar toas a paginas e retornar tudo para o cliente.

A integração pode ocorrer problemas em que alguns campos obrigatórios são retornados null. Então o dominio da integração todos devem ser opcionais e a regra de obrigatóriedade no dominio da fatia.

Deve ter dominio agconta e chave pix.
Não existe cache nessa rota, apenas retornar conforme integração
Dados da query devem ser extraidos do token.

### Integração
ROTA: GET /v1/contatos-core (GraphQL)
QueryParameters = idPessoa, idConta
```json
{
    "data": [
        {
            "idDestino": "UUID",
            "idContato": "UUID",
            "nome": "Joao",
            "apelido": "Joaozinho",
            "dadosDestino":{
                "chave": "47997746981",
                "ag": "0500",
                "conta": "1234",
                "dac": "",
                "tipoConta": "C" // C = Corrente, P = Poupanca e G = Pagamento 
            }
        }
    ]
}
```