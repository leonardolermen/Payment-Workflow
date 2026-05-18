# PayFlow — Kafka: Tópicos, Producers e Consumers

Broker: `localhost:9092`  
Gerenciamento visual: `http://localhost:8090` (Kafka UI)

---

## Tópicos

| Tópico | Partições | Réplicas | Uso |
|---|---|---|---|
| `payflow.payment.requested` | 3 | 1 | Provisionado (fluxo futuro) |
| `payflow.fraud.completed` | 3 | 1 | Provisionado (fluxo futuro) |
| `payflow.payment.alerts` | 3 | 1 | Alertas de pagamentos suspeitos/pendentes |
| `payflow.transaction.completed` | 3 | 1 | Provisionado (fluxo futuro) |
| `payflow.review.completed` | 3 | 1 | Notificação de decisão de revisão manual |
| `payflow.payment.alerts.dlt` | 1 | 1 | Dead Letter Queue de alertas |
| `payflow.review.completed.dlt` | 1 | 1 | Dead Letter Queue de revisões |

---

## Producers

### `ManualAnalysisHandler` / `PendingReviewHandler` / `SuspiciousHandler`

- **Tópico:** `payflow.payment.alerts`
- **Key:** UUID do pagamento
- **Payload:** `PaymentAlertEvent`
- **Quando:** Antifraude retorna `MANUAL_ANALYSIS`, `PENDING_REVIEW` ou `SUSPICIOUS`

```json
{
  "paymentId": "uuid",
  "payerId": "uuid",
  "payeeId": "uuid",
  "amount": 1500.00,
  "alertType": "MANUAL_ANALYSIS",
  "reason": "Análise manual necessária",
  "timestamp": "2026-05-18T12:00:00"
}
```

**Valores de `alertType`:** `PENDING_REVIEW` | `MANUAL_ANALYSIS` | `SUSPICIOUS`

---

### `ManualReviewService`

- **Tópico:** `payflow.review.completed`
- **Payload:** `ManualReviewDecision`
- **Quando:** Analista submete decisão em `POST /api/manual-review/payment/{id}`

```json
{
  "paymentId": "uuid",
  "reviewerId": "analista-01",
  "decision": "APPROVED",
  "reason": "Cliente confirmou",
  "notes": "Verificado por telefone"
}
```

---

## Consumers

### `AlertConsumerService`

| Listener | Tópico | Group ID |
|---|---|---|
| `handlerPaymentAlert` | `payflow.payment.alerts` | `alert-group` |
| `handlerReviewCompleted` | `payflow.review.completed` | `review-notification-group` |

**Comportamento por `alertType`:**

| alertType | Ação |
|---|---|
| `PENDING_REVIEW` | Notifica equipe de análise (email simulado) |
| `MANUAL_ANALYSIS` | Notifica sistema de análise manual |
| `SUSPICIOUS` | Notifica equipe de segurança |

**`handlerReviewCompleted`:** notifica sistema externo da decisão tomada.

---

## Configuração do Consumer (core-service)

| Propriedade | Valor |
|---|---|
| Deserializador | `ErrorHandlingDeserializer` (wrapper seguro) |
| Tipo padrão | `PaymentAlertEvent` |
| Pacotes confiáveis | `com.payflow.commons.dto` |
| Retry | 3 tentativas, backoff exponencial: 1s → 2s → 10s |
| Ack mode | `RECORD` |

---

## Dead Letter Queues

Mensagens que excedem o número de retries são encaminhadas para os tópicos `.dlt`:

| Tópico principal | DLT |
|---|---|
| `payflow.payment.alerts` | `payflow.payment.alerts.dlt` |
| `payflow.review.completed` | `payflow.review.completed.dlt` |

Consumer group dedicado `dlq-group` monitora ambas as DLTs.
