# PayFlow — Strategy Pattern

## O que é

Após receber o resultado da análise antifraude, o `PaymentService` seleciona dinamicamente o comportamento correto sem usar `if/else` ou `switch`. Cada `Status_Fraud` tem um handler isolado com sua própria responsabilidade.

---

## Estrutura

```
strategy/
├── PaymentStatusHandler.java        ← interface (contrato)
├── factory/
│   └── PaymentStatusHandlerFactory  ← seleciona o handler pelo status
└── handlers/
    ├── ApprovedHandler              ← debita payer, credita payee → SUCCESS
    ├── RejectedHandler              ← status → FAILED
    ├── ManualAnalysisHandler        ← status → PENDING + Kafka alert
    ├── PendingReviewHandler         ← status → PENDING + Kafka alert
    └── SuspiciousHandler            ← status → PENDING + Kafka alert crítico
```

---

## Interface

```java
public interface PaymentStatusHandler {
    void handle(Payment payment, FraudAnalysisResponse response);
}
```

---

## Factory

```java
@Component
public class PaymentStatusHandlerFactory {

    private final Map<Status_Fraud, PaymentStatusHandler> handlers;

    public PaymentStatusHandlerFactory(...todos os handlers injetados...) {
        this.handlers = Map.of(
                Status_Fraud.APPROVED,         approvedHandler,
                Status_Fraud.REJECTED,         rejectedHandler,
                Status_Fraud.PENDING_REVIEW,   pendingReviewHandler,
                Status_Fraud.MANUAL_ANALYSIS,  manualAnalysisHandler,
                Status_Fraud.SUSPICIOUS,       suspiciousHandler
        );
    }

    public PaymentStatusHandler getHandler(Status_Fraud status) {
        PaymentStatusHandler handler = handlers.get(status);
        if (handler == null) {
            throw new IllegalArgumentException("Status inválido: " + status);
        }
        return handler;
    }
}
```

---

## Uso no service

```java
// ✅ Sem if/else — o padrão decide o comportamento
PaymentStatusHandler handler = factory.getHandler(fraudResponse.getStatus());
handler.handle(payment, fraudResponse);

// ❌ Nunca faça isso
if (status == APPROVED) { ... }
else if (status == REJECTED) { ... }
else if (status == SUSPICIOUS) { ... }
```

---

## Comportamento de cada handler

### `ApprovedHandler`
1. Busca `payer` e `payee` pelo UUID
2. Valida que `payer.balance >= payment.amount` (→ `FAILED` se não)
3. Debita `payer.balance -= amount`
4. Credita `payee.balance += amount`
5. Status → `SUCCESS`
6. Em qualquer exceção: status → `FAILED`

### `RejectedHandler`
1. Status → `FAILED`
2. Loga a rejeição

### `ManualAnalysisHandler`
1. Status → `PENDING`
2. Publica `PaymentAlertEvent` em `payflow.payment.alerts` com `alertType = "MANUAL_ANALYSIS"`

### `PendingReviewHandler`
1. Status → `PENDING`
2. Publica `PaymentAlertEvent` em `payflow.payment.alerts` com `alertType = "PENDING_REVIEW"`

### `SuspiciousHandler`
1. Status → `PENDING`
2. Publica `PaymentAlertEvent` em `payflow.payment.alerts` com `alertType = "SUSPICIOUS"`

---

## Como adicionar um novo handler

**Passo 1 — Criar o handler** em `strategy/handlers/`:

```java
@Component
@RequiredArgsConstructor
public class NovoStatusHandler implements PaymentStatusHandler {

    private final PaymentRepository paymentRepository;

    @Override
    public void handle(Payment payment, FraudAnalysisResponse response) {
        payment.setStatus(Enum_Payment.PENDING);
        paymentRepository.save(payment);
        // lógica específica
    }
}
```

**Passo 2 — Registrar na factory** em `PaymentStatusHandlerFactory`:

```java
// Injetar no construtor:
private final NovoStatusHandler novoStatusHandler;

// Adicionar no Map:
Status_Fraud.NOVO_STATUS, novoStatusHandler
```

**Passo 3 — Adicionar no enum** `Status_Fraud` em `commons`:

```java
public enum Status_Fraud {
    APPROVED, REJECTED, PENDING_REVIEW, MANUAL_ANALYSIS, SUSPICIOUS,
    NOVO_STATUS  // ← novo valor
}
```

> O `PaymentService` e os outros handlers **não são modificados**. O padrão isola completamente cada comportamento.

---

## Reutilização no fluxo de revisão manual

O mesmo strategy é reaproveitado pelo `ManualReviewService`. O `DecisionBuilder` converte a decisão humana no formato `FraudAnalysisResponse`, e o handler correspondente é executado:

```java
FraudAnalysisResponse response = DecisionBuilder.fromDecision(decision);
PaymentStatusHandler handler = factory.getHandler(response.getStatus());
handler.handle(payment, response);
```
