# PayFlow — Endpoints de Pagamento

Base path: `/payments`  
Via gateway: `/api/core/payments/**`  
Autenticação: **JWT obrigatório**

---

## POST /payments

Cria um pagamento e dispara o pipeline antifraude.

**Request**
```json
{
  "payerId": "uuid-do-pagador",
  "payeeId": "uuid-do-recebedor",
  "amount": 1500.00,
  "idempotencyKey": "pedido-xyz-001"
}
```

**Response 200**
```json
{
  "id": "uuid-do-pagamento",
  "payerId": "uuid-do-pagador",
  "payeeId": "uuid-do-recebedor",
  "amount": 1500.00,
  "status": "SUCCESS",
  "createdAt": "2026-05-18T12:00:00"
}
```

**Status possíveis no response**

| Status | Significado |
|---|---|
| `SUCCESS` | Aprovado pelo antifraude e transferência concluída |
| `PENDING` | Em análise manual — aguarda revisão humana |
| `FAILED` | Rejeitado pelo antifraude ou erro na execução |

**Erros**

| Status | Motivo |
|---|---|
| `400` | Saldo insuficiente |
| `400` | Pagador e recebedor são o mesmo usuário |
| `404` | Usuário (pagador ou recebedor) não encontrado |
| `409` | `idempotencyKey` já utilizada |

---

## GET /payments/{id}

Retorna um pagamento pelo UUID.

**Response 200** — `PaymentResponse`

**Erros:** `404` se não encontrado.

---

## GET /payments/users/{userId}

Lista todos os pagamentos de um usuário (como pagador **ou** recebedor).

**Response 200** — `List<PaymentResponse>`

---

## GET /payments/status/{status}

Lista pagamentos por status.

**Path param:** `APPROVED` | `PENDING` | `REJECTED` | `SUCCESS` | `FAILED`

**Response 200** — `List<PaymentResponse>`

---

## GET /payments

Lista todos os pagamentos.

**Response 200** — `List<PaymentResponse>`

---

## Pipeline de criação

```
POST /payments
  │
  ├─ 1. Valida idempotencyKey          → 409 se duplicada
  ├─ 2. Busca payer e payee            → 404 se não encontrado
  ├─ 3. payer ≠ payee                  → 400 se iguais
  ├─ 4. payer.balance >= amount        → 400 se insuficiente
  ├─ 5. Persiste Payment (PENDING)     → REQUIRES_NEW transaction
  ├─ 6. Chama fraud-service            → POST /fraud/analyze
  └─ 7. Executa handler conforme score:
        APPROVED        → debita/credita → SUCCESS
        REJECTED        → FAILED
        MANUAL_ANALYSIS → PENDING + Kafka alert
        PENDING_REVIEW  → PENDING + Kafka alert
        SUSPICIOUS      → PENDING + Kafka alert
```
