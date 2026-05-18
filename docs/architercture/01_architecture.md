# PayFlow — Arquitetura

## Visão Macro

```
                        ┌──────────────────┐
   Cliente HTTP  ──►    │   API Gateway    │  :8080
                        │  Spring Cloud    │
                        └────────┬─────────┘
               ┌─────────────────┴──────────────────┐
               ▼                                    ▼
     ┌──────────────────┐               ┌──────────────────┐
     │   core-service   │ ◄──── REST ──►│  fraud-service   │
     │      :8081       │    (Feign)    │      :8082       │
     └────────┬─────────┘               └────────┬─────────┘
              │                                  │
              │  Kafka (produce/consume)          │  PostgreSQL
              ▼                                  ▼
     ┌──────────────────┐               ┌──────────────────┐
     │  Apache Kafka    │               │   fraud_db       │
     │     :9092        │               └──────────────────┘
     └──────────────────┘
              │
              ▼
     ┌──────────────────┐
     │   payflow_db     │
     │  (PostgreSQL)    │
     └──────────────────┘
```

## Diagrama de Fluxo (Mermaid)

```mermaid
graph TD
    Client[Cliente] -->|Bearer JWT| GW[API Gateway :8080]

    GW -->|/api/core/**| Core[core-service :8081]
    GW -->|/api/fraud/**| Fraud[fraud-service :8082]

    Core -->|POST /fraud/analyze| Fraud
    Fraud -->|GET /payments/{id}| Core
    Fraud -->|GET /users/{id}| Core

    Core -->|produce| Kafka[Apache Kafka]
    Kafka -->|consume| Core

    Core --- DB1[(payflow_db)]
    Fraud --- DB2[(fraud_db)]
    Core --- Redis[(Redis)]
```

## Padrões Adotados

| Padrão | Onde |
|---|---|
| **Strategy** | Handlers de status pós-antifraude (`ApprovedHandler`, `RejectedHandler`, etc.) |
| **Factory** | `PaymentStatusHandlerFactory`, `PaymentFactory`, `UserFactory` |
| **Builder** | `AnalysisRequestBuilder`, `PaymentAlertEventBuilder`, `StatusHistoryBuilder` |
| **Idempotência** | Campo `idempotencyKey` único na tabela `payments` |
| **DLQ** | Tópicos `.dlt` para mensagens com falha no Kafka |

## Comunicação entre Serviços

| De | Para | Protocolo | Propósito |
|---|---|---|---|
| `core-service` | `fraud-service` | REST Feign | Solicitar análise de risco |
| `fraud-service` | `core-service` | REST Feign | Buscar dados de pagamento e usuários |
| `core-service` | Kafka | Produce | Publicar alertas de pagamento |
| `core-service` | Kafka | Consume | Processar alertas e decisões de revisão |

### Autenticação Interna

Todas as chamadas Feign entre serviços injetam o header `X-Internal-Token` com um shared secret configurado via `INTERNAL_API_TOKEN`. O `core-service` valida esse header via `InternalApiKeyFilter`.
