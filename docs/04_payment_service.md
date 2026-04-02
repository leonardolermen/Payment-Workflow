# Payment Service - Plano de Implementação

## Objetivo
É o coração coreógrafo na parte síncrona. Ele acolhe a intenção de pagar e orquestra o início dessa saga, persistindo estados temporários.

## Especificações Técnicas
- **Tecnologia**: Spring Boot + Kafka Producer + Redis.
- **Banco de Dados**: PostgreSQL (Table: `payments`).

## Responsabilidades Essenciais
1. **Idempotência (Crucial)**: Evitar cobranças duplicadas. Utilizará o cabeçalho HTTP `Idempotency-Key` enviado pelo cliente. O serviço armazena isso via Redis (Ex: `key=idempotency_uuid`, com TTL de 24h). Caso repita, devolve o mesmo `payment_id` e status sem refazer a chamada.
2. **Integração Externa (Síncrona)**: Bate no User Service para ver se cliente existe. Bate via FeignClient no `Fraud Service` para *risk rating*. Se for alta probabilidade de fraude, o status vai para `FAILED` imediatamente.
3. **Publicação no Kafka**: Salva o pagamento em banco (Status `PENDING`) e no meio da mesmíssima transação idealmente usaria Outbox Pattern ou envia o evento `PaymentCreated` (contendo dados resumidos JSON) para o tópico no Kafka.

## Endpoints
- `POST /payments`

## Plano de Execução Breve
1. Implementar verificação de idempotência no Filter ou Interceptor.
2. Criar a lógica que coordena a gravação do log em BD.
3. Implementar o Producer Kafka.

## Diagrama de Sequência - Criação de Pagamento

```mermaid
sequenceDiagram
    participant Client as Cliente
    participant Gateway as API Gateway
    participant Redis as Redis Cache
    participant Pay as Payment Service
    participant User as User Service
    participant Fraud as Fraud Service
    participant DB as Postgre (Payments)
    participant Kafka as Apache Kafka

    Client->>Gateway: POST /payments (Idempotency-Key)
    Gateway->>Pay: Forward Request
    
    Pay->>Redis: Verifica Idempotency-Key
    alt Chave já existe
        Redis-->>Pay: Retorna Dados Cacheados
        Pay-->>Gateway: 200 OK (Pagamento já processado)
        Gateway-->>Client: 200 OK
    else Chave nova
        Redis-->>Pay: Null
        Pay->>User: Valida Cliente (REST)
        User-->>Pay: OK
        
        Pay->>Fraud: Avalia Risco (REST)
        alt Risco Alto
            Fraud-->>Pay: REJECTED
            Pay-->>Gateway: 400 Bad Request
            Gateway-->>Client: Pagamento Recusado (Fraude)
        else Risco Aceitável
            Fraud-->>Pay: APPROVED
            Pay->>DB: Salva Pagamento (Status=PENDING)
            Pay->>Kafka: Produz Evento [PaymentCreated]
            Pay->>Redis: Salva Idempotency-Key
            Pay-->>Gateway: 202 Accepted (PENDING)
            Gateway-->>Client: 202 Status
        end
    end
```
