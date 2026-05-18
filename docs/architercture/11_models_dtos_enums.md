# PayFlow — Modelos, DTOs e Enums

---

## Entidades (core-service)

### `User` — tabela `users`

| Coluna | Tipo | Constraints |
|---|---|---|
| `id` | Long | PK, auto-increment |
| `uuid` | UUID | unique, imutável |
| `name` | String | not null |
| `email` | String | unique, not null |
| `password` | String | not null (bcrypt) |
| `document` | String | unique, not null |
| `documentType` | String | not null |
| `balance` | BigDecimal | not null |
| `status` | User_Status | not null |
| `createdAt` | LocalDateTime | not null |

### `Payment` — tabela `payments`

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

### `StatusHistory` — tabela `status_history`

| Coluna | Tipo | Constraints |
|---|---|---|
| `id` | UUID | PK, auto-gerado |
| `ownerId` | UUID | UUID do pagamento |
| `oldStatus` | Enum_Payment | not null |
| `newStatus` | Enum_Payment | not null |
| `changedBy` | String | not null |
| `changeReason` | String | not null |
| `timestamp` | LocalDateTime | not null |
| `source` | String | not null (`FRAUD_ANALYSIS`, `MANUAL_REVIEW`) |

### `Transaction` — tabela `transactions`

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

---

## Entidades (fraud-service)

### `FraudAnalysisLog` — tabela `fraud_analysis_logs`

| Coluna | Tipo | Constraints |
|---|---|---|
| `Id` | Long | PK, auto-increment |
| `uuid` | UUID | unique, imutável |
| `paymentId` | UUID | unique, imutável |
| `score` | Double | not null |
| `status` | Status_Fraud | not null |
| `reason` | String | not null |
| `evaluatedAt` | LocalDateTime | not null |

---

## DTOs compartilhados (commons)

### `PaymentRequest`
| Campo | Tipo |
|---|---|
| `payerId` | UUID |
| `payeeId` | UUID |
| `amount` | BigDecimal |
| `idempotencyKey` | String |

### `PaymentResponse`
| Campo | Tipo |
|---|---|
| `id` | UUID |
| `payerId` | UUID |
| `payeeId` | UUID |
| `amount` | BigDecimal |
| `status` | Enum_Payment |
| `createdAt` | LocalDateTime |

### `FraudAnalysisRequest`
| Campo | Tipo |
|---|---|
| `paymentId` | UUID |
| `amount` | BigDecimal |
| `payerId` | UUID |
| `payeeId` | UUID |

### `FraudAnalysisResponse`
| Campo | Tipo |
|---|---|
| `status` | Status_Fraud |
| `score` | Double |
| `reason` | String |
| `paymentId` | UUID |

### `UserResponse`
| Campo | Tipo |
|---|---|
| `id` | UUID |
| `name` | String |
| `email` | String |
| `balance` | BigDecimal |
| `status` | User_Status |
| `document` | String |
| `documentType` | String |
| `createdAt` | LocalDateTime |

### `PaymentAlertEvent` (Kafka)
| Campo | Tipo |
|---|---|
| `paymentId` | UUID |
| `payerId` | UUID |
| `payeeId` | UUID |
| `amount` | BigDecimal |
| `alertType` | String |
| `reason` | String |
| `timestamp` | LocalDateTime |

### `ManualReviewDecision` (Kafka)
| Campo | Tipo |
|---|---|
| `paymentId` | UUID |
| `reviewerId` | String |
| `decision` | String |
| `reason` | String |
| `notes` | String |

---

## Enums

### `Enum_Payment`
| Valor | Descrição |
|---|---|
| `PENDING` | Aguardando análise ou revisão manual |
| `APPROVED` | Aprovado (intermediário) |
| `SUCCESS` | Transferência concluída |
| `REJECTED` | Rejeitado |
| `FAILED` | Falhou por erro ou saldo |

### `Status_Fraud`
| Valor | Score | Descrição |
|---|---|---|
| `APPROVED` | < 30 | Risco baixo |
| `MANUAL_ANALYSIS` | 30–69 | Requer revisão humana |
| `PENDING_REVIEW` | — | Aguardando revisão |
| `SUSPICIOUS` | — | Atividade suspeita |
| `REJECTED` | ≥ 70 | Alto risco — bloqueado |

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
