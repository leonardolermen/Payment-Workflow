# PayFlow — Convenções e Boas Práticas

---

## Nomenclatura de classes

| Tipo | Sufixo | Exemplo |
|---|---|---|
| Conversor estático | `Builder` | `PaymentAlertEventBuilder` |
| Criador de entidade JPA | `Factory` | `PaymentFactory` |
| Criador de DTO de response | `Factory` | `PaymentResponseFactory` |
| Implementação de strategy | `Handler` | `ApprovedHandler` |
| Seletor de strategy | `HandlerFactory` | `PaymentStatusHandlerFactory` |
| Interface de strategy | sem sufixo | `PaymentStatusHandler` |
| DTO de entrada | `Request` ou `DTO` | `PaymentRequest`, `RegisterRequestDTO` |
| DTO de saída | `Response` ou `DTO` | `PaymentResponse`, `AuthResponseDTO` |

---

## Nomenclatura de métodos

| Padrão | Quando usar | Exemplo |
|---|---|---|
| `fromXxx(Xxx)` | Cria/converte a partir de um objeto | `fromRequest(request)` |
| `fromXxxAndYyy(Xxx, Yyy)` | Combina dois objetos de domínio | `fromUserAndToken(user, token, expiry)` |
| `fromXxx(Xxx, Status, customizer)` | Factory extensível com lambda | `fromRequest(request, status, b -> b.campo(x))` |

---

## Injeção de dependência

Sempre use **construtor** com `@RequiredArgsConstructor`:

```java
// ✅ Preferido — imutável, testável, explícito
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final AntiFraudClient antiFraudClient;
}

// ⚠️ Evitar — dificulta testes unitários, dependências ocultas
@Service
public class PaymentService {
    @Autowired
    private PaymentRepository paymentRepository;
}
```

---

## Classes utilitárias (Builder/Factory)

Sempre `final` com construtor `private`:

```java
public final class PaymentFactory {
    private PaymentFactory() {}
    // ...
}
```

---

## Enums vs Strings mágicas

Sempre use enums para status e tipos:

```java
// ✅ Correto
payment.setStatus(Enum_Payment.PENDING);
user.setStatus(User_Status.ACTIVE);

// ❌ Errado — strings mágicas são frágeis
payment.setStatus("pending");
if (status.equals("APPROVED")) { ... }
```

**Exceção controlada:** o campo `source` em `StatusHistory` é uma string, mas seu valor é sempre definido dentro do `StatusHistoryBuilder`, nunca inline no service.

---

## Transações

```java
// Use @Transactional nos services, nunca nos controllers
@Transactional
public PaymentResponse createPayment(PaymentRequest request) { ... }

// Para operação com transação própria (independente da transação pai)
@Transactional(propagation = Propagation.REQUIRES_NEW)
public Payment saveInitialPayment(Payment payment) { ... }
```

> A persistência inicial do `Payment` usa `REQUIRES_NEW` para garantir que o registro exista no banco antes de chamar o fraud-service via Feign.

---

## Tratamento de erros

```java
// ✅ Exceção de negócio — classe própria em exception/
throw new UserNotFoundException("Usuário não encontrado: " + id);

// ✅ Nunca retornar null — use Optional ou lance exceção
return userRepository.findByUuid(id)
    .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

// ❌ Não trate exceções no controller — o GlobalExceptionHandler cuida disso
```

---

## Kafka — regras

```java
// ✅ Eventos Kafka sempre construídos via Builder
PaymentAlertEvent event = PaymentAlertEventBuilder.fromAlertEvento(payment, response);
kafkaTemplate.send(TOPIC, event);

// ❌ Nunca construir evento inline no handler
kafkaTemplate.send(TOPIC, PaymentAlertEvent.builder()
    .paymentId(...)  // acoplado, difícil de testar
    .build());

// ✅ Nunca publicar no service — sempre no Handler
// Service → chama factory.getHandler(status).handle(payment, response)
// Handler → publica o evento
```

---

## Separação de responsabilidades — resumo

| O que preciso fazer | Onde fica |
|---|---|
| Receber e validar request HTTP | `controller/` |
| Orquestrar fluxo de negócio | `services/` |
| Criar entidade a partir de request | `model/factory/` |
| Criar DTO de response | `dto/factory/` |
| Converter objeto A em objeto B | `builder/` |
| Comportamento específico por status | `strategy/handlers/` |
| Definir qual handler executar | `strategy/factory/` |
| Validar campos do request | Bean Validation (`@Valid` + `@NotBlank`, etc.) |
| Tratar exceções globalmente | `exception/GlobalExceptionHandler` |
