# PayFlow - Plano Detalhado de Tarefas (Tasks de Implementação)

Este documento centraliza todas as etapas técnicas do ecossistema PayFlow com status real extraído do código, dívidas técnicas identificadas e ideias de melhoria.

**Legenda:** ✅ Feito · ⚠️ Parcial/Divergência · 🐛 Bug/Dívida técnica · [ ] Pendente · 💡 Ideia nova

---

## 🚨 Bugs Técnicos Críticos (Prioridade Alta)

### 1.1 Entidade Transaction Incompleta
- 🐛 **Bug na migration V3**: colunas `payer_id` e `payee_id` existem no modelo Java mas **não foram adicionadas na tabela** `transactions` do script SQL
- 🐛 **`paymentId` é `String`** no modelo mas deveria ser `UUID` para consistência
- 🐛 **`@PrePersist` não seta `executedAt`** — campo `NOT NULL` no banco ficará nulo
- 🐛 **`Transaction` nunca é persistida**: após SUCCESS ou FAILED, nenhum registro é gravado na tabela
- [ ] **Criar migration `V5`** corrigindo a tabela `transactions`: adicionar `payer_id UUID NOT NULL` e `payee_id UUID NOT NULL`, alterar `payment_id` para `UUID`
- [ ] **Corrigir modelo Java** para alinhar com a migration corrigida
- [ ] **Implementar persistência de Transaction** no fluxo de pagamento

### 1.2 MANUAL_ANALYSIS Não Tratado no PaymentService
- ⚠️ O `fraud-service` pode retornar status `MANUAL_ANALYSIS` (score 30–70), mas o `PaymentService` só checa `REJECTED`
- Pagamentos de risco médio estão sendo aprovados silenciosamente
- [ ] **Tratar status `MANUAL_ANALYSIS`**: salvar como `PENDING_REVIEW` e não realizar débito/crédito, aguardando decisão manual
- [ ] Adicionar status `PENDING_REVIEW` no enum `Enum_Payment`

### 1.3 Concorrência no Saldo (Double-Spend Risk)
- 🐛 Atualmente `userRepository.save(payer)` e `userRepository.save(payee)` dentro de `@Transactional` sem lock explícito
- Em cenário de concorrência (dois pagamentos simultâneos do mesmo usuário), pode ocorrer double-spend
- [ ] Adicionar query com `@Lock(PESSIMISTIC_WRITE)` no `UserRepository`: `findByUuidForUpdate`

---

## ✅ Implementações Completas

### 2.1 Core Service - Entidades e Repositórios
- ✅ **Entidade User** com todos os campos necessários
- ✅ **Entidade Payment** com status, idempotency key, timestamps
- ✅ **Entidade Transaction** (modelo criado, mas não usada - ver bugs acima)
- ✅ **Migrations**: V1 (users), V2 (payments), V3 (transactions incompleta), V4 (remove constraints)
- ✅ **UserRepository**: `findByEmail`, `findByDocument`, `findByUuid`
- ✅ **PaymentRepository**: `findByIdempotencyKey`, `findByPayerIdOrPayeeId`, `findByUuid`
- ✅ **TransactionRepository**: `findByPayerIdAndExecutedAtAfter`, `findByPayeeIdAndExecutedAtAfter`

### 2.2 Core Service - Services
- ✅ **PaymentService**: fluxo síncrono com idempotência, validação, chamada antifraude
- ✅ **UserService**: operações administrativas (read, update, delete, balance)
- ✅ **AuthService**: registro com validação, BCrypt, JWT
- ✅ **JwtService**: geração e validação de tokens HS256
- ✅ **PaymentPersistenceHelper**: garante commit do PENDING antes da chamada ao fraud-service
- ✅ **ManualReviewService**: revisão manual de pagamentos pendentes
  - `getPendingPayments()` - Lista pagamentos PENDING
  - `getPaymentDetails(UUID)` - Detalhes de pagamento
  - `processDecision(ManualReviewDecision, UUID)` - Processa decisão manual
  - `getHistoryBySource(String)` - Histórico por fonte
  - `getHistoryByPaymentId(UUID)` - Histórico por pagamento

### 2.3 Core Service - Strategy Pattern para Status
- ✅ **PaymentStatusHandlerFactory**: Factory para selecionar handler correto
- ✅ **ApprovedHandler**: Trata pagamentos aprovados
- ✅ **RejectedHandler**: Trata pagamentos rejeitados
- ✅ **PendingReviewHandler**: Trata pagamentos em revisão pendente
- ✅ **ManualAnalysisHandler**: Trata pagamentos em análise manual
- ✅ **SuspiciousHandler**: Trata pagamentos suspeitos

### 2.4 Core Service - Controllers
- ✅ **PaymentController**: `POST /payments`, `GET /payments/{id}`, `GET /payments/users/{userId}`
- ✅ **UserController**: `GET /users/{id}`, `PUT /users/{id}/balance`
- ✅ **UserPeriodController**: `GET /users/{userId}/recent-transactions?period=1h|6h|24h|1d`
- ✅ **AuthController**: `POST /auth/register`, `POST /auth/login`
- ✅ **ManualReviewController**: revisão manual de pagamentos pendentes

### 2.5 Core Service - Kafka Consumers
- ✅ **AlertConsumerService** com Strategy Pattern:
  - `@KafkaListener(topics = "payflow.payment.alerts")` - Processa alertas de fraude
  - `@KafkaListener(topics = "payflow.review.completed")` - Processa decisões de revisão manual
  - **AlertHandler** - Interface para handlers de alerta
  - **AlertType** - Enum: PENDING_REVIEW, MANUAL_ANALYSIS, SUSPICIOUS
  - **PendingReviewHandler**, **ManualAnalysisHandler**, **SuspiciousHandler**
  - **AlertHandlerFactory** - Factory para selecionar handler
  - Removeu switch case com strings literais, usando tipagem forte

### 2.6 Fraud Service - Entidades e Repositórios
- ✅ **Entidade FraudAnalysisLog** com paymentId, score, status, reason, evaluatedAt
- ✅ **Migration V1** correspondente
- ✅ **FraudLogRepository**: `findByPaymentId`

### 2.7 Fraud Service - Motor de Regras
- ✅ **Interface RiskRule** com suporte a TransactionHistory
- ✅ **RiskRuleEngine**: Avalia todas as regras e calcula score total
- ✅ **FraudAnalysisService**: Orquestra análise, registra logs, atualiza cache
- ✅ **TransactionHistoryCacheService**: Cache em memória com Caffeine
  - Limpeza diária automática via scheduled task
  - Métodos: `addTransaction()`, `getUserTransactions()`, `countTransactionsInLastHours()`, etc.

### 2.8 Fraud Service - Regras Implementadas
**Regras Estáticas:**
- ✅ **NewPayeeHighValueRule** (peso: 70) - Beneficiário novo + valor > R$ 35.000
- ✅ **PayeeInactiveRule** (peso: 30) - Beneficiário inativo
- ✅ **InsufficientBalanceRule** (peso: 30) - Saldo insuficiente
- ✅ **HighValueRule** (peso: 30) - Valor > R$ 25.000
- ✅ **PayerInactiveRule** (peso: 40) - Pagador inativo

**Regras Baseadas em Histórico:**
- ✅ **VelocityRule** (peso: 50) - >10 pagamentos em 1h ou >30 em 24h
- ✅ **SameAmountPatternRule** (peso: 60) - >3 pagamentos do mesmo valor em 24h
- ✅ **RapidSuccessivePaymentsRule** (peso: 55) - >5 pagamentos em 5 minutos

### 2.9 Fraud Service - Controllers
- ✅ **FraudController**: `POST /fraud/analyze`, `GET /fraud/analysis/{paymentId}`
- ✅ **ManualAnalyzeController**: `PUT /manual-analyze/payment/{paymentId}`, `PUT /manual-analyze/user/{userId}`

### 2.10 Commons Module - DTOs e Enums
- ✅ **PaymentRequest**, **PaymentResponse**
- ✅ **UserRequest**, **UserResponse**
- ✅ **FraudAnalysisRequest**, **FraudAnalysisResponse**
- ✅ **PaymentAlertEvent**, **ManualReviewDecision**
- ✅ **Enums**: `Enum_Payment`, `User_Status`, `Status_Fraud`, `AlertType`

### 2.11 Segurança e Gateway
- ✅ **API Gateway**: rotas `/api/core/**` e `/api/fraud/**`, CORS global
- ✅ **Spring Security no core-service**:
  - `JwtAuthenticationFilter`: valida Bearer token
  - `InternalApiKeyFilter`: autentica service-to-service via `X-Internal-Token`
  - `RequestLoggingFilter`: injeta `traceId` UUID no MDC
  - `SecurityConfig`: rotas `/auth/**` públicas, demais exigem autenticação
- ✅ **Autenticação service-to-service**: fraud-service chama core-service com `X-Internal-Token`
- ✅ **KafkaConfig**: Error handler com DLQ, retry com backoff

---

## [ ] Melhorias Pendentes (Prioridade Média)

### 3.1 Validação de DTOs
- [ ] Adicionar Bean Validation (`@Valid`) nos DTOs de entrada
- [ ] `PaymentRequest`: `@NotNull` em campos obrigatórios, `@Positive` em amount
- [ ] `RegisterRequestDTO`: `@Email`, `@NotBlank`, `@Size(min=8)` em password
- [ ] Anotar parâmetros de controller com `@Valid`

### 3.2 Idempotência com Redis
- Container Redis já está configurado no `docker-compose.yml`
- [ ] Adicionar dependência `spring-boot-starter-data-redis`
- [ ] Implementar cache com TTL de 24h para idempotency keys
- [ ] Benefício: evita hit no banco para chaves recentes

### 3.3 Refresh Token
- [ ] Criar tabela `refresh_tokens` (token, userId, expiresAt, revoked)
- [ ] Endpoint `POST /auth/refresh` para renovar JWT
- [ ] Endpoint `POST /auth/logout` para revogar refresh token

### 3.4 Roles e Autorização Granular
- [ ] Definir roles: `ROLE_USER`, `ROLE_ADMIN`
- [ ] Proteger `PUT /users/{id}/balance` para `ROLE_ADMIN` ou `ROLE_INTERNAL`
- [ ] Proteger `GET /fraud/analysis/{paymentId}` para `ROLE_ADMIN` ou `ROLE_INTERNAL`

### 3.5 Timeout em Transações
- [ ] Adicionar `@Transactional(timeout = 5)` no `createPayment`
- [ ] Evita que chamada ao fraud-service segure transação aberta por tempo indeterminado

### 3.6 Circuit Breaker no Fraud Client
- [ ] Adicionar Resilience4j Circuit Breaker no `AntiFraudClient`
- [ ] Definir política: fail-open (APPROVED com score 0) ou fail-closed (rejeitar)

### 3.7 Propagar traceId em Chamadas Feign
- [ ] Configurar `RequestInterceptor` no Feign para repassar header `X-Trace-Id`
- [ ] Adicionar `traceId` no header de resposta HTTP

---

## [ ] Funcionalidades Futuras (Prioridade Baixa)

### 4.1 Kafka - Event-Driven Architecture
- [ ] Criar `KafkaTopicsConfig.java` com beans `NewTopic`
- [ ] Criar `PaymentEventProducer` no core-service
- [ ] Criar `PaymentEvent` DTO no commons
- [ ] Refatorar `POST /payments` para assíncrono (PROCESSING + Kafka)
- [ ] Criar `PaymentRequestedConsumer` no fraud-service
- [ ] Criar `FraudResultProducer` no fraud-service
- [ ] Criar `FraudCompletedConsumer` no core-service

### 4.2 Notificações
- [ ] Criar `TransactionCompletedConsumer`
- [ ] Implementar webhook simulado
- [ ] Implementar e-mail simulado (log)
- [ ] Adicionar container Mailhog no docker-compose
- [ ] Implementar retry com Spring Retry
- [ ] Implementar Dead Letter Topic
- [ ] Criar endpoint `POST /admin/dlq/replay`

### 4.3 Testes
- [ ] `PaymentServiceTest` com JUnit 5 + Mockito
- [ ] `FraudAnalysisServiceTest` testando cada regra
- [ ] `AuthServiceTest` registro e login
- [ ] `JwtServiceTest` geração e validação
- [ ] Testcontainers + PostgreSQL para testes de repositório
- [ ] `PaymentFlowIntegrationTest` fluxo ponta-a-ponta

### 4.4 Documentação de API
- [ ] Adicionar SpringDoc OpenAPI (`springdoc-openapi-starter-webmvc-ui`)
- [ ] Acessível em `/swagger-ui.html` de cada serviço
- [ ] Anotar controllers com `@Operation`, `@ApiResponse`
- [ ] Configurar rota no api-gateway para Swagger unificado

### 4.5 Observabilidade
- [ ] Adicionar `spring-boot-starter-actuator`
- [ ] Expor `/actuator/health` em ambos os serviços
- [ ] Adicionar `micrometer-registry-prometheus` para métricas
- [ ] Adicionar container Prometheus + Grafana no docker-compose
- [ ] Dashboards: taxa de pagamentos por status, latência p99, taxa de rejeição

### 4.6 Listagens com Paginação
- [ ] Adicionar `GET /payments` com `Pageable`
- [ ] Adicionar `GET /users` com `Pageable`
- [ ] Adicionar `findByStatus` no `PaymentRepository`

---

## 💡 Ideias e Melhorias Opcionais

- Adicionar campo `webhookUrl` na tabela `users` para notificações personalizadas
- Implementar Outbox Pattern para publicação de eventos Kafka
- Evoluir motor de regras para Drools ou IA
- Implementar rate limiting por usuário
- Adicionar auditoria completa de todas as operações
- Implementar feature flags para rollout gradual
- Adicionar integração com provedores de pagamento reais
