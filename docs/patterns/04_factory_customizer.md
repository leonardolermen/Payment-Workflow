# PayFlow — Factory com Customizer `Function<Builder, Builder>`

## O que é

Padrão avançado implementado em `PaymentFactory` e `UserFactory`. Permite **estender a construção** via lambda sem criar novos métodos de factory para cada variação.

---

## Como funciona

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
        builder = customizer.apply(builder);
    }

    return builder.build();
}

// Sobrecarga sem customizer — atalho para o caso padrão
public static Payment fromRequest(PaymentRequest request, Enum_Payment status) {
    return fromRequest(request, status, null);
}
```

---

## Uso

```java
// Caso padrão — sem customizer
Payment payment = PaymentFactory.fromRequest(request, Enum_Payment.PENDING);

// Caso específico — adiciona campo extra via lambda
Payment payment = PaymentFactory.fromRequest(
    request,
    Enum_Payment.PENDING,
    builder -> builder.someExtraField("valor")
);
```

---

## Por que usar

Sem este padrão, cada variação exigiria um método novo:

```java
// ❌ Explosão de métodos — evitar
fromRequest(request, status)
fromRequestWithExtraFieldA(request, status, fieldA)
fromRequestWithExtraFieldB(request, status, fieldB)
fromRequestWithAandB(request, status, fieldA, fieldB)
```

Com o customizer:

```java
// ✅ Um único método extensível
fromRequest(request, status, b -> b.fieldA(a).fieldB(b))
```

---

## Quando usar

| Situação | Usar customizer? |
|---|---|
| Fluxo padrão sem variação | ❌ Use a sobrecarga sem customizer |
| Campo extra opcional em casos específicos | ✅ Sim |
| Campos obrigatórios em todos os casos | ❌ Adicione no método base |
| Mais de 2 campos extras recorrentes | ❌ Crie um novo método nomeado |

---

## Regra

O customizer nunca deve conter lógica de negócio. Apenas atribuição de campos:

```java
// ✅ Correto
builder -> builder.observacao("auditoria interna")

// ❌ Errado — lógica não pertence aqui
builder -> {
    if (algo) builder.status(X);
    return builder;
}
```
