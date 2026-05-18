# PayFlow — Estrutura de Pacotes

## core-service

```
com.payflow.coreservice/
├── builder/              ← conversores estáticos entre objetos de domínio
├── client/               ← Feign clients (chamadas para serviços externos)
├── config/               ← configurações Spring (Kafka, Security, etc.)
├── consumer/             ← listeners Kafka (@KafkaListener)
├── controller/           ← endpoints REST (@RestController)
├── dto/                  ← DTOs locais do core-service
│   └── factory/          ← factories que produzem DTOs de resposta
├── enums/                ← enums locais do serviço (Document_Type, etc.)
├── exception/            ← exceções de negócio + GlobalExceptionHandler
├── logging/              ← filtros de log e tracing (MDC)
├── model/                ← entidades JPA (@Entity)
│   └── factory/          ← factories que produzem entidades a partir de requests
├── repository/           ← interfaces JPA (extends JpaRepository)
├── security/             ← filtros e configs de segurança (JWT, InternalApiKey)
├── services/             ← lógica de negócio (@Service)
└── strategy/
    ├── PaymentStatusHandler.java   ← interface do strategy
    ├── factory/                    ← factory que seleciona o handler correto
    └── handlers/                   ← uma implementação por status de fraude
```

## fraud-service

```
com.payflow.fraudservice/
├── client/               ← Feign client para o core-service
├── config/               ← configurações Spring
├── controller/           ← endpoints REST
├── model/                ← entidade FraudAnalysisLog
├── Repository/           ← interface JPA
└── service/              ← FraudAnalysisService (lógica de scoring)
```

## commons

```
com.payflow.commons/
├── dto/
│   ├── alert/            ← PaymentAlertEvent, ManualReviewDecision
│   ├── fraud/            ← FraudAnalysisRequest, FraudAnalysisResponse
│   ├── payment/          ← PaymentRequest, PaymentResponse
│   └── user/             ← UserRequest, UserResponse
└── enums/
    ├── fraud/            ← Status_Fraud
    ├── payment/          ← Enum_Payment
    └── user/             ← User_Status
```

---

## Regras por camada

| Camada | Pode fazer | Não pode fazer |
|---|---|---|
| `controller/` | Receber request, validar com `@Valid`, delegar ao service | Lógica de negócio, acesso direto ao repositório |
| `services/` | Orquestrar, validar regras, chamar repos e clients | Construir DTOs manualmente (use factory/builder) |
| `builder/` | Converter objetos de domínio entre si | Chamar repositórios, lançar exceções de negócio |
| `model/factory/` | Criar entidades a partir de requests | Chamar repositórios, aplicar regras de negócio |
| `dto/factory/` | Criar DTOs de response a partir de entidades | Qualquer lógica além de mapeamento de campos |
| `strategy/handlers/` | Executar ação específica de um status | Decidir qual handler usar (papel da factory) |

---

## Onde colocar cada coisa

**Novo DTO de entrada (request)?**
- Compartilhado entre serviços → `commons/dto/`
- Exclusivo do core-service → `core-service/dto/`

**Novo DTO de saída (response)?**
- Criar DTO em `dto/` + factory em `dto/factory/`

**Nova entidade JPA?**
- Entidade em `model/` + factory em `model/factory/` + repository em `repository/`

**Nova conversão entre objetos internos?**
- Builder em `builder/`

**Novo comportamento por status?**
- Handler em `strategy/handlers/` + registro em `PaymentStatusHandlerFactory`
