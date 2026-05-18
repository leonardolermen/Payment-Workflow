# PayFlow — API Gateway

Porta: **8080**  
Tecnologia: Spring Cloud Gateway (WebFlux)

---

## Rotas

| ID | Path do cliente | StripPrefix | Destino | Auth |
|---|---|---|---|---|
| `core-auth` | `/api/core/auth/**` | 2 | `core-service:8081` | Pública |
| `core-service` | `/api/core/**` | 2 | `core-service:8081` | JWT obrigatório |
| `fraud-service` | `/api/fraud/**` | 1 | `fraud-service:8082` | JWT obrigatório |

### Como o StripPrefix funciona

| Path do cliente | Strip | Chega no serviço como |
|---|---|---|
| `/api/core/auth/login` | 2 | `/auth/login` |
| `/api/core/payments` | 2 | `/payments` |
| `/api/core/users/{id}` | 2 | `/users/{id}` |
| `/api/core/api/manual-review/**` | 2 | `/api/manual-review/**` |
| `/api/fraud/analyze` | 1 | `/fraud/analyze` |

---

## Rate Limiting

Aplicado na rota `core-service` (protegida) via Redis:

| Propriedade | Valor |
|---|---|
| `replenishRate` | 20 req/s por IP |
| `burstCapacity` | 40 requisições |
| Key resolver | IP real (suporta `X-Forwarded-For`) |

---

## CORS

Configurado globalmente para todas as rotas:

```yaml
allowedOriginPatterns: "*"
allowedMethods: GET, POST, PUT, DELETE, OPTIONS
allowedHeaders: "*"
allowCredentials: true
```

---

## Filtros globais

| Filtro | Ordem | O que faz |
|---|---|---|
| `RequestTracingFilter` | -300 | Gera/propaga `X-Trace-Id` |
| `JwtAuthenticationFilter` | -200 | Valida Bearer JWT; retorna `401` se inválido |
| `InternalTokenRelayFilter` | -100 | Injeta `X-Internal-Token` em todo request downstream |

---

## Resposta de erro (401 — JWT ausente/inválido)

```json
{
  "status": 401,
  "message": "Token de autenticação não fornecido",
  "path": "/api/core/payments",
  "traceId": "a1b2c3d4-...",
  "timestamp": "2026-05-18T12:00:00"
}
```

---

## Variáveis de configuração

| Propriedade | Env var | Padrão |
|---|---|---|
| `jwt.secret` | `JWT_SECRET` | Secret Base64 embutido |
| `internal-api.token` | `INTERNAL_API_TOKEN` | `dev-internal-shared-secret-change-me` |
| `spring.data.redis.host` | — | `localhost` |
| `spring.data.redis.port` | — | `6379` |
