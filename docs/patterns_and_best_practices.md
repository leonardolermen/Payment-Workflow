# PayFlow — Padrões de Projeto e Boas Práticas

> Referência para devs e IAs. Descreve os padrões implementados no projeto, como usá-los, quando criar novos e o que evitar.

---

## Índice

1. [Estrutura de Pacotes](#1-estrutura-de-pacotes)
2. [Builder Pattern — Conversores Estáticos](#2-builder-pattern--conversores-estáticos)
3. [Factory Pattern — Criação de Objetos](#3-factory-pattern--criação-de-objetos)
4. [Factory com Customizer (Function\<Builder, Builder\>)](#4-factory-com-customizer-functionbuilder-builder)
5. [Strategy Pattern — Handlers de Status](#5-strategy-pattern--handlers-de-status)
6. [Convenções de Nomenclatura](#6-convenções-de-nomenclatura)
7. [Regras Gerais de Código](#7-regras-gerais-de-código)
8. [Como Adicionar Novos Padrões](#8-como-adicionar-novos-padrões)

---

## 1. Estrutura de Pacotes

```
core-service/
└── com.payflow.coreservice/
    ├── builder/              ← conversores estáticos entre objetos de domínio
    ├── config/               ← configurações Spring (Kafka, Security, etc.)
    ├── consumer/             ← listeners Kafka
    ├── controller/           ← endpoints REST
    ├── client/               ← Feign clients (chamadas externas)
    ├── dto/                  ← DTOs locais do core-service
    │   └── factory/          ← factories que produzem DTOs de resposta
    ├── enums/                ← enums locais do serviço
    ├── exception/            ← exceções de negócio + GlobalExceptionHandler
    ├── logging/              ← filtros de log/tracing
    ├── model/                ← entidades JPA
    │   └── factory/          ← factories que produzem entidades
    ├── repository/           ← interfaces JPA
    ├── security/             ← filtros e configurações de segurança
    ├── services/             ← lógica de negócio
    └── strategy/
        ├── PaymentStatusHandler.java   ← interface do strategy
        ├── factory/                    ← factory que seleciona o handler
        └── handlers/                   ← implementações do strategy
```

**Regra:** cada pacote tem responsabilidade única. Nunca colocar lógica de negócio em `controller` ou `builder`.

---

## 2. Builder Pattern — Conversores Estáticos

### O que é

Neste projeto, "Builder" **não** é o GoF Builder clássico. É uma **classe utilitária estática** responsável por converter um objeto de domínio em outro, encapsulando o mapeamento de campos e centralizando a lógica de construção.

### Estrutura padrão

```java
public class XyzBuilder {

    // Construtor privado — nunca instanciar
    private XyzBuilder() {}

    public static TargetObject fromSourceObject(SourceObject source) {
        return TargetObject.builder()
                .field1(source.getField1())
                .field2(source.getField2())
                .build();
    }
}
```

### Builders existentes

| Classe | Entrada | Saída | Onde é usado |
|---|---|---|---|
| `AnalysisRequestBuilder` | `Payment` | `FraudAnalysisRequest` | `PaymentService` antes de chamar o fraud-service |
| `PaymentAlertEventBuilder` | `Payment` + `FraudAnalysisResponse` | `PaymentAlertEvent` | Handlers de status (Kafka) |
| `StatusHistoryBuilder` | `UUID` + `Enum_Payment` + `ManualReviewDecision` | `StatusHistory` | `ManualReviewService` |
| `DecisionBuilder` | `ManualReviewDecision` | `FraudAnalysisResponse` | `ManualReviewService` ao reprocessar decisão |
| `PaymentDetailsBuilder` | `Payment` | `PaymentDetailsRequest` | `ManualReviewService` para endpoints de revisão |

### Exemplo real — `AnalysisRequestBuilder`

```java
// Antes (acoplado, difícil de testar)
FraudAnalysisRequest request = new FraudAnalysisRequest();
request.setPaymentId(payment.getUuid());
request.setPayerId(payment.getPayerId());
// ...

// Depois (limpo, testável, centralizado)
FraudAnalysisRequest request = AnalysisRequestBuilder.fromAnalysisRequest(payment);
```

### Exemplo real — `StatusHistoryBuilder`

```java
// Converte uma decisão manual em entidade de histórico
StatusHistory history = StatusHistoryBuilder.fromManualReview(
    paymentId,
    payment.getStatus(),   // oldStatus
    decision               // ManualReviewDecision com reviewerId, reason, etc.
);
```

Internamente o builder:
1. Deriva o `newStatus` a partir de `decision.getDecision()` (`"APPROVED"` → `Enum_Payment.APPROVED`)
2. Define `source = "MANUAL_REVIEW"` automaticamente
3. Registra o `timestamp` com `LocalDateTime.now()`

### Quando criar um novo Builder

Crie um Builder quando:
- Você está construindo um objeto a partir de **dois ou mais** objetos de domínio diferentes
- A construção envolve alguma **derivação de campo** (ex: status derivado de string, timestamp automático)
- O mesmo mapeamento é feito em **mais de um lugar**

Não crie um Builder para mapeamentos triviais de um campo só.

---

## 3. Factory Pattern — Criação de Objetos

### O que é

Factories criam **instâncias novas** de entidades ou DTOs a partir de inputs externos (requests, DTOs de entrada). Diferente dos Builders que convertem entre objetos existentes, as Factories são o ponto de entrada de novos dados no domínio.

### Factories existentes

| Classe | Pacote | Responsabilidade |
|---|---|---|
| `PaymentFactory` | `model/factory` | Cria `Payment` a partir de `PaymentRequest` |
| `UserFactory` | `model/factory` | Cria `User` a partir de `RegisterRequestDTO` ou `UserRequest` |
| `PaymentResponseFactory` | `dto/factory` | Converte `Payment` → `PaymentResponse` (DTO de saída) |
| `AuthResponseFactory` | `dto/factory` | Monta `AuthResponseDTO` a partir de `User` + `token` |

### Estrutura padrão

```java
public final class XyzFactory {

    private XyzFactory() {} // utilitária — não instanciar

    public static Entity fromRequest(RequestDTO request) {
        return Entity.builder()
                .field(request.getField())
                .status(DefaultStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
```

### Exemplo real — `PaymentResponseFactory`

```java
// No controller ou service, nunca montar o DTO na mão:
return PaymentResponseFactory.fromPayment(payment);

// Não faça isso espalhado pelo código:
return PaymentResponse.builder()
    .id(payment.getUuid())
    .payerId(payment.getPayerId())
    // ... repetido em 3 lugares diferentes
    .build();
```

### Regra de separação

| Tipo | Pacote | Quando usar |
|---|---|---|
| `model/factory` | Cria **entidades JPA** (Payment, User) | Dados chegando do mundo externo (request HTTP) |
| `dto/factory` | Cria **DTOs de resposta** | Dados saindo do domínio para o cliente |
| `builder/` | **Converte** entre objetos do domínio | Dados fluindo internamente entre serviços/camadas |

---

## 4. Factory com Customizer `Function<Builder, Builder>`

### O que é

Um padrão avançado usado em `PaymentFactory` e `UserFactory` que permite **estender a construção** sem criar novos métodos de factory.

### Como funciona

```java
public static Payment fromRequest(
    PaymentRequest request,
    Enum_Payment status,
    Function<Payment.PaymentBuilder, Payment.PaymentBuilder> customizer
) {
    Payment.PaymentBuilder builder = Payment.builder()
            .payerId(request.getPayerId())
            .payeeId(request.getPayeeId())
            .amount(request.getAmount())
            .status(status)
            .idempotencyKey(request.getIdempotencyKey());

    if (customizer != null) {
        builder = customizer.apply(builder); // aplica personalização opcional
    }

    return builder.build();
}

// Atalho sem customizer
public static Payment fromRequest(PaymentRequest request, Enum_Payment status) {
    return fromRequest(request, status, null);
}
```

### Uso com customizer

```java
// Uso padrão (sem customizer)
Payment payment = PaymentFactory.fromRequest(request, Enum_Payment.PENDING);

// Uso com customizer — adiciona campo extra sem criar novo método de factory
Payment payment = PaymentFactory.fromRequest(
    request,
    Enum_Payment.PENDING,
    builder -> builder.someExtraField("valor")
);
```

### Quando usar este padrão

- Quando a factory tem um fluxo padrão mas **casos específicos** precisam de campos extras
- Quando você quer **evitar explosão de métodos** na factory (ex: `fromRequestWithX`, `fromRequestWithY`, `fromRequestWithXAndY`)
- Quando o campo extra é opcional e não deve ser parte do contrato padrão

---

## 5. Strategy Pattern — Handlers de Status

### O que é

Após receber o resultado da análise antifraude, o `PaymentService` seleciona dinamicamente o comportamento correto via `PaymentStatusHandlerFactory`. Cada `Status_Fraud` tem seu próprio handler com lógica isolada.

### Estrutura

```
PaymentStatusHandler (interface)
    └── handle(Payment payment, FraudAnalysisResponse response)

PaymentStatusHandlerFactory
    └── getHandler(Status_Fraud status) → PaymentStatusHandler

Handlers:
    ├── ApprovedHandler        → debita/credita → SUCCESS
    ├── RejectedHandler        → FAILED
    ├── ManualAnalysisHandler  → PENDING + Kafka alert
    ├── PendingReviewHandler   → PENDING + Kafka alert
    └── SuspiciousHandler      → PENDING + Kafka alert crítico
```

### Interface

```java
public interface PaymentStatusHandler {
    void handle(Payment payment, FraudAnalysisResponse response);
}
```

### Uso no service

```java
// PaymentService — sem if/else
PaymentStatusHandler handler = factory.getHandler(fraudResponse.getStatus());
handler.handle(payment, fraudResponse);
```

### Como adicionar um novo status

1. Crie `NovoStatusHandler.java` em `strategy/handlers/`:

```java
@Component
@RequiredArgsConstructor
public class NovoStatusHandler implements PaymentStatusHandler {

    @Override
    public void handle(Payment payment, FraudAnalysisResponse response) {
        // lógica específica
    }
}
```

2. Registre no `PaymentStatusHandlerFactory`:

```java
@PostConstruct
private void init() {
    handlers.put(Status_Fraud.NOVO_STATUS, novoStatusHandler);
}
```

3. Adicione o valor `NOVO_STATUS` no enum `Status_Fraud` em `commons`.

**Não modifique** o `PaymentService` nem os outros handlers — o padrão isola completamente cada comportamento.

---

## 6. Convenções de Nomenclatura

### Classes

| Tipo | Sufixo | Exemplo |
|---|---|---|
| Conversor estático | `Builder` | `PaymentAlertEventBuilder` |
| Criador de entidade | `Factory` | `PaymentFactory` |
| Criador de DTO | `Factory` | `PaymentResponseFactory` |
| Implementação de strategy | `Handler` | `ApprovedHandler` |
| Seletor de strategy | `HandlerFactory` | `PaymentStatusHandlerFactory` |
| Interface de strategy | sem sufixo | `PaymentStatusHandler` |
| DTO de entrada (request) | `Request` ou `DTO` | `PaymentRequest`, `RegisterRequestDTO` |
| DTO de saída (response) | `Response` ou `DTO` | `PaymentResponse`, `AuthResponseDTO` |

### Métodos de Builder/Factory

| Padrão | Uso | Exemplo |
|---|---|---|
| `fromXxx(...)` | Converte/cria a partir de `Xxx` | `fromRequest(request)` |
| `fromXxxAndYyy(...)` | Combina dois objetos | `fromUserAndToken(user, token, expiry)` |
| `fromXxx(Xxx, Function<Builder, Builder>)` | Factory extensível | `fromRequest(request, status, customizer)` |

### Campos de status

Sempre use **enums**, nunca strings mágicas:

```java
// ✅ Correto
payment.setStatus(Enum_Payment.PENDING);
history.setSource("MANUAL_REVIEW"); // exceção: source é string controlada pelo builder

// ❌ Errado
payment.setStatus("pending");
if (decision.equals("APPROVED")) { ... } // use enum
```

---

## 7. Regras Gerais de Código

### Injeção de dependência

Prefira **injeção via construtor** com `@RequiredArgsConstructor` (Lombok):

```java
// ✅ Preferido
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final AntiFraudClient antiFraudClient;
}

// ⚠️ Evitar (dificulta testes)
@Service
public class PaymentService {
    @Autowired
    private PaymentRepository paymentRepository;
}
```

### Nunca instanciar Builders e Factories

São classes utilitárias. Construtor deve ser `private`:

```java
public final class PaymentFactory {
    private PaymentFactory() {} // garante que ninguém instancia
}
```

### Transações

- Use `@Transactional` nos **services**, nunca nos controllers
- Para operações que devem ter transação própria independente da transação pai: `@Transactional(propagation = Propagation.REQUIRES_NEW)`
- Exemplo: persistência inicial do `Payment` antes de chamar o fraud-service usa `REQUIRES_NEW` para garantir que o pagamento seja salvo mesmo se o antifraude falhar

### Separação de responsabilidades

| Camada | Pode fazer | Não pode |
|---|---|---|
| `Controller` | Receber request, delegar ao service, retornar response | Lógica de negócio, acesso direto ao repositório |
| `Service` | Orquestrar, validar, chamar repositórios e clients | Construção manual de DTOs (use factory) |
| `Builder/Factory` | Construir objetos | Chamar repositórios, lançar exceções de negócio |
| `Handler` | Executar ação específica de um status | Decidir qual handler usar (responsabilidade da factory) |

### Tratamento de erros

- Exceções de negócio devem ser **classes próprias** em `exception/`
- Nunca retornar `null` — prefira lançar exceção ou retornar `Optional`
- O `GlobalExceptionHandler` centraliza todos os tratamentos — não trate exceções no controller

### Kafka

- Nunca publicar eventos diretamente no service — use os Handlers
- Todo evento publicado deve ter um Builder correspondente em `builder/`
- Exemplo: `PaymentAlertEventBuilder.fromAlertEvento(payment, response)` — nunca construir o evento inline no handler

---

## 8. Como Adicionar Novos Padrões

### Novo endpoint com fluxo completo

1. **DTO de request** em `commons/dto/` (se compartilhado) ou `dto/` (se local)
2. **Factory** em `dto/factory/` para o DTO de response
3. **Builder** em `builder/` se a construção envolver múltiplos objetos
4. **Service** com a lógica orquestrada
5. **Controller** delegando ao service
6. **Testes** unitários do service e do builder/factory

### Novo tipo de alerta Kafka

1. Adicione o valor ao enum ou crie string constante
2. Crie (ou reutilize) um Builder em `builder/` para o evento
3. Publique no Handler correspondente
4. Trate no `AlertConsumerService`

### Novo handler de status de fraude

Veja [Seção 5 — Como adicionar um novo status](#5-strategy-pattern--handlers-de-status).

---

## Referência Rápida

```
Novo objeto a partir de request HTTP     → Factory (model/factory ou dto/factory)
Converter objeto A em objeto B           → Builder (builder/)
Comportamento variável por status/tipo   → Strategy (strategy/handlers/)
Selecionar qual handler usar             → Factory (strategy/factory/)
Construção flexível com campo opcional   → Factory com Function<Builder, Builder>
```
