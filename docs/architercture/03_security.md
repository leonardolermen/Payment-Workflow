# PayFlow — Segurança

## Autenticação de usuários (JWT)

| Propriedade | Valor |
|---|---|
| Algoritmo | HS256 |
| Expiração | 24h (`86400000ms`) |
| Header | `Authorization: Bearer <token>` |
| Secret | Base64 — configurável via `JWT_SECRET` |

O token é gerado no login/registro e deve ser enviado em todas as requisições protegidas.

### Rotas públicas (sem JWT)

| Método | Path |
|---|---|
| `POST` | `/api/core/auth/register` |
| `POST` | `/api/core/auth/login` |

Todas as demais rotas exigem `Authorization: Bearer <token>` válido.

---

## Autenticação interna (service-to-service)

Chamadas entre `core-service` e `fraud-service` usam o header:

```
X-Internal-Token: <shared-secret>
```

Configurado via `INTERNAL_API_TOKEN` (padrão dev: `dev-internal-shared-secret-change-me`).

O `core-service` valida o header no `InternalApiKeyFilter` e atribui a role `ROLE_INTERNAL` ao contexto de segurança. O `api-gateway` injeta o header automaticamente via `InternalTokenRelayFilter` em todas as requisições downstream.

---

## Filtros de segurança — api-gateway

Executados em ordem de prioridade:

| Ordem | Filtro | Responsabilidade |
|---|---|---|
| -300 | `RequestTracingFilter` | Gera `X-Trace-Id` e propaga para request e response |
| -200 | `JwtAuthenticationFilter` | Valida JWT; extrai username e adiciona `X-User-Id` ao downstream |
| -100 | `InternalTokenRelayFilter` | Injeta `X-Internal-Token` em todas as requisições downstream |

Rotas públicas (`/api/core/auth/**`) pulam o `JwtAuthenticationFilter`.

---

## Filtros de segurança — core-service

Cadeia Spring Security (`OncePerRequestFilter`) na seguinte ordem:

1. `RequestLoggingFilter` — injeta `traceId` no MDC
2. `InternalApiKeyFilter` — valida `X-Internal-Token`
3. `JwtAuthenticationFilter` — valida JWT e popula `SecurityContext`

### Rotas liberadas no core-service

```
/auth/**   → permitAll
/error     → permitAll
demais     → authenticated
```

---

## Headers propagados pelo gateway

| Header | Origem | Destino |
|---|---|---|
| `X-Trace-Id` | Gateway (gerado) | Request + Response |
| `X-Internal-Token` | Gateway (injetado) | Downstream services |
| `X-User-Id` | Gateway (extraído do JWT) | Downstream services |
