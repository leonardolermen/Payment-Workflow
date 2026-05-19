# [ ] Melhorias Pendentes

Melhorias que fazem sentido implementar (prioridade média).

---

## 3.1 Validação de DTOs

- [ ] Adicionar Bean Validation (`@Valid`) nos DTOs de entrada
- [ ] `PaymentRequest`: `@NotNull` em campos obrigatórios, `@Positive` em amount
- [ ] `RegisterRequestDTO`: `@Email`, `@NotBlank`, `@Size(min=8)` em password
- [ ] Anotar parâmetros de controller com `@Valid`

---

## 3.2 Idempotência com Redis

Container Redis já está configurado no `docker-compose.yml`

- [ ] Adicionar dependência `spring-boot-starter-data-redis`
- [ ] Implementar cache com TTL de 24h para idempotency keys
- [ ] Benefício: evita hit no banco para chaves recentes

---

## 3.3 Refresh Token

- [ ] Criar tabela `refresh_tokens` (token, userId, expiresAt, revoked)
- [ ] Endpoint `POST /auth/refresh` para renovar JWT
- [ ] Endpoint `POST /auth/logout` para revogar refresh token

---

## 3.4 Roles e Autorização Granular

- [ ] Definir roles: `ROLE_USER`, `ROLE_ADMIN`
- [ ] Proteger `PUT /users/{id}/balance` para `ROLE_ADMIN` ou `ROLE_INTERNAL`
- [ ] Proteger `GET /fraud/analysis/{paymentId}` para `ROLE_ADMIN` ou `ROLE_INTERNAL`

---

## 3.5 Timeout em Transações

- [ ] Adicionar `@Transactional(timeout = 5)` no `createPayment`
- [ ] Evita que chamada ao fraud-service segure transação aberta por tempo indeterminado

---

## 3.6 Circuit Breaker no Fraud Client

- [ ] Adicionar Resilience4j Circuit Breaker no `AntiFraudClient`
- [ ] Definir política: fail-open (APPROVED com score 0) ou fail-closed (rejeitar)

---

## 3.7 Propagar traceId em Chamadas Feign

- [ ] Configurar `RequestInterceptor` no Feign para repassar header `X-Trace-Id`
- [ ] Adicionar `traceId` no header de resposta HTTP
