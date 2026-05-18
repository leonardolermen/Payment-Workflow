# PayFlow — Padrões de Projeto: Índice

> Referência para devs e IAs. Todo o código desta seção é baseado no código real do projeto.

---

## Arquivos desta seção

| Arquivo | Conteúdo |
|---|---|
| `01_package_structure.md` | Estrutura de pacotes e responsabilidades |
| `02_builder.md` | Builder Pattern — conversores estáticos entre objetos |
| `03_factory.md` | Factory Pattern — criação de entidades e DTOs |
| `04_factory_customizer.md` | Factory com `Function<Builder, Builder>` — pattern avançado |
| `05_strategy.md` | Strategy Pattern — handlers de status de pagamento |
| `06_conventions.md` | Nomenclatura, regras de código e boas práticas gerais |
| `07_how_to_extend.md` | Como adicionar novos padrões ao projeto |

---

## Referência Rápida

```
Novo objeto a partir de request HTTP     → Factory  (model/factory ou dto/factory)
Converter objeto A em objeto B           → Builder  (builder/)
Comportamento variável por status/tipo   → Strategy (strategy/handlers/)
Selecionar qual handler usar             → Factory  (strategy/factory/)
Construção flexível com campo opcional   → Factory com Function<Builder, Builder>
```

---

## Padrões em uso

| Padrão | Onde vive | Classe(s) chave |
|---|---|---|
| Builder estático | `builder/` | `AnalysisRequestBuilder`, `StatusHistoryBuilder`, `DecisionBuilder`, `PaymentAlertEventBuilder`, `PaymentDetailsBuilder` |
| Factory de entidade | `model/factory/` | `PaymentFactory`, `UserFactory` |
| Factory de DTO | `dto/factory/` | `PaymentResponseFactory`, `AuthResponseFactory` |
| Factory com customizer | `model/factory/` | `PaymentFactory.fromRequest(..., customizer)` |
| Strategy | `strategy/handlers/` | `ApprovedHandler`, `RejectedHandler`, `ManualAnalysisHandler`, `PendingReviewHandler`, `SuspiciousHandler` |
| Strategy Factory | `strategy/factory/` | `PaymentStatusHandlerFactory` |
