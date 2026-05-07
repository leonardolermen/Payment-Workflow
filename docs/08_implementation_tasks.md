# PayFlow - Plano Detalhado de Tarefas (Tasks de Implementação)

Este documento centraliza todas as etapas técnicas do ecossistema PayFlow com status real extraído do código, dívidas técnicas identificadas e ideias de melhoria.

**Legenda:** ✅ Feito · ⚠️ Parcial/Divergência · 🐛 Bug/Dívida técnica · [ ] Pendente · 💡 Ideia nova

---

## Fase 1: Modelagem de Dados e Entidades (JPA)

### 1.1 Entidades do `core-service`
- ✅ **Entidade `User`**
  - Campos: `id` (Long PK auto), `uuid` (UUID único, imutável), `name`, `email` (único), `password` (BCrypt), `document` (CPF/CNPJ único), `documentType`, `balance` (NUMERIC 19,2), `status` (ACTIVE/INACTIVE), `createdAt`.
  - `@PrePersist` gera UUID automaticamente.

- ✅ **Entidade `Payment`**
  - Campos: `id` (Long PK auto), `uuid` (UUID), `payerId`, `payeeId`, `amount` (BigDecimal), `status` (PENDING/SUCCESS/FAILED/REJECTED), `idempotencyKey` (único), `createdAt`.
  - `@PrePersist` gera UUID e seta `createdAt`.

- ⚠️ **Entidade `Transaction`** — modelo criado, mas **nunca persistida no fluxo atual**
  - Campos presentes no modelo: `uuid`, `paymentId` (String), `status`, `reason`, `payerId`, `payeeId`, `executedAt`.
  - 🐛 **Bug na migration V3**: colunas `payer_id` e `payee_id` existem no modelo Java mas **não foram adicionadas na tabela** `transactions` do script SQL. Qualquer tentativa de salvar uma `Transaction` vai causar erro de coluna inexistente.
  - 🐛 **`paymentId` é `String`** no modelo mas deveria ser `UUID` para consistência com o restante do sistema.
  - 🐛 **`@PrePersist` não seta `executedAt`** — campo `NOT NULL` no banco ficará nulo ao persistir sem setar manualmente.
  - [ ] **Criar migration `V5`** corrigindo a tabela `transactions`: adicionar `payer_id UUID NOT NULL` e `payee_id UUID NOT NULL`, e alterar `payment_id` para `UUID`.
  - [ ] **Corrigir modelo Java** para alinhar com a migration corrigida.

- ✅ **Migrations `core-service`**
  - V1: tabela `users`
  - V2: tabela `payments`
  - V3: tabela `transactions` (incompleta — ver bugs acima)
  - V4: remove constraints `UNIQUE` errôneas de `payer_id`/`payee_id` em `payments`

### 1.2 Entidades do `fraud-service`
- ✅ **Entidade `FraudAnalysisLog`** com `paymentId`, `score`, `status` (APPROVED/REJECTED/MANUAL_ANALYSIS), `reason`, `evaluatedAt`.
- ✅ **Migration V1 do `fraud-service`** correspondente.

---

## Fase 2: Repositórios e Queries

- ✅ **`UserRepository`**: `findByEmail`, `findByDocument`, `findByUuid`.
- ✅ **`PaymentRepository`**: `findByIdempotencyKey`, `findByPayerIdOrPayeeId`, `findByUuid`.
- ✅ **`TransactionRepository`**: `findByPayerIdAndExecutedAtAfter`, `findByPayeeIdAndExecutedAtAfter` (usado em `UserPeriodController`).
- ✅ **`FraudLogRepository`**: `findByPaymentId`.
- 💡 **Ideia**: Adicionar `findByStatus` no `PaymentRepository` para futura listagem admin de pagamentos por status (PENDING, FAILED, etc.).

---

## Fase 2.5: DTOs, Services e Controllers

### 2.5.1 DTOs
- ✅ **`commons` module** — DTOs compartilhados entre serviços:
  - `PaymentRequest` (payerId, payeeId, amount, idempotencyKey)
  - `PaymentResponse` (id, status, amount, createdAt...)
  - `UserRequest` / `UserResponse` (campos completos incluindo documentType, balance, status)
  - `FraudAnalysisRequest` / `FraudAnalysisResponse`
  - Enums: `Enum_Payment`, `User_Status`, `Status_Fraud`
- ✅ **`core-service`** — DTOs locais: `AuthRequestDTO`, `AuthResponseDTO`, `RegisterRequestDTO`, `PaymentRequestDTO`, `PaymentResponseDTO`.
- [ ] **Adicionar Bean Validation (`@Valid`)** nos campos dos DTOs de entrada:
  - `PaymentRequest`: `@NotNull` em `payerId`, `payeeId`, `amount`; `@Positive` em `amount`; `@NotBlank` em `idempotencyKey`.
  - `RegisterRequestDTO`: `@Email`, `@NotBlank`, `@Size(min=8)` em `password`.
  - Anotar parâmetros de controller com `@Valid` para ativar a validação automática.

### 2.5.2 Services
- ✅ **`PaymentService`** — fluxo síncrono completo:
  1. Valida idempotência via DB.
  2. Busca payer e payee.
  3. Valida auto-transferência e saldo suficiente.
  4. Persiste pagamento como PENDING em transação separada (`REQUIRES_NEW`) via `PaymentPersistenceHelper`.
  5. Chama `AntiFraudClient` via Feign.
  6. Se REJECTED → atualiza status para FAILED e lança exceção.
  7. Se APPROVED → debita payer, credita payee, marca SUCCESS.
  - ⚠️ **`MANUAL_ANALYSIS` não tratado**: o `fraud-service` pode retornar esse status (score 30–70), mas o `PaymentService` só checa `REJECTED`. Pagamentos de risco médio estão sendo aprovados silenciosamente.
  - 🐛 **`Transaction` nunca é persistida**: após SUCCESS ou FAILED, nenhum registro é gravado na tabela `transactions`. O modelo e o repositório existem mas não são usados.
  - 💡 **TODO já marcado no código**: implementar Strategy Pattern para tratamento dos diferentes status do antifraude (`APPROVED`, `REJECTED`, `MANUAL_ANALYSIS`).

- ✅ **`UserService`** — operações administrativas: `readUserById`, `updateUser`, `delete`, `updateBalance`.

- ✅ **`AuthService`** — registro com validação de e-mail e documento únicos, BCrypt, geração de JWT. Login via `AuthenticationManager`.

- ✅ **`JwtService`** — geração e validação de tokens HS256, expiração configurável via `application.yml`.

- ✅ **`FraudAnalysisService`** — motor de score de risco:
  - +50 pontos: valor > R$ 15.000
  - +30 pontos: saldo do pagador insuficiente (double-check)
  - +40 pontos: pagador com status != ACTIVE
  - +30 pontos: recebedor com status != ACTIVE
  - +30 pontos: recebedor com conta criada há menos de 30 dias
  - Score > 70 → REJECTED; score 30–70 → MANUAL_ANALYSIS; score < 30 → APPROVED
  - ⚠️ **Verificação de frequência comentada**: o código para contar transações recentes do pagador/recebedor (via `CoreServiceClient.getRecentTransactionCount`) está comentado aguardando ativação.

- ✅ **`PaymentPersistenceHelper`** — garante que o PENDING seja comitado antes da chamada ao fraud-service (que precisa enxergar o registro via `GET /payments/{id}`). Usa `Propagation.REQUIRES_NEW`.

### 2.5.3 Controllers
- ✅ **`PaymentController`**: `POST /payments`, `GET /payments/{id}`, `GET /payments/users/{userId}`.
- ✅ **`UserController`**: `GET /users/{id}`, `PUT /users/{id}/balance`.
- ✅ **`UserPeriodController`**: `GET /users/{userId}/recent-transactions?period=1h|6h|24h|1d` — usado pelo fraud-service para verificar frequência de transações.
- ✅ **`AuthController`**: `POST /auth/register`, `POST /auth/login`.
- ✅ **`FraudController`**: `POST /fraud/analyze`, `GET /fraud/analysis/{paymentId}`.
- 💡 **Ideia**: Adicionar `GET /payments` com paginação (`Pageable`) para listagem admin de todos os pagamentos.
- 💡 **Ideia**: Adicionar `GET /users` com paginação para listagem de usuários.

---

## Fase 3: Segurança, Gateway e Autenticação

- ✅ **API Gateway** (`api-gateway`, porta 8080):
  - Rota `/api/core/**` → `core-service` (porta 8081), StripPrefix=1.
  - Rota `/api/fraud/**` → `fraud-service` (porta 8082), StripPrefix=1.
  - CORS global via `globalcors` (allowedOriginPatterns `*`, todos os métodos).

- ✅ **Spring Security no `core-service`**:
  - `JwtAuthenticationFilter`: extrai e valida Bearer token no header `Authorization`.
  - `InternalApiKeyFilter`: autentica chamadas service-to-service via header `X-Internal-Token`, concede role `ROLE_INTERNAL`. Token configurado via env var `INTERNAL_API_TOKEN`.
  - `RequestLoggingFilter`: injeta `traceId` (UUID) no MDC para rastreabilidade em todos os logs.
  - `SecurityConfig`: rotas `/auth/**` e `/error` públicas; demais exigem autenticação.

- ✅ **Autenticação service-to-service**: `fraud-service` chama `core-service` com `X-Internal-Token` via `FeignInternalAuthConfig`.

- [ ] **Refresh Token**: JWT atual tem expiração de 24h sem mecanismo de renovação.
  - Criar tabela `refresh_tokens` (token, userId, expiresAt, revoked).
  - Endpoint `POST /auth/refresh` que valida o refresh token e emite novo JWT.
  - Endpoint `POST /auth/logout` que revoga o refresh token.

- [ ] **Roles e Autorização granular**: atualmente só existe `ROLE_INTERNAL`. 
  - Definir roles de usuário final: `ROLE_USER`, `ROLE_ADMIN`.
  - Proteger `PUT /users/{id}/balance` e `GET /fraud/analysis/{paymentId}` para `ROLE_ADMIN` ou `ROLE_INTERNAL` apenas.

---

## Fase 4: Correções e Melhorias no Fluxo Síncrono Atual

> Antes de partir para Kafka, consolidar o fluxo síncrono sem bugs.

- [ ] **Persistir `Transaction` após desfecho do pagamento**:
  - No `PaymentService`, após marcar SUCCESS: criar registro `Transaction` com status SUCCESS, reason "Aprovado", payerId, payeeId, paymentId, executedAt.
  - Após marcar FAILED (fraude ou saldo): criar registro `Transaction` com status FAILED e reason descritivo.
  - Depende da correção das migrations e do modelo (item da Fase 1.1).

- [ ] **Tratar status `MANUAL_ANALYSIS` no `PaymentService`**:
  - Opção A (simples): tratar como APPROVED por ora, logando um alerta (`log.warn`).
  - Opção B (correta): salvar pagamento como status `PENDING_REVIEW` e não realizar o débito/crédito, aguardando decisão manual. Exige novo status no enum `Enum_Payment`.

- [ ] **Implementar Strategy Pattern para desfechos do antifraude** (TODO já marcado no código):
  - Interface `FraudResponseStrategy` com método `handle(Payment, FraudAnalysisResponse)`.
  - Implementações: `ApprovedStrategy`, `RejectedStrategy`, `ManualAnalysisStrategy`.
  - `PaymentService` delega para a strategy correta com base no status retornado.

- ⚠️ **Redis para idempotência** (estava no plano original, não implementado):
  - Container Redis já está configurado no `docker-compose.yml`.
  - Adicionar dependência `spring-boot-starter-data-redis` no `pom.xml` do `core-service`.
  - Implementar cache com TTL de 24h: ao receber `Idempotency-Key`, checar no Redis antes de ir ao banco. Guardar o `PaymentResponse` serializado para devolver imediatamente em caso de duplicata.
  - Benefício: evita hit no banco para chaves recentes.

- [ ] **Lock Pessimista no saldo** (`@Lock(LockModeType.PESSIMISTIC_WRITE)`):
  - Atualmente `userRepository.save(payer)` e `userRepository.save(payee)` dentro de `@Transactional` sem lock explícito.
  - Em cenário de concorrência (dois pagamentos simultâneos do mesmo usuário), pode ocorrer double-spend.
  - Adicionar query com `@Lock` no `UserRepository`: `findByUuidForUpdate`.

- 💡 **Ideia**: Adicionar `@Transactional(timeout = 5)` no `createPayment` para evitar que a chamada ao fraud-service (que é I/O externo) segure a transação aberta por tempo indeterminado.

- 💡 **Ideia**: Adicionar Feign fallback via Resilience4j Circuit Breaker no `AntiFraudClient`:
  - Se o fraud-service estiver down, retornar APPROVED com score 0 (fail-open) ou rejeitar por segurança (fail-closed). Definir política.

---

## Fase 5: Mensageria — Kafka (Event-Driven Architecture)

> Infraestrutura Kafka já está 100% pronta: `spring-kafka` no `pom.xml`, configurações de producer/consumer com JSON serializers em ambos `application.yml`, Kafka + Zookeeper + Kafka-UI no `docker-compose.yml`.

### 5.1 Configuração de Tópicos (Java)
- [ ] **Criar `KafkaTopicsConfig.java`** no `core-service`:
  - Bean `NewTopic` para `payflow.payment.requested` (1 partição, replication=1).
  - Bean `NewTopic` para `payflow.fraud.completed`.
  - Bean `NewTopic` para `payflow.transaction.completed`.
  - Bean `NewTopic` para `payflow.notification.failed.dlq`.

### 5.2 Refatoração do Fluxo para Assíncrono (Core Producer)
- [ ] **Criar `PaymentEventProducer`** no `core-service`:
  - Injeta `KafkaTemplate<String, Object>`.
  - Método `publishPaymentRequested(PaymentEvent event)` que publica em `payflow.payment.requested`.
- [ ] **Criar `PaymentEvent` DTO** no módulo `commons`:
  - Campos: `paymentId` (UUID), `payerId`, `payeeId`, `amount`, `idempotencyKey`, `timestamp`.
- [ ] **Refatorar `POST /payments`**:
  - Salva pagamento com status `PROCESSING` (novo status no enum).
  - Publica evento no Kafka.
  - Retorna `202 Accepted` com o `paymentId` para o cliente consultar depois.

### 5.3 Motor de Antifraude Assíncrono (Fraud Consumer + Producer)
- [ ] **Criar `PaymentRequestedConsumer`** no `fraud-service`:
  - `@KafkaListener(topics = "payflow.payment.requested", groupId = "fraud-group")`.
  - Executa `FraudAnalysisService.analyzePayment(...)` com os dados do evento.
- [ ] **Criar `FraudResultProducer`** no `fraud-service`:
  - Após análise, publica em `payflow.fraud.completed` um `FraudResultEvent` com `paymentId` e `status` (APPROVED/REJECTED/MANUAL_ANALYSIS).

### 5.4 Consumer de Resultado de Fraude + Liquidação (Core Consumer)
- [ ] **Criar `FraudCompletedConsumer`** no `core-service`:
  - `@KafkaListener(topics = "payflow.fraud.completed", groupId = "core-group")`.
  - Se REJECTED: atualiza `payment.status = FAILED`, persiste `Transaction` (FAILED), publica em `payflow.transaction.completed`.
  - Se APPROVED: executa débito/crédito com `@Lock(PESSIMISTIC_WRITE)`, atualiza `payment.status = SUCCESS`, persiste `Transaction` (SUCCESS), publica em `payflow.transaction.completed`.
  - Se MANUAL_ANALYSIS: atualiza `payment.status = PENDING_REVIEW`, não movimenta saldo.

---

## Fase 6: Notificações e Dead Letter Queue

### 6.1 Consumer de Conclusão
- [ ] **Criar `TransactionCompletedConsumer`** (pode ficar no `core-service` ou em um futuro `notification-service`):
  - `@KafkaListener(topics = "payflow.transaction.completed", groupId = "notification-group")`.
  - Recebe evento com resultado final da transação.

### 6.2 Disparo de Notificações
- [ ] **Webhook simulado**:
  - `RestTemplate` (ou `WebClient`) fazendo `POST` para uma URL configurável por usuário (campo `webhookUrl` na tabela `users` — novo campo).
  - Payload: `{ paymentId, status, amount, timestamp }`.
- [ ] **E-mail simulado**:
  - Logar evento de envio via `log.info("email.send to={} subject={}", user.getEmail(), ...)`.
  - Opcional: integrar `JavaMailSender` com SMTP mock (Mailhog via Docker).
- 💡 **Ideia**: Adicionar container Mailhog no `docker-compose.yml` para simular recebimento de e-mails em desenvolvimento.

### 6.3 Resiliência e DLQ
- [ ] **Retry com Spring Retry ou Resilience4j**:
  - Anotar método de disparo de webhook com `@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))`.
- [ ] **Dead Letter Topic**:
  - Após 3 falhas consecutivas, enviar mensagem para `payflow.notification.failed.dlq`.
  - 💡 Criar endpoint admin `POST /admin/dlq/replay` para reinjetar mensagens do DLQ no tópico original.

---

## Fase 7: Qualidade de Código e Testes

> Esta fase pode (e deve) andar em paralelo com as demais.

### 7.1 Testes Unitários
- [ ] **`PaymentServiceTest`** (JUnit 5 + Mockito):
  - Cenário: idempotência detectada → deve lançar `409 CONFLICT`.
  - Cenário: saldo insuficiente → deve lançar `400 BAD_REQUEST`.
  - Cenário: auto-transferência → deve lançar `400 BAD_REQUEST`.
  - Cenário: fraude REJECTED → deve salvar FAILED e lançar exceção.
  - Cenário: fraude APPROVED → deve debitar/creditar e salvar SUCCESS.
- [ ] **`FraudAnalysisServiceTest`**:
  - Testar cada regra de score individualmente (valor alto, conta nova, status inativo).
  - Testar limites de threshold (score 29 → APPROVED, score 30 → MANUAL_ANALYSIS, score 71 → REJECTED).
- [ ] **`AuthServiceTest`**: registro com e-mail duplicado, login com credenciais inválidas.
- [ ] **`JwtServiceTest`**: geração de token, validação, token expirado.

### 7.2 Testes de Integração
- [ ] **Testcontainers** + PostgreSQL para testes de repositório.
- [ ] **`PaymentFlowIntegrationTest`**: fluxo ponta-a-ponta síncrono com banco real (Testcontainers).

### 7.3 Documentação de API
- [ ] **Adicionar SpringDoc OpenAPI** (`springdoc-openapi-starter-webmvc-ui`) ao `core-service` e `fraud-service`.
  - Acessível em `/swagger-ui.html` de cada serviço.
  - Anotar controllers com `@Operation`, `@ApiResponse`.
  - 💡 Configurar rota no `api-gateway` para expor Swagger unificado.

---

## Fase 8: Observabilidade e Infra

### 8.1 Logging estruturado (já parcialmente feito)
- ✅ `RequestLoggingFilter` injeta `traceId` UUID no MDC em cada request.
- ✅ `GlobalExceptionHandler` propaga `traceId` em todos os responses de erro.
- [ ] **Propagar `traceId` nas chamadas Feign**: configurar `RequestInterceptor` no Feign para repassar o header `X-Trace-Id` para os serviços downstream.
- 💡 **Ideia**: Adicionar `traceId` no header de resposta HTTP (`response.setHeader("X-Trace-Id", traceId)`) para facilitar debugging pelo cliente.

### 8.2 Métricas e Dashboard (opcional)
- 💡 Adicionar `spring-boot-starter-actuator` + `micrometer-registry-prometheus` para expor métricas em `/actuator/prometheus`.
- 💡 Adicionar container Prometheus + Grafana no `docker-compose.yml`.
- 💡 Dashboards sugeridos: taxa de pagamentos por status, latência p99 do endpoint `/payments`, taxa de rejeição do antifraude.

### 8.3 Health Checks
- [ ] **Adicionar `spring-boot-starter-actuator`** e expor `/actuator/health` em ambos os serviços.
- [ ] Configurar rota no `api-gateway` para `/health` de cada serviço.
