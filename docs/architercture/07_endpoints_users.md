# PayFlow — Endpoints de Usuários, Revisão Manual e Histórico

Autenticação: **JWT obrigatório** em todos os endpoints desta página.

---

## Usuários — `/users`

> Operações administrativas. Para criar usuários via registro público, usar `/auth/register`.

### GET /users/{id}

Retorna dados de um usuário pelo UUID.

**Response 200**
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

**Erros:** `404` se não encontrado.

---

### PUT /users/{id}/balance

Atualiza o saldo de um usuário (operação **aditiva** — pode ser negativa para débito).

**Query param:** `amount` (BigDecimal)

**Response 200** — `UserResponse` atualizado.

---

### GET /users/{userId}/recent-transactions?period={period}

Retorna a contagem de transações recentes de um usuário.  
Usado internamente pelo `fraud-service`.

**Query param `period`:** `1h` | `6h` | `24h` | `1d`

**Response 200** — `Integer`

---

## Revisão Manual — `/api/manual-review`

Endpoints para analistas revisarem pagamentos em status `PENDING`.

### GET /api/manual-review/pending

Lista todos os pagamentos aguardando revisão (status `PENDING`).

**Response 200** — `List<PaymentDetailsRequest>`

---

### GET /api/manual-review/payment/{paymentId}

Detalhes de um pagamento específico para revisão.

**Response 200** — `PaymentDetailsRequest`

**Erros:** `404` se não encontrado.

---

### POST /api/manual-review/payment/{paymentId}

Registra a decisão de um revisor.

**Request**
```json
{
  "reviewerId": "analista-01",
  "decision": "APPROVED",
  "reason": "Cliente confirmou a operação",
  "notes": "Verificado por telefone"
}
```

**O que acontece:**
1. Persiste `StatusHistory` com a mudança de status
2. Executa o handler (`ApprovedHandler` ou `RejectedHandler`)
3. Publica `ManualReviewDecision` no tópico `payflow.review.completed`

**Response 200** — body vazio.

---

## Histórico de Status — `/api/history`

### GET /api/history/payment/{paymentId}

Retorna todo o histórico de mudanças de status de um pagamento.

**Response 200** — `List<StatusHistory>`

```json
[
  {
    "id": "uuid",
    "ownerId": "uuid-pagamento",
    "oldStatus": "PENDING",
    "newStatus": "SUCCESS",
    "changedBy": "analista-01",
    "changeReason": "Cliente confirmou a operação",
    "timestamp": "2026-05-18T12:30:00",
    "source": "MANUAL_REVIEW"
  }
]
```

---

### GET /api/history/source/{source}

Retorna histórico filtrado pela origem da mudança.

**Exemplos de `source`:** `FRAUD_ANALYSIS`, `MANUAL_REVIEW`

**Response 200** — `List<StatusHistory>`
