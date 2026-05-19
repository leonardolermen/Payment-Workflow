# PayFlow - Plano Detalhado de Tarefas (Tasks de Implementação)

Este documento centraliza todas as etapas técnicas do ecossistema PayFlow com status real extraído do código, dívidas técnicas identificadas e ideias de melhoria.

**Legenda:** ? Feito · ?? Parcial/Divergência · ?? Bug/Dívida técnica · [ ] Pendente · ?? Ideia nova

---

## ?? Estrutura de Documentação

A documentação foi dividida em arquivos separados para melhor organização:

### ?? [Bugs Críticos](./bugs-criticos.md)
Bugs técnicos que precisam ser corrigidos com prioridade alta.
- Entidade Transaction incompleta
- MANUAL_ANALYSIS não tratado no PaymentService
- Concorrência no saldo (double-spend risk)

### ? [Implementações Completas](./implementacoes-completas.md)
Todas as funcionalidades já implementadas no ecossistema PayFlow.
- Core Service (entidades, repositórios, services, controllers, Kafka consumers)
- Fraud Service (motor de regras, cache de histórico, controllers)
- Commons Module (DTOs e Enums)
- Segurança e Gateway

### [ ] [Melhorias Pendentes](./melhorias-pendentes.md)
Melhorias que fazem sentido implementar (prioridade média).
- Validação de DTOs
- Idempotência com Redis
- Refresh Token
- Roles e autorização granular
- Timeout em transações
- Circuit Breaker
- Propagar traceId

### [ ] [Funcionalidades Futuras](./funcionalidades-futuras.md)
Funcionalidades planejadas para implementação futura (prioridade baixa).
- Kafka Event-Driven Architecture completa
- Notificações (webhook, email, DLQ)
- Testes (unitários, integração)
- Documentação de API (OpenAPI)
- Observabilidade (Actuator, Prometheus, Grafana)
- Listagens com paginação

### ?? [Ideias Opcionais](./ideias-opcionais.md)
Ideias que podem ser consideradas para melhorias futuras.
- Webhook personalizado
- Outbox Pattern
- Evolução para Drools/IA
- Rate limiting
- Auditoria completa
- Feature flags
- Integração com provedores reais

---

## ?? Resumo

| Categoria | Status | Arquivo |
|-----------|--------|---------|
| Bugs Críticos | ?? 3 bugs | [bugs-criticos.md](./bugs-criticos.md) |
| Implementações | ? Completo | [implementacoes-completas.md](./implementacoes-completas.md) |
| Melhorias | [ ] 7 itens | [melhorias-pendentes.md](./melhorias-pendentes.md) |
| Funcionalidades Futuras | [ ] 6 áreas | [funcionalidades-futuras.md](./funcionalidades-futuras.md) |
| Ideias | ?? 7 ideias | [ideias-opcionais.md](./ideias-opcionais.md) |


## ????? Tasks Específicas por Desenvolvedor

### [Manual Decision Refactor - Dev Jorge](./09_manual_decision_refactor.md)
Mover completamente a lógica de decisões manuais de pagamentos do core-service para o fraud-service, utilizando Kafka para comunicação assíncrona.

- Remover ManualReviewController e ManualReviewService do core-service
- Centralizar toda lógica de decisão manual no fraud-service
- Criar tópico Kafka payflow.manual.decision
- Implementar producer no fraud-service e consumer no core-service
- Fluxo: Fraud Service ? Kafka ? Core Service

### [User KYC Flow - Dev Higao](./10_user_kyc_flow.md)
Implementar fluxo de KYC (Know Your Customer) para novos usuários, criando-os com status EM_ANALISE e validando via fraud-service.

- Criar usuário com status EM_ANALISE ao registrar
- Enviar evento via Kafka para fraud-service
- Implementar regras básicas de KYC (email temporário, documento inválido, etc.)
- Receber resultado e atualizar status (ACTIVE/BLOCKED)
- Criar tópicos Kafka payflow.user.created e payflow.user.kyc.result

---

## ?? Resumo

| Categoria | Status | Arquivo |
|-----------|--------|---------|
| Bugs Críticos | ?? 3 bugs | [bugs-criticos.md](./bugs-criticos.md) |
| Implementações | ? Completo | [implementacoes-completas.md](./implementacoes-completas.md) |
| Melhorias | [ ] 7 itens | [melhorias-pendentes.md](./melhorias-pendentes.md) |
| Funcionalidades Futuras | [ ] 6 áreas | [funcionalidades-futuras.md](./funcionalidades-futuras.md) |
| Ideias | ?? 7 ideias | [ideias-opcionais.md](./ideias-opcionais.md) |
| Tasks Específicas | [ ] 2 tasks | [09_manual_decision_refactor.md](./09_manual_decision_refactor.md), [10_user_kyc_flow.md](./10_user_kyc_flow.md) |
