# [ ] Melhorias Pendentes

Melhorias que fazem sentido implementar (prioridade média).

---

## 3.1 Validação de DTOs

### Status: ⚠️ PARCIALMENTE IMPLEMENTADO

- [x] Adicionar Bean Validation (`@Valid`) nos DTOs de entrada
- [x] `RegisterRequestDTO`: `@Email`, `@NotBlank`, `@Size(min=8)` em password (implementado no AuthController)
- [ ] `PaymentRequest`: `@NotNull` em campos obrigatórios, `@Positive` em amount (pendente no PaymentController)
- [ ] Adicionar validações em outros DTOs de entrada

---

## 3.2 Idempotência com Redis

### Status: ✅ COMPLETAMENTE IMPLEMENTADO

Container Redis já está configurado no `docker-compose.yml`

- [x] Adicionar dependência `spring-boot-starter-data-redis`
- [x] Implementar cache com TTL de 24h para idempotency keys
- [x] Benefício: evita hit no banco para chaves recentes
- [x] `RedisConfig` criado com configurações adequadas
- [x] `PaymentService` utiliza RedisTemplate para idempotência

---

## 3.3 Refresh Token

### Status: ❌ NÃO IMPLEMENTADO

- [ ] Criar tabela `refresh_tokens` (token, userId, expiresAt, revoked)
- [ ] Endpoint `POST /auth/refresh` para renovar JWT
- [ ] Endpoint `POST /auth/logout` para revogar refresh token

---

## 3.4 Roles e Autorização Granular

### Status: ⚠️ PARCIALMENTE IMPLEMENTADO

- [x] Definir roles: `ROLE_INTERNAL` (implementado via InternalApiKeyFilter)
- [ ] Definir roles: `ROLE_USER`, `ROLE_ADMIN`
- [ ] Proteger `PUT /users/{id}/balance` para `ROLE_ADMIN` ou `ROLE_INTERNAL`
- [ ] Proteger `GET /fraud/analysis/{paymentId}` para `ROLE_ADMIN` ou `ROLE_INTERNAL`

---

## 3.5 Timeout em Transações

### Status: ❌ NÃO IMPLEMENTADO

- [ ] Adicionar `@Transactional(timeout = 5)` no `createPayment`
- [ ] Evita que chamada ao fraud-service segure transação aberta por tempo indeterminado

---

## 3.6 Circuit Breaker no Fraud Client

### Status: ❌ NÃO IMPLEMENTADO

- [ ] Adicionar Resilience4j Circuit Breaker no `AntiFraudClient`
- [ ] Definir política: fail-open (APPROVED com score 0) ou fail-closed (rejeitar)

---

## 3.7 Propagar traceId em Chamadas Feign

### Status: ✅ COMPLETAMENTE IMPLEMENTADO

- [x] Configurar `RequestInterceptor` no Feign para repassar header `X-Trace-Id`
- [x] Adicionar `traceId` no header de resposta HTTP
- [x] `RequestLoggingFilter` injeta traceId no MDC para rastreabilidade
- [x] Integração com TraceFlow library implementada
