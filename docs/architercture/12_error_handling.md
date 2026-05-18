# PayFlow — Tratamento de Erros

---

## Formato padrão de erro

Todos os erros retornam JSON no seguinte formato:

```json
{
  "status": 400,
  "message": "Descrição do problema",
  "errors": {
    "traceId": "uuid-de-rastreamento",
    "campo": "mensagem de validação"
  },
  "timestamp": "2026-05-18T12:00:00"
}
```

---

## Mapeamento de exceções — core-service

| Exceção | HTTP | Quando ocorre |
|---|---|---|
| `MethodArgumentNotValidException` | `400` | Campos obrigatórios inválidos (`@Valid`) |
| `HttpMessageNotReadableException` | `400` | JSON mal formatado |
| `InvalidFormatException` | `400` | Valor inválido para enum ou tipo |
| `EmailAlreadyExistsException` | `409` | E-mail já cadastrado |
| `DocumentAlreadyExistsException` | `409` | Documento já cadastrado |
| `UserNotFoundException` | `404` | Usuário não encontrado |
| `ResponseStatusException` | variável | Erro de negócio com status explícito |
| `RuntimeException` | `500` | Erro interno não tratado |
| `Exception` | `500` | Erro genérico inesperado |

> Erros 5xx retornam mensagem genérica ao cliente para não expor detalhes internos.

---

## Mapeamento de exceções — api-gateway

| Situação | HTTP | Mensagem |
|---|---|---|
| JWT ausente em rota protegida | `401` | Token de autenticação não fornecido |
| JWT inválido ou expirado | `401` | Token inválido ou expirado |
| Serviço downstream indisponível | `503` | Serviço temporariamente indisponível |
| Outros erros de gateway | variável | Mensagem da exceção |

---

## Rastreabilidade (traceId)

Cada requisição recebe um `traceId` UUID gerado pelo `RequestTracingFilter` (gateway) ou `RequestLoggingFilter` (core-service).

O `traceId` é:
- Propagado via header `X-Trace-Id` para todos os serviços downstream
- Injetado no MDC (`Mapped Diagnostic Context`) para aparecer nos logs
- Incluído em todas as respostas de erro no campo `errors.traceId`

**Exemplo de log com traceId:**
```
INFO [core-service] gateway.request traceId=abc123 method=POST path=/payments
WARN [core-service] http.error traceId=abc123 status=400 reason=Saldo insuficiente
```

---

## Erros de validação — exemplo

```json
{
  "status": 400,
  "message": "Dados inválidos. Verifique os campos e tente novamente.",
  "errors": {
    "traceId": "a1b2-c3d4-...",
    "name": "Nome é obrigatório",
    "password": "Senha deve ter no mínimo 6 caracteres"
  },
  "timestamp": "2026-05-18T12:00:00"
}
```

---

## Erros de enum inválido

Quando um valor inválido é passado para um campo enum, a resposta inclui os valores aceitos:

```json
{
  "status": 400,
  "message": "Dados inválidos. Verifique o payload e tente novamente.",
  "errors": {
    "documentType": "Valor inválido 'RG'. Valores aceitos: [CPF, CNPJ]"
  },
  "timestamp": "2026-05-18T12:00:00"
}
```
