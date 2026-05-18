# PayFlow — Factory Pattern

## O que é

Factories criam **instâncias novas** de entidades ou DTOs a partir de inputs externos (requests HTTP, DTOs de entrada). São o ponto de entrada de dados novos no domínio.

> Factory = cria objetos novos a partir de inputs externos  
> Builder = converte entre objetos internos já existentes

---

## Factories existentes

| Classe | Pacote | Entrada | Saída |
|---|---|---|---|
| `PaymentFactory` | `model/factory` | `PaymentRequest` + `Enum_Payment` | `Payment` (entidade) |
| `UserFactory` | `model/factory` | `RegisterRequestDTO` ou `UserRequest` | `User` (entidade) |
| `PaymentResponseFactory` | `dto/factory` | `Payment` | `PaymentResponse` (DTO) |
| `AuthResponseFactory` | `dto/factory` | `User` + `token` + `expiresIn` | `AuthResponseDTO` |

---

## Estrutura padrão

```java
public final class XyzFactory {

    private XyzFactory() {} // utilitária — nunca instanciar

    public static Entity fromRequest(RequestDTO request) {
        return Entity.builder()
                .field(request.getField())
                .status(User_Status.ACTIVE)   // default definido aqui
                .createdAt(LocalDateTime.now()) // timestamp gerenciado aqui
                .build();
    }
}
```

---

## Exemplos reais

### `PaymentResponseFactory` — Payment → PaymentResponse

```java
// ✅ Sempre use a factory para montar o DTO de response
return PaymentResponseFactory.fromPayment(payment);

// ❌ Nunca monte o DTO inline no service ou controller
return PaymentResponse.builder()
    .id(payment.getUuid())
    .payerId(payment.getPayerId())
    // ... 6 campos — repetido em cada lugar que precisa
    .build();
```

---

### `UserFactory` — RegisterRequestDTO → User

A factory define defaults de negócio que o request não carrega:

```java
public static User fromRegisterRequest(RegisterRequestDTO request, String encodedPassword) {
    return User.builder()
            .name(request.getName())
            .email(request.getEmail())
            .password(encodedPassword)             // já encodado externamente
            .document(request.getDocument())
            .documentType(request.getDocumentType().name())
            .balance(request.getBalance() != null
                     ? request.getBalance()
                     : BigDecimal.ZERO)            // default: zero
            .status(User_Status.ACTIVE)            // sempre ACTIVE no registro
            .createdAt(LocalDateTime.now())        // timestamp controlado aqui
            .build();
}
```

---

### `AuthResponseFactory` — User + token → AuthResponseDTO

Combina dados de fontes diferentes, sem ser um builder pois o token é externo ao domínio:

```java
public static AuthResponseDTO fromUserAndToken(User user, String token, long expiresInSeconds) {
    return new AuthResponseDTO(
            token,
            "Bearer",                                        // tipo fixo
            user.getUuid(),
            user.getName(),
            user.getEmail(),
            LocalDateTime.now().plusSeconds(expiresInSeconds) // calcula expiração
    );
}
```

---

## Regra de separação entre pacotes

| Pacote | Produz | Quando usar |
|---|---|---|
| `model/factory/` | Entidades JPA (`@Entity`) | Dados chegando do cliente (request HTTP) |
| `dto/factory/` | DTOs de resposta | Dados saindo do domínio para o cliente |
| `builder/` | Qualquer objeto | Dados fluindo entre camadas/serviços internos |

---

## Regras

- Construtor **sempre `private`** — `final class` + `private constructor`
- Nenhuma lógica de negócio além de mapeamento e defaults
- Nenhum acesso a repositório ou serviço
- Defaults de domínio (status inicial, timestamps) vivem **na factory**, não no service
