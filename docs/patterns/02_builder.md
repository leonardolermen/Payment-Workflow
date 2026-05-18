# PayFlow — Builder Pattern

## O que é neste projeto

Neste projeto, "Builder" **não** é o GoF Builder clássico com fluent API. É uma **classe utilitária estática** que converte um objeto de domínio em outro, centralizando o mapeamento de campos e qualquer derivação necessária.

> Builder = conversor entre objetos existentes  
> Factory = criador de objetos novos a partir de inputs externos

---

## Estrutura padrão

```java
public class XyzBuilder {

    private XyzBuilder() {} // utilitária — nunca instanciar

    public static TargetObject fromSourceObject(SourceObject source) {
        return TargetObject.builder()
                .field1(source.getField1())
                .field2(source.getField2())
                .build();
    }
}
```

---

## Builders existentes

| Classe | Entrada | Saída | Usado em |
|---|---|---|---|
| `AnalysisRequestBuilder` | `Payment` | `FraudAnalysisRequest` | `PaymentService` |
| `PaymentAlertEventBuilder` | `Payment` + `FraudAnalysisResponse` | `PaymentAlertEvent` | Handlers de status |
| `StatusHistoryBuilder` | `UUID` + `Enum_Payment` + `ManualReviewDecision` | `StatusHistory` | `ManualReviewService` |
| `DecisionBuilder` | `ManualReviewDecision` | `FraudAnalysisResponse` | `ManualReviewService` |
| `PaymentDetailsBuilder` | `Payment` | `PaymentDetailsRequest` | `ManualReviewService` |

---

## Exemplos reais

### `AnalysisRequestBuilder` — Payment → FraudAnalysisRequest

```java
// ❌ Inline (acoplado, repetível, difícil de testar)
FraudAnalysisRequest request = FraudAnalysisRequest.builder()
    .paymentId(payment.getUuid())
    .payerId(payment.getPayerId())
    .payeeId(payment.getPayeeId())
    .amount(payment.getAmount())
    .build();

// ✅ Com builder (centralizado, testável)
FraudAnalysisRequest request = AnalysisRequestBuilder.fromAnalysisRequest(payment);
```

---

### `StatusHistoryBuilder` — múltiplos objetos → StatusHistory

Este builder recebe **três inputs** e deriva campos automaticamente:

```java
StatusHistory history = StatusHistoryBuilder.fromManualReview(
    paymentId,           // UUID do pagamento
    payment.getStatus(), // oldStatus atual
    decision             // ManualReviewDecision com reviewerId e reason
);
```

Internamente ele:
1. Deriva `newStatus` → `"APPROVED"` string vira `Enum_Payment.APPROVED`
2. Define `source = "MANUAL_REVIEW"` automaticamente
3. Define `timestamp = LocalDateTime.now()`

---

### `DecisionBuilder` — ManualReviewDecision → FraudAnalysisResponse

Converte a decisão humana no mesmo formato que o antifraude retornaria, permitindo que o mesmo handler de strategy seja reaproveitado:

```java
FraudAnalysisResponse response = DecisionBuilder.fromDecision(decision);
// Depois reutiliza o handler:
PaymentStatusHandler handler = factory.getHandler(response.getStatus());
handler.handle(payment, response);
```

---

### `PaymentAlertEventBuilder` — Payment + FraudAnalysisResponse → PaymentAlertEvent

Constrói o evento Kafka de alerta combinando dados dos dois objetos:

```java
PaymentAlertEvent event = PaymentAlertEventBuilder.fromAlertEvento(payment, response);
kafkaTemplate.send(TOPIC, event);
```

---

## Quando criar um novo Builder

**Crie um Builder quando:**
- A construção envolve **dois ou mais objetos** de domínio diferentes
- Há **derivação de campo** (string → enum, cálculo de timestamp, status derivado)
- O mesmo mapeamento ocorre em **mais de um lugar** no código

**Não crie um Builder quando:**
- O mapeamento é trivial (1:1 de campos sem derivação)
- O objeto só tem uma fonte de dados (use Factory diretamente)
