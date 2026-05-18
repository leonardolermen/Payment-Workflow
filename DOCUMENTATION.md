# PayFlow — Documentação Completa

> Sistema de pagamentos orientado a eventos com análise antifraude, revisão manual e notificações assíncronas.

---

## Índice

1. [Visão Geral](#1-visão-geral)
2. [Arquitetura](#2-arquitetura)
3. [Módulos](#3-módulos)
4. [Infraestrutura (Docker)](#4-infraestrutura-docker)
5. [Segurança](#5-segurança)
6. [Endpoints REST](#6-endpoints-rest)
7. [DTOs e Contratos](#7-dtos-e-contratos)
8. [Entidades e Modelos](#8-entidades-e-modelos)
9. [Enums](#9-enums)
10. [Services](#10-services)
11. [Strategy Pattern — Handlers de Status](#11-strategy-pattern--handlers-de-status)
12. [Kafka — Tópicos e Mensageria](#12-kafka--tópicos-e-mensageria)
13. [Fluxos de Negócio](#13-fluxos-de-negócio)
14. [Tratamento de Erros](#14-tratamento-de-erros)
15. [Comunicação entre Serviços](#15-comunicação-entre-serviços)

---

## 1. Visão Geral

**PayFlow** é uma plataforma de processamento de pagamentos baseada em microsserviços. Cada pagamento passa por um pipeline de validação que inclui:

- Verificação de idempotência e saldo
- Análise antifraude síncrona (REST)
- Roteamento por strategy pattern conforme o resultado da análise
- Notificações assíncronas via Kafka
- Revisão manual quando necessário
- Histórico de status auditável

**Stack principal:** Java 17 · Spring Boot 3 · Spring Security · Spring Kafka · Spring Cloud OpenFeign · PostgreSQL · Redis · Apache Kafka · Docker

---

## 2. Arquitetura

```
                          ┌─────────────────┐
       Client HTTP  ──►   │   API Gateway   │  :8080
                          │  (Spring Cloud) │
                          └────────┬────────┘
                     ┌─────────────┴──────────────┐
                     ▼                            ▼
           ┌──────────────────┐        ┌──────────────────┐
           │   core-service   │◄──────►│  fraud-service   │
           │     :8081        │  REST  │     :8082        │
           └────────┬─────────┘        └────────┬─────────┘
                    │  Kafka                     │  PostgreSQL
                    │  (produce/consume)         │  fraud_db
                    ▼                            ▼
           ┌──────────────────┐        ┌──────────────────┐
           │ Apache Kafka     │        │  PostgreSQL       │
           │    :9092         │        │  fraud_db        │
           └──────────────────┘        └──────────────────┘
                    │
                    ▼
           ┌──────────────────┐
           │  PostgreSQL       │
           │  payflow_db       │
           └──────────────────┘
```

### Comunicação entre Serviços

| Direção | Protocolo | Descrição |
|---|---|---|
| core-service → fraud-service | REST (Feign) | Solicita análise antifraude |
| fraud-service → core-service | REST (Feign) | Busca dados de pagamento e usuários |
| core-service → Kafka | Produce | Publica alertas de pagamento (`payflow.payment.alerts`) |
| core-service → Kafka | Produce | Notifica decisão de revisão (`payflow.review.completed`) |
| core-service | Consume | Consome alertas de pagamento e decisões de revisão |

### Autenticação Service-to-Service

Requisições internas entre `core-service` e `fraud-service` utilizam o header `X-Internal-Token` com um token compartilhado configurável via variável de ambiente `INTERNAL_API_TOKEN`.

---

## 3. Módulos

```
Payment-Workflow/
├── commons/          # DTOs, enums e contratos compartilhados entre serviços
├── api-gateway/      # Spring Cloud Gateway — roteamento e CORS
├── core-service/     # Serviço principal: auth, usuários, pagamentos, revisão manual
├── fraud-service/    # Serviço de análise antifraude
├── docker-compose.yml
└── pom.xml           # Parent POM (multi-módulo Maven)
```

### commons
Biblioteca compartilhada. Não expõe endpoints. Contém:
- DTOs de requisição/resposta usados em ambos os serviços
- Enums de status compartilhados

### api-gateway
Gateway de entrada único. Roteia:
- `/api/core/**` → `core-service` (:8081) com `StripPrefix=1`
- `/api/fraud/**` → `fraud-service` (:8082) com `StripPrefix=1`
- CORS liberado globalmente para todos os origins

### core-service
Núcleo do sistema. Responsável por:
- Registro e autenticação de usuários (JWT)
- CRUD administrativo de usuários
- Criação e consulta de pagamentos
- Orquestração do pipeline antifraude
- Revisão manual de pagamentos
- Histórico de status
- Produção e consumo de eventos Kafka

### fraud-service
Serviço dedicado à análise de risco. Responsável por:
- Calcular score de risco de um pagamento
- Persistir logs de análise antifraude
- Consultar dados do `core-service` via Feign

---

## 4. Infraestrutura (Docker)

Arquivo: `docker-compose.yml`

| Serviço | Imagem | Porta | Descrição |
|---|---|---|---|
| `payflow-postgres` | postgres:15-alpine | 5432 | Banco de dados principal |
| `payflow-redis` | redis:7-alpine | 6379 | Cache (provisionado, não ativamente usado no código atual) |
| `payflow-zookeeper` | confluentinc/cp-zookeeper:7.4.0 | 2181 | Coordenação do Kafka |
| `payflow-kafka` | confluentinc/cp-kafka:7.4.0 | 9092 / 29092 | Broker de mensagens |
| `payflow-kafka-ui` | provectuslabs/kafka-ui | 8090 | Interface web para monitoramento do Kafka |

### Bancos de dados

| Serviço | Database |
|---|---|
| core-service | `payflow_db` |
| fraud-service | `fraud_db` |

Ambos são criados automaticamente pelo script `init-db.sql` na inicialização do container.

As migrações de schema são gerenciadas pelo **Flyway** (`baseline-on-migrate: true`).

---

## 5. Segurança

### Autenticação de Usuários (JWT)
- Algoritmo: **HS256**
- Secret: configurado em `jwt.secret` (Base64 encoded)
- Expiração: `86400000ms` (24 horas)
- Token retornado no campo `token` do `AuthResponseDTO`
- Enviar como header: `Authorization: Bearer <token>`

### Autenticação Interna (Service-to-Service)
- Header: `X-Internal-Token`
- Valor padrão (dev): `dev-internal-shared-secret-change-me`
- Configurável via env: `INTERNAL_API_TOKEN`

### Portas e URLs (configuração local)

| Serviço | URL |
|---|---|
| API Gateway | `http://localhost:8080` |
| core-service | `http://localhost:8081` |
| fraud-service | `http://localhost:8082` |
| Kafka UI | `http://localhost:8090` |

---

## 6. Endpoints REST

### 6.1 Auth — `AuthController` (core-service)
Base path: `/auth`

---

#### `POST /auth/register`
Registra um novo usuário e retorna JWT.

**Request body:** `RegisterRequestDTO`
```json
{
  "name": "João Silva",
  "email": "joao@email.com",
  "password": "senha123",
  "confirmPassword": "senha123",
  "document": "123.456.789-00",
  "documentType": "CPF",
  "balance": 5000.00
}
```

**Validações:**
- `name`: obrigatório, 3–100 caracteres
- `email`: obrigatório, formato válido
- `password`: obrigatório, 6–100 caracteres
- `confirmPassword`: deve ser igual a `password`
- `document`: obrigatório
- `documentType`: `CPF` ou `CNPJ`

**Response `200 OK`:** `AuthResponseDTO`
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "userId": "uuid-do-usuario",
  "name": "João Silva",
  "email": "joao@email.com",
  "expiresAt": "2026-05-19T11:52:00"
}
```

**Erros:**
- `409 Conflict` — email já cadastrado
- `409 Conflict` — documento já cadastrado
- `400 Bad Request` — validações falhas

---

#### `POST /auth/login`
Autentica e retorna JWT.

**Request body:** `AuthRequestDTO`
```json
{
  "email": "joao@email.com",
  "password": "senha123"
}
```

**Response `200 OK`:** `AuthResponseDTO` (mesmo formato de `/register`)

**Erros:**
- `401 Unauthorized` — credenciais inválidas
- `404 Not Found` — usuário não encontrado

---

### 6.2 Usuários — `UserController` (core-service)
Base path: `/users`

> **Atenção:** Estes endpoints são para operações **administrativas**. Registro de novos usuários deve ser feito via `/auth/register`.

---

#### `GET /users/{id}`
Retorna dados de um usuário pelo UUID.

**Path param:** `id` (UUID)

**Response `200 OK`:** `UserResponse`
```json
{
  "id": "uuid",
  "name": "João Silva",
  "email": "joao@email.com",
  "balance": 4500.00,
  "status": "ACTIVE",
  "document": "123.456.789-00",
  "documentType": "CPF",
  "createdAt": "2026-05-18T10:00:00"
}
```

**Erros:**
- `404 Not Found` — usuário não encontrado

---

#### `PUT /users/{id}/balance`
Atualiza o saldo de um usuário (operação aditiva).

**Path param:** `id` (UUID)
**Query param:** `amount` (BigDecimal — pode ser negativo para débito)

**Response `200 OK`:** `UserResponse` atualizado

---

### 6.3 Pagamentos — `PaymentController` (core-service)
Base path: `/payments`

---

#### `POST /payments`
Cria um novo pagamento e dispara o pipeline antifraude.

**Request body:** `PaymentRequest`
```json
{
  "payerId": "uuid-do-pagador",
  "payeeId": "uuid-do-recebedor",
  "amount": 1500.00,
  "idempotencyKey": "chave-unica-do-cliente"
}
```

**Response `200 OK`:** `PaymentResponse`
```json
{
  "id": "uuid-do-pagamento",
  "payerId": "uuid-do-pagador",
  "payeeId": "uuid-do-recebedor",
  "amount": 1500.00,
  "status": "SUCCESS",
  "createdAt": "2026-05-18T11:00:00"
}
```

**Status possíveis no response:**
- `SUCCESS` — aprovado e concluído
- `PENDING` — em análise manual / revisão
- `FAILED` — rejeitado ou erro

**Erros:**
- `400 Bad Request` — saldo insuficiente
- `400 Bad Request` — pagador e recebedor são o mesmo
- `404 Not Found` — usuário não encontrado
- `409 Conflict` — `idempotencyKey` já utilizada

---

#### `GET /payments/{id}`
Retorna um pagamento pelo UUID.

**Response `200 OK`:** `PaymentResponse`

**Erros:**
- `404 Not Found`

---

#### `GET /payments/users/{userId}`
Lista todos os pagamentos de um usuário (como pagador ou recebedor).

**Response `200 OK`:** `List<PaymentResponse>`

---

#### `GET /payments/status/{status}`
Lista pagamentos por status.

**Path param:** `status` — `APPROVED` | `PENDING` | `REJECTED` | `SUCCESS` | `FAILED`

**Response `200 OK`:** `List<PaymentResponse>`

---

#### `GET /payments`
Lista todos os pagamentos.

**Response `200 OK`:** `List<PaymentResponse>`

---

### 6.4 Revisão Manual — `ManualReviewController` (core-service)
Base path: `/api/manual-review`

---

#### `GET /api/manual-review/pending`
Lista pagamentos com status `PENDING` aguardando revisão.

**Response `200 OK`:** `List<PaymentDetailsRequest>`

---

#### `GET /api/manual-review/payment/{paymentId}`
Retorna detalhes de um pagamento específico para revisão.

**Path param:** `paymentId` (UUID)

**Response `200 OK`:** `PaymentDetailsRequest`

---

#### `POST /api/manual-review/payment/{paymentId}`
Registra a decisão de um revisor sobre um pagamento.

**Path param:** `paymentId` (UUID)

**Request body:** `ManualReviewDecision`
```json
{
  "reviewerId": "analista-01",
  "decision": "APPROVED",
  "reason": "Transação verificada manualmente",
  "notes": "Cliente confirmou a operação"
}
```

**Comportamento:**
1. Persiste histórico de status (`StatusHistory`)
2. Executa o handler de status correspondente (aprova ou rejeita)
3. Publica evento no tópico `payflow.review.completed`

**Response `200 OK`:** body vazio

---

### 6.5 Histórico de Status — `HistoryController` (core-service)
Base path: `/api/history`

---

#### `GET /api/history/payment/{paymentId}`
Retorna o histórico de mudanças de status de um pagamento.

**Response `200 OK`:** `List<StatusHistory>`

---

#### `GET /api/history/source/{source}`
Retorna histórico de status filtrado pela origem da mudança.

**Exemplos de `source`:** `FRAUD_ANALYSIS`, `MANUAL_REVIEW`

**Response `200 OK`:** `List<StatusHistory>`

---

### 6.6 Transações Recentes — `UserPeriodController` (core-service)

#### `GET /users/{userId}/recent-transactions?period={period}`
Retorna a contagem de transações recentes de um usuário (usado internamente pelo fraud-service).

**Path param:** `userId` (UUID)
**Query param:** `period` — `1h` | `6h` | `24h` | `1d`

**Response `200 OK`:** `Integer` (contagem)

---

### 6.7 Fraude — `FraudController` (fraud-service)
Base path: `/fraud`

---

#### `POST /fraud/analyze`
Analisa um pagamento e retorna o resultado da análise de risco.

> Chamado pelo `core-service` via Feign Client.

**Request body:** `FraudAnalysisRequest`
```json
{
  "paymentId": "uuid-do-pagamento",
  "amount": 1500.00,
  "payerId": "uuid-do-pagador",
  "payeeId": "uuid-do-recebedor"
}
```

**Response `200 OK`:** `FraudAnalysisResponse`
```json
{
  "status": "APPROVED",
  "score": 0.0,
  "reason": "Transação aprovada",
  "paymentId": "uuid-do-pagamento"
}
```

---

#### `GET /fraud/analysis/{paymentId}`
Retorna todos os logs de análise antifraude para um pagamento.

**Response `200 OK`:** `List<FraudAnalysisLog>`

---

## 7. DTOs e Contratos

### commons — DTOs compartilhados

#### `PaymentRequest`
| Campo | Tipo | Descrição |
|---|---|---|
| `payerId` | UUID | UUID do pagador |
| `payeeId` | UUID | UUID do recebedor |
| `amount` | BigDecimal | Valor da transferência |
| `idempotencyKey` | String | Chave única para evitar duplicatas |

#### `PaymentResponse`
| Campo | Tipo | Descrição |
|---|---|---|
| `id` | UUID | UUID do pagamento |
| `payerId` | UUID | UUID do pagador |
| `payeeId` | UUID | UUID do recebedor |
| `amount` | BigDecimal | Valor |
| `status` | Enum_Payment | Status atual |
| `createdAt` | LocalDateTime | Data de criação |

#### `FraudAnalysisRequest`
| Campo | Tipo | Descrição |
|---|---|---|
| `paymentId` | UUID | UUID do pagamento a analisar |
| `amount` | BigDecimal | Valor |
| `payerId` | UUID | UUID do pagador |
| `payeeId` | UUID | UUID do recebedor |

#### `FraudAnalysisResponse`
| Campo | Tipo | Descrição |
|---|---|---|
| `status` | Status_Fraud | Resultado da análise |
| `score` | Double | Score de risco (0–100) |
| `reason` | String | Motivo |
| `paymentId` | UUID | UUID do pagamento |

#### `UserRequest`
| Campo | Tipo | Descrição |
|---|---|---|
| `name` | String | Nome |
| `email` | String | Email |
| `password` | String | Senha |
| `document` | String | Documento |

#### `UserResponse`
| Campo | Tipo | Descrição |
|---|---|---|
| `id` | UUID | UUID do usuário |
| `name` | String | Nome |
| `email` | String | Email |
| `balance` | BigDecimal | Saldo atual |
| `status` | User_Status | Status da conta |
| `document` | String | Documento |
| `documentType` | String | Tipo do documento |
| `createdAt` | LocalDateTime | Data de criação |

#### `PaymentAlertEvent` (Kafka)
| Campo | Tipo | Descrição |
|---|---|---|
| `paymentId` | UUID | UUID do pagamento |
| `payerId` | UUID | UUID do pagador |
| `payeeId` | UUID | UUID do recebedor |
| `amount` | BigDecimal | Valor |
| `alertType` | String | `PENDING_REVIEW` / `MANUAL_ANALYSIS` / `SUSPICIOUS` |
| `reason` | String | Motivo do alerta |
| `timestamp` | LocalDateTime | Timestamp do evento |

#### `ManualReviewDecision` (Kafka)
| Campo | Tipo | Descrição |
|---|---|---|
| `paymentId` | UUID | UUID do pagamento |
| `reviewerId` | String | ID do revisor |
| `decision` | String | Decisão: `APPROVED` / `REJECTED` |
| `reason` | String | Motivo da decisão |
| `notes` | String | Observações opcionais |

---

### core-service — DTOs locais

#### `RegisterRequestDTO`
| Campo | Tipo | Validações |
|---|---|---|
| `name` | String | `@NotBlank`, 3–100 chars |
| `email` | String | `@NotBlank`, `@Email` |
| `password` | String | `@NotBlank`, 6–100 chars |
| `confirmPassword` | String | deve ser igual a `password` |
| `document` | String | `@NotBlank` |
| `documentType` | Document_Type | `@NotNull` — `CPF` ou `CNPJ` |
| `balance` | BigDecimal | Opcional — saldo inicial |

#### `AuthRequestDTO`
| Campo | Tipo | Validações |
|---|---|---|
| `email` | String | `@NotBlank`, `@Email` |
| `password` | String | `@NotBlank` |

#### `AuthResponseDTO`
| Campo | Tipo | Descrição |
|---|---|---|
| `token` | String | JWT gerado |
| `type` | String | Sempre `"Bearer"` |
| `userId` | UUID | UUID do usuário autenticado |
| `name` | String | Nome |
| `email` | String | Email |
| `expiresAt` | LocalDateTime | Data/hora de expiração |

---

## 8. Entidades e Modelos

### `User` (tabela: `users`)
| Coluna | Tipo | Constraints |
|---|---|---|
| `id` | Long | PK, auto-increment |
| `uuid` | UUID | unique, not null, imutável |
| `name` | String | not null |
| `email` | String | unique, not null |
| `password` | String | not null (hash bcrypt) |
| `document` | String | unique, not null |
| `documentType` | String | not null |
| `balance` | BigDecimal | not null |
| `status` | User_Status | not null (`ACTIVE` / `INACTIVE`) |
| `createdAt` | LocalDateTime | not null |

### `Payment` (tabela: `payments`)
| Coluna | Tipo | Constraints |
|---|---|---|
| `id` | Long | PK, auto-increment |
| `uuid` | UUID | gerado no `@PrePersist` |
| `payerId` | UUID | not null |
| `payeeId` | UUID | not null |
| `amount` | BigDecimal | not null |
| `status` | Enum_Payment | enum string |
| `idempotencyKey` | String | unique, not null |
| `createdAt` | LocalDateTime | preenchido no `@PrePersist` |

### `StatusHistory` (tabela: `status_history`)
| Coluna | Tipo | Constraints |
|---|---|---|
| `id` | UUID | PK, auto-gerado |
| `ownerId` | UUID | UUID do pagamento (FK lógica) |
| `oldStatus` | Enum_Payment | not null |
| `newStatus` | Enum_Payment | not null |
| `changedBy` | String | quem realizou a mudança |
| `changeReason` | String | motivo da mudança |
| `timestamp` | LocalDateTime | momento da mudança |
| `source` | String | origem: `FRAUD_ANALYSIS`, `MANUAL_REVIEW` |

### `Transaction` (tabela: `transactions`)
| Coluna | Tipo | Constraints |
|---|---|---|
| `id` | Long | PK, auto-increment |
| `uuid` | UUID | gerado no `@PrePersist` |
| `paymentId` | String | referência ao UUID do pagamento |
| `status` | Enum_Transaction | `SUCCESS` / `FAILED` |
| `reason` | String | not null |
| `payeeId` | UUID | not null |
| `payerId` | UUID | not null |
| `executedAt` | LocalDateTime | not null |

### `FraudAnalysisLog` (tabela: `fraud_analysis_logs`) — fraud-service
| Coluna | Tipo | Constraints |
|---|---|---|
| `Id` | Long | PK, auto-increment |
| `uuid` | UUID | unique, imutável, gerado no `@PrePersist` |
| `paymentId` | UUID | unique, imutável |
| `score` | Double | not null |
| `status` | Status_Fraud | not null |
| `reason` | String | not null |
| `evaluatedAt` | LocalDateTime | not null |

---

## 9. Enums

### `Enum_Payment` (status do pagamento)
| Valor | Descrição |
|---|---|
| `PENDING` | Aguardando análise ou revisão manual |
| `APPROVED` | Aprovado (intermediário) |
| `SUCCESS` | Transferência concluída com sucesso |
| `REJECTED` | Rejeitado |
| `FAILED` | Falhou por erro ou insuficiência |

### `Status_Fraud` (resultado da análise antifraude)
| Valor | Score | Descrição |
|---|---|---|
| `APPROVED` | < 30 | Risco baixo — pagamento liberado |
| `MANUAL_ANALYSIS` | 30–69 | Risco médio — requer revisão humana |
| `PENDING_REVIEW` | — | Aguardando revisão |
| `SUSPICIOUS` | — | Atividade suspeita detectada |
| `REJECTED` | ≥ 70 | Alto risco — pagamento bloqueado |

### `User_Status`
| Valor | Descrição |
|---|---|
| `ACTIVE` | Conta ativa |
| `INACTIVE` | Conta inativa |

### `Document_Type`
| Valor | Descrição |
|---|---|
| `CPF` | Pessoa Física |
| `CNPJ` | Pessoa Jurídica |

### `Enum_Transaction`
| Valor | Descrição |
|---|---|
| `SUCCESS` | Transação bem-sucedida |
| `FAILED` | Transação falhou |

---

## 10. Services

### `AuthService` (core-service)
Responsável por registro e autenticação.

| Método | Descrição |
|---|---|
| `register(RegisterRequestDTO)` | Valida unicidade de email e documento, codifica senha com bcrypt, cria usuário via `UserFactory`, gera JWT |
| `login(AuthRequestDTO)` | Autentica via `AuthenticationManager`, busca usuário, gera JWT |

### `UserService` (core-service)
CRUD administrativo de usuários.

| Método | Descrição |
|---|---|
| `readUserById(UUID)` | Busca usuário pelo UUID |
| `updateUser(UUID, UserRequest)` | Atualiza dados do usuário |
| `delete(UUID)` | Remove o usuário |
| `updateBalance(UUID, BigDecimal)` | Adiciona/subtrai valor do saldo (operação aditiva) |

### `PaymentService` (core-service)
Orquestra o pipeline completo de pagamentos.

| Método | Descrição |
|---|---|
| `createPayment(PaymentRequest)` | Pipeline completo: idempotência → validação → persistência → antifraude → handler de status |
| `getById(UUID)` | Busca pagamento pelo UUID |
| `getByUser(UUID)` | Lista pagamentos do usuário (como pagador ou recebedor) |
| `getByStatus(Enum_Payment)` | Filtra pagamentos por status |
| `getAll()` | Lista todos os pagamentos |

### `ManualReviewService` (core-service)
Gerencia o fluxo de revisão manual.

| Método | Descrição |
|---|---|
| `getPendingPayments()` | Lista pagamentos com status `PENDING` |
| `getPaymentDetails(UUID)` | Detalhes de um pagamento para revisão |
| `processDecision(ManualReviewDecision, UUID)` | Salva histórico, executa handler de status, publica evento Kafka |
| `getHistoryBySource(String)` | Busca histórico de status por origem |
| `getHistoryByPaymentId(UUID)` | Busca histórico de status de um pagamento |

### `JwtService` (core-service)
Gerencia tokens JWT.

| Método | Descrição |
|---|---|
| `generateToken(UserDetails)` | Gera token JWT assinado com HS256 |
| `isTokenValid(String, UserDetails)` | Valida token e verifica expiração |
| `extractUsername(String)` | Extrai o subject (email) do token |

### `FraudAnalysisService` (fraud-service)
Executa a análise de risco.

| Método | Descrição |
|---|---|
| `analyzePayment(FraudAnalysisRequest)` | Busca dados via Feign, calcula score, persiste log, retorna resultado |
| `calculateRiskScore(...)` | Algoritmo de scoring (veja detalhes abaixo) |
| `determineStatus(score)` | Converte score numérico em `Status_Fraud` |
| `determineReason(score, payment)` | Gera mensagem de razão |

### `AlertConsumerService` (core-service)
Consome eventos Kafka.

| Listener | Tópico | Ação |
|---|---|---|
| `handlerPaymentAlert` | `payflow.payment.alerts` | Roteia para email, análise manual ou equipe de segurança conforme `alertType` |
| `handlerReviewCompleted` | `payflow.review.completed` | Notifica sistema externo da decisão de revisão |

---

## 11. Strategy Pattern — Handlers de Status

Após receber o resultado da análise antifraude, o `PaymentService` utiliza o `PaymentStatusHandlerFactory` para selecionar o handler correto baseado no `Status_Fraud` retornado. O mesmo pattern é usado pelo `ManualReviewService` ao processar uma decisão.

```
PaymentStatusHandlerFactory
├── APPROVED       → ApprovedHandler
├── REJECTED       → RejectedHandler
├── PENDING_REVIEW → PendingReviewHandler
├── MANUAL_ANALYSIS→ ManualAnalysisHandler
└── SUSPICIOUS     → SuspiciousHandler
```

### `ApprovedHandler`
- Verifica saldo do pagador novamente
- Debita valor do pagador (`payer.balance -= amount`)
- Credita valor ao recebedor (`payee.balance += amount`)
- Atualiza status do pagamento para `SUCCESS`
- Em caso de erro: status → `FAILED`

### `RejectedHandler`
- Atualiza status do pagamento para `FAILED`
- Loga a rejeição

### `PendingReviewHandler`
- Atualiza status do pagamento para `PENDING`
- Publica `PaymentAlertEvent` no tópico `payflow.payment.alerts`

### `ManualAnalysisHandler`
- Atualiza status do pagamento para `PENDING`
- Publica `PaymentAlertEvent` no tópico `payflow.payment.alerts`

### `SuspiciousHandler`
- Atualiza status do pagamento para `PENDING`
- Publica `PaymentAlertEvent` (alerta crítico) no tópico `payflow.payment.alerts`

---

## 12. Kafka — Tópicos e Mensageria

### Tópicos

| Tópico | Partições | Réplicas | Producer | Consumer | Payload |
|---|---|---|---|---|---|
| `payflow.payment.requested` | 3 | 1 | — | — | — (provisionado) |
| `payflow.fraud.completed` | 3 | 1 | — | — | — (provisionado) |
| `payflow.payment.alerts` | 3 | 1 | `ManualAnalysisHandler`, `PendingReviewHandler`, `SuspiciousHandler` | `AlertConsumerService` (group: `alert-group`) | `PaymentAlertEvent` |
| `payflow.transaction.completed` | 3 | 1 | — | — | — (provisionado) |
| `payflow.review.completed` | 3 | 1 | `ManualReviewService` | `AlertConsumerService` (group: `review-notification-group`) | `ManualReviewDecision` |
| `payflow.payment.alerts.dlt` | 1 | 1 | — | — | Dead Letter Queue de alertas |
| `payflow.review.completed.dlt` | 1 | 1 | — | — | Dead Letter Queue de revisões |

### Configuração do Consumer (core-service)
- **Deserialização segura:** `ErrorHandlingDeserializer` como wrapper
- **Tipo padrão:** `PaymentAlertEvent`
- **Retry:** 3 tentativas com backoff exponencial (1s → 2s → 10s)
- **Ack mode:** `RECORD`
- **Pacotes confiáveis:** `com.payflow.commons.dto`

---

## 13. Fluxos de Negócio

### 13.1 Fluxo Principal — Criar Pagamento

```
Cliente
  │
  ├─ POST /payments
  │
  ▼
PaymentService.createPayment()
  │
  ├─ 1. Valida idempotencyKey (409 se duplicada)
  ├─ 2. Busca payer e payee no banco (404 se não encontrado)
  ├─ 3. Valida payer ≠ payee (400)
  ├─ 4. Valida saldo do payer (400 se insuficiente)
  ├─ 5. Cria Payment com status PENDING (REQUIRES_NEW tx)
  │
  ├─ 6. Chama fraud-service via Feign
  │       POST /fraud/analyze
  │       ▼
  │   FraudAnalysisService.analyzePayment()
  │       ├─ Busca Payment em core-service (GET /payments/{id})
  │       ├─ Busca Payer em core-service (GET /users/{id})
  │       ├─ Busca Payee em core-service (GET /users/{id})
  │       ├─ Calcula riskScore:
  │       │     +30 se amount > R$25.000
  │       │     +30 se saldo do payer < amount
  │       │     +40 se payer INATIVO
  │       │     +30 se payee INATIVO
  │       │     +70 se payee criado < 7 dias E amount > R$35.000
  │       │     Score máximo: 100
  │       ├─ Persiste FraudAnalysisLog
  │       └─ Retorna FraudAnalysisResponse { status, score, reason }
  │
  ├─ 7. PaymentStatusHandlerFactory.getHandler(status)
  │
  └─ 8. Handler executa:
        ├── APPROVED       → debita/credita + SUCCESS
        ├── REJECTED       → FAILED
        ├── PENDING_REVIEW → PENDING + Kafka alert
        ├── MANUAL_ANALYSIS→ PENDING + Kafka alert
        └── SUSPICIOUS     → PENDING + Kafka alert
```

### 13.2 Fluxo de Revisão Manual

```
Analista
  │
  ├─ GET /api/manual-review/pending         → lista pagamentos PENDING
  ├─ GET /api/manual-review/payment/{id}    → detalha pagamento
  │
  └─ POST /api/manual-review/payment/{id}
         { decision: "APPROVED", reviewerId: "analista-01", ... }
         │
         ▼
  ManualReviewService.processDecision()
         │
         ├─ 1. Busca Payment pelo UUID
         ├─ 2. Persiste StatusHistory (oldStatus → newStatus, source=MANUAL_REVIEW)
         ├─ 3. Converte decision → FraudAnalysisResponse via DecisionBuilder
         ├─ 4. Executa PaymentStatusHandler (APPROVED ou REJECTED)
         └─ 5. Publica ManualReviewDecision em payflow.review.completed
                   │
                   ▼
            AlertConsumerService.handlerReviewCompleted()
                   └─ Notifica sistema externo
```

### 13.3 Fluxo de Alertas Kafka

```
Handler publica PaymentAlertEvent
  em payflow.payment.alerts
         │
         ▼
AlertConsumerService.handlerPaymentAlert()
         │
         ├─ alertType = "PENDING_REVIEW"  → sendEmailToAnalysisTeam()
         ├─ alertType = "MANUAL_ANALYSIS" → notifyManualAnalysisSystem()
         └─ alertType = "SUSPICIOUS"      → notifySecurityTeam()
```

### 13.4 Cálculo de Score Antifraude

| Regra | Pontos |
|---|---|
| Valor > R$ 25.000 | +30 |
| Saldo do pagador < valor da transação | +30 |
| Pagador com status INACTIVE | +40 |
| Recebedor com status INACTIVE | +30 |
| Recebedor criado há < 7 dias **E** valor > R$ 35.000 | +70 |
| **Score máximo** | **100** |

| Faixa de score | Status retornado |
|---|---|
| 0–29 | `APPROVED` |
| 30–69 | `MANUAL_ANALYSIS` |
| ≥ 70 | `REJECTED` |

---

## 14. Tratamento de Erros

O `GlobalExceptionHandler` (presente em ambos os serviços) padroniza todas as respostas de erro:

```json
{
  "status": 400,
  "message": "Descrição do erro",
  "errors": {
    "traceId": "uuid-de-rastreamento",
    "campo": "mensagem de validação"
  },
  "timestamp": "2026-05-18T11:52:00"
}
```

### Mapeamento de Exceções

| Exceção | HTTP Status | Mensagem |
|---|---|---|
| `MethodArgumentNotValidException` | `400` | Erros de campo detalhados |
| `HttpMessageNotReadableException` | `400` | JSON inválido ou mal formatado |
| `InvalidFormatException` | `400` | Valor inválido para enum/tipo |
| `EmailAlreadyExistsException` | `409` | Email já cadastrado |
| `DocumentAlreadyExistsException` | `409` | Documento já cadastrado |
| `UserNotFoundException` | `404` | Usuário não encontrado |
| `ResponseStatusException` | variável | Razão da exceção (5xx → mensagem genérica) |
| `RuntimeException` | `500` | Mensagem genérica |
| `Exception` | `500` | Mensagem genérica |

### Rastreabilidade
Cada request recebe um `traceId` gerado pelo `RequestLoggingFilter` e propagado via **MDC (Mapped Diagnostic Context)** para logs e respostas de erro.

---

## 15. Comunicação entre Serviços

### Feign Clients

#### `AntiFraudClient` (core-service → fraud-service)
```
URL: ${anti-fraud-service.url}  (padrão: http://localhost:8082)

POST /fraud/analyze
  body:  FraudAnalysisRequest
  return: FraudAnalysisResponse
```

#### `CoreServiceClient` (fraud-service → core-service)
```
URL: ${core-service.url}  (padrão: http://localhost:8081)

GET /payments/{paymentId}              → PaymentResponse
GET /users/{userId}                    → UserResponse
GET /users/{userId}/recent-transactions?period={period} → Integer
```

Autenticação das chamadas Feign internas: configurada via `FeignInternalAuthConfig` que injeta o header `X-Internal-Token` automaticamente.

---

*Documentação gerada em 18/05/2026 — PayFlow v1.0*
