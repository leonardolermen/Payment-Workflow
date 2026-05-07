# PayFlow - Plano Detalhado de Tarefas (Tasks de Implementação)

Este documento centraliza todas as etapas técnicas de alto a baixo nível exigidas para finalizar o ecossistema PayFlow. Siga esta ordem ou atribua as *epics* de forma paralela.

---

## Fase 1: Modelagem de Dados e Entidades (JPA)
A fundação do sistema. Começaremos desenhando as tabelas e entidades principais no `core-service` e no `fraud-service`.

### 1.1 Entidades do `core-service`
- ✅ **Criar Entidade `User` (Perfil de cliente)**
  - Campos: `id` (UUID), `name`, `email` (único), `password` (criptografada), `cpfCnpj` (único), `balance` (Decimal), `status` (ACTIVE, INACTIVE), `createdAt`.
  - Configurar mapeamento `@Table(name = "users")` e restrições.
- ✅ **Criar Entidade `Payment` (Intenção de Pagamento)**
  - Campos: `id` (UUID), `payerId` (Vínculo c/ User do pagador), `payeeId` (Vínculo c/ User recebedor), `amount` (Decimal), `status` (PENDING, REJECTED, SUCCESS, FAILED), `idempotencyKey` (Unique), `createdAt`.
  - Configurar mapeamento `@Table(name = "payments")`.
- ✅ **Criar Entidade `Transaction` (Liquidação Efetiva)**
  - Campos: `id` (UUID), `paymentId` (Vínculo c/ Payment), `status` (SUCCESS, FAILED), `reason` (varchar para logs de falha), `executedAt`.
  - Configurar mapeamento `@Table(name = "transactions")`.
- ✅ **Criar Script Migration `V1`**
  - Mapear a criação dessas entidades no script Flyway `V1__init.sql` no `core-service`.

### 1.2 Entidades do `fraud-service`
- ✅ **Criar Entidade `FraudAnalysisLog`**
  - Campos: `id` (UUID), `paymentId` (UUID de referência do core), `score` (Decimal/Double), `status` (APPROVED, REJECTED), `reason`, `evaluatedAt`.
  - Configurar mapeamento na Base do Fraud (`@Table(name = "fraud_analysis_logs")`).
- ✅ **Criar Script Migration `V1` do Fraud**
  - Escrever DDL correspondente no arquivo `V1__init.sql` do `fraud-service`.

---

## Fase 2: Regras de Banco de Dados e Repository Pattern
- ✅ Criar `UserRepository`, `PaymentRepository`, e `TransactionRepository` no `core-service` estendendo `JpaRepository`.
- ✅ Criar `FraudLogRepository` no `fraud-service`.
- ✅ Escrever queries customizadas (ex: Buscar usuário por e-mail, buscar pagamento por Idempotency-key).
  - `findByEmail`, `findByDocument`, `findByUuid` no `UserRepository`
  - `findByIdempotencyKey`, `findByPayerIdOrPayeeId`, `findByUuid` no `PaymentRepository`

---

## Fase 2.5: Controllers, Services e DTOs (Camada de Aplicação)
**Componentes essenciais que expõem a API e contêm a lógica de negócio**

### 2.5.1 DTOs (Data Transfer Objects)
- ✅ **Criar DTOs do core-service**:
  - `PaymentRequestDTO` / `PaymentResponseDTO` no `core-service`
  - `UserRequest` / `UserResponse` no módulo `commons` (compartilhados entre serviços)
- ✅ `AuthRequestDTO` - Login (email, password)
- ✅ `AuthResponseDTO` - Token JWT + dados do usuário
- ✅ **Criar DTOs do fraud-service**:
- ✅ `FraudAnalysisRequestDTO` - Dados para análise (paymentId, amount, payerId, payeeId)
- ✅ `FraudAnalysisResponseDTO` - Resultado (status, score, reason)

### 2.5.2 Services (Camada de Negócio)
- ✅ **Services do core-service**:
  - `PaymentService` - Idempotência via DB, validação de saldo, integração síncrona com fraud, débito/crédito
  - `UserService` - CRUD administrativo (read, update, delete, updateBalance)
- ✅ `AuthService` - Geração/validação JWT, criptografia de senhas (BCrypt)
- ✅ **Services do fraud-service**:
- ✅ `FraudAnalysisService` - Regras de análise de fraude, cálculo de score, persistência

### 2.5.3 Controllers (Endpoints REST)
- ✅ **Controllers do core-service**:
  - `PaymentController` - `POST /payments`, `GET /payments/{id}`, `GET /payments/users/{userId}`
  - `UserController` - `GET /users/{id}`, `PUT /users/{id}`, `DELETE /users/{id}`, `PUT /users/{id}/balance`
  - `UserPeriodController` - endpoint auxiliar de usuários por período
- ✅ `AuthController` - `POST /auth/register`, `POST /auth/login`
- ✅ **Controllers do fraud-service**:
- ✅ `FraudController` - `POST /fraud/analyze`, `GET /fraud/analysis/{paymentId}`

---

## Fase 3: Segurança, Gateway e Autenticação
- ✅ **Configurar API Gateway (`api-gateway`)**:
  - Rotas `/api/core/**` → `localhost:8081` e `/api/fraud/**` → `localhost:8082` definidas no `application.yml`.
  - CORS global configurado via `globalcors`.
- ✅ **Módulo de Autenticação (`core-service`)**:
  - `spring-boot-starter-security` + `jjwt` adicionados.
  - `JwtAuthenticationFilter` + `InternalApiKeyFilter` + `SecurityConfig` implementados.
  - `POST /auth/register` e `POST /auth/login` funcionais.

---

## Fase 4: O Coração do Pagamento (Payment API síncrona)
- ⚠️ **Configurar Redis**: dependência `spring-boot-starter-data-redis` **ainda não adicionada**.
  - *Divergência*: idempotência foi implementada via consulta ao banco (`findByIdempotencyKey`) em vez de cache Redis com TTL. Funcional, mas sem expiração automática de 24h.
- ✅ **Implementar Endpoint `POST /payments` (`core-service`)**:
  - Idempotência via DB (`findByIdempotencyKey`), validação de saldo, registro em `payments`.
- ✅ **Integração Feign / REST c/ Fraud Service**:
  - `AntiFraudClient` via `@FeignClient` implementado no `core-service`.
  - `POST /fraud/analyze` funcional no `fraud-service` com regras e persistência.
- ✅ **Atualização Conforme Antifraude**:
  - REJECTED → `payment.status = FAILED` + `400`.
  - APPROVED → débito/crédito síncrono + `payment.status = SUCCESS`.

---

## Fase 5: Mensageria (Event-Driven Flow c/ Kafka)
> ⚠️ Dependência `spring-kafka` já adicionada no `pom.xml`, mas nenhuma classe de Producer/Consumer criada ainda.
- [ ] **Producers**: 
  - Configurar classe produtora no `core-service` para postar no tópico `payment-created-topic` após aprovar no antifraude.
- [ ] **Configurar Tópicos**:
  - Via Docker-compose ou código Java (`NewTopic`), expor os tópicos padrão `payment-created-topic` e `transaction-completed-topic`.

---

## Fase 6: Liquidação Financeira (Transaction Logic)
*(Assumindo que essa lógica ficará no `core-service` ou em um módulo novo focado em consumer)*
- [ ] **Kafka Consumer de Pagamentos**:
  - Escrever `@KafkaListener(topics = "payment-created-topic")` no componente de transação.
- [ ] **Lógica de Lock Otimista/Pessimista de Saldo**:
  - Pegar o `payerId` (remetente), travar linha (`@Lock(LockModeType.PESSIMISTIC_WRITE)`).
  - Verificar se saldo é suficiente.
  - Deduzir balance do payer, transferir balance na conta do `payee`.
- [ ] **Salvar Transação**:
  - Criar `Transaction` no BD da liquidação com os registros da operação aprovada ou negada por saldo insuficiente.
- [ ] **Emissão de Conclusão**:
  - Produzir evento de sucesso/falha no tópico `transaction-completed-topic`.

---

## Fase 7: Sistema de Notificações e Webhooks
- [ ] **Criar / Configurar Notification Consumer (`core-service` ou microserviço à parte)**:
  - Ouver `@KafkaListener(topics = "transaction-completed-topic")`.
- [ ] **Disparo de Serviços Externos**:
  - Atualizar status final da linha na tabela `payments` (SUCCESS ou FAILED).
  - Implementar envio simulado de Webhook (`RestTemplate` fazendo POST pra URL de log do cliente).
  - Envio simulado de e-mail (Log ou serviço JavaMailSender).
- [ ] **Tratamento de Falhas e Dead Letter Queue**:
  - Configurar Resilience4j + DLT (Dead Letter Topic). Se a notificação falhar 3x por erro 500 do Webhook, droppar no tópico `-dql` do Kafka.
