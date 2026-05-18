# PayFlow — Endpoints do Fraud Service

Base path: `/fraud`  
Via gateway: `/api/fraud/**`  
Porta: **8082**

---

## POST /fraud/analyze

Analisa um pagamento e retorna o resultado de risco.

> Chamado automaticamente pelo `core-service` via Feign durante `POST /payments`. Não é necessário chamar manualmente em fluxo normal.

**Request**
```json
{
  "paymentId": "uuid-do-pagamento",
  "amount": 1500.00,
  "payerId": "uuid-do-pagador",
  "payeeId": "uuid-do-recebedor"
}
```

**Response 200**
```json
{
  "status": "APPROVED",
  "score": 0.0,
  "reason": "Transação aprovada",
  "paymentId": "uuid-do-pagamento"
}
```

---

## GET /fraud/analysis/{paymentId}

Retorna todos os logs de análise antifraude de um pagamento.

**Response 200** — `List<FraudAnalysisLog>`
```json
[
  {
    "uuid": "uuid-do-log",
    "paymentId": "uuid-do-pagamento",
    "score": 30.0,
    "status": "MANUAL_ANALYSIS",
    "reason": "Análise manual necessária",
    "evaluatedAt": "2026-05-18T12:00:00"
  }
]
```

---

## Algoritmo de Score

O serviço busca dados do `core-service` (pagamento, pagador, recebedor) e calcula um score de 0 a 100:

| Regra | Pontos |
|---|---|
| `amount > R$ 25.000` | +30 |
| `payer.balance < amount` | +30 |
| `payer.status == INACTIVE` | +40 |
| `payee.status == INACTIVE` | +30 |
| `payee criado há < 7 dias` **E** `amount > R$ 35.000` | +70 |
| **Máximo** | **100** |

### Resultado por faixa

| Faixa | Status retornado |
|---|---|
| 0 – 29 | `APPROVED` |
| 30 – 69 | `MANUAL_ANALYSIS` |
| ≥ 70 | `REJECTED` |

---

## Banco de dados

Tabela: `fraud_analysis_logs` (banco `fraud_db`)

| Coluna | Tipo | Descrição |
|---|---|---|
| `uuid` | UUID | Identificador único do log |
| `paymentId` | UUID | UUID do pagamento analisado |
| `score` | Double | Score calculado (0–100) |
| `status` | Status_Fraud | Resultado da análise |
| `reason` | String | Motivo da decisão |
| `evaluatedAt` | LocalDateTime | Momento da análise |
