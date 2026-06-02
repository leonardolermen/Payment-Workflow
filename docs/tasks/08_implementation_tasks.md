# PayFlow - Plano Detalhado de Tarefas (Tasks de Implementação)

Este documento centraliza todas as etapas técnicas do ecossistema PayFlow com status real extraído do código, dívidas técnicas identificadas e ideias de melhoria.

**Legenda:** ? Feito · ?? Parcial/Divergência · ?? Bug/Dívida técnica · [ ] Pendente · ?? Ideia nova

---

## ?? Estrutura de Documentação

A documentação foi dividida em arquivos separados para melhor organização:

### ?? [Bugs Críticos](./bugs-criticos.md)
Bugs técnicos que precisam ser corrigidos com prioridade alta.
- Entidade Transaction incompleta (1 bug ativo)
- MANUAL_ANALYSIS não tratado no PaymentService (✅ CORRIGIDO)
- Concorrência no saldo (double-spend risk) (✅ CORRIGIDO)

### ? [Implementações Completas](./implementacoes-completas.md)
Todas as funcionalidades já implementadas no ecossistema PayFlow.
- Core Service (entidades, repositórios, services, controllers, Kafka consumers)
- Fraud Service (motor de regras, cache de histórico, controllers)
- Commons Module (DTOs e Enums)
- Segurança e Gateway
- Kafka Infrastructure (9 tópicos configurados)
- Redis Integration (idempotência implementada)
- Observabilidade (Actuator, OpenTelemetry)
- Controllers adicionais (HistoryController, ManualAnalyzeController)

### ⚠️ [Melhorias Pendentes](./melhorias-pendentes.md)
Melhorias que fazem sentido implementar (prioridade média).
- Validação de DTOs (⚠️ Parcial)
- Idempotência com Redis (✅ Completo)
- Refresh Token (❌ Não implementado)
- Roles e autorização granular (⚠️ Parcial)
- Timeout em transações (❌ Não implementado)
- Circuit Breaker (❌ Não implementado)
- Propagar traceId (✅ Completo)

### ⚠️ [Funcionalidades Futuras](./funcionalidades-futuras.md)
Funcionalidades planejadas para implementação futura (prioridade baixa).
- Kafka Event-Driven Architecture (⚠️ Parcial - 8 tópicos configurados)
- Notificações (⚠️ Parcial - DLQ e retry implementados)
- Testes (❌ Não implementado)
- Documentação de API (❌ Não implementado)
- Observabilidade (⚠️ Parcial - dependências adicionadas)
- Listagens com paginação (⚠️ Parcial - endpoints sem Pageable)

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
| Bugs Críticos | ?? 1 bug ativo (2 corrigidos) | [bugs-criticos.md](./bugs-criticos.md) |
| Implementações | ? Completo + novas implementações | [implementacoes-completas.md](./implementacoes-completas.md) |
| Melhorias | ⚠️ 3 implementadas, 4 pendentes | [melhorias-pendentes.md](./melhorias-pendentes.md) |
| Funcionalidades Futuras | ⚠️ 3 parciais, 3 pendentes | [funcionalidades-futuras.md](./funcionalidades-futuras.md) |
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
| Bugs Críticos | ?? 1 bug ativo (2 corrigidos) | [bugs-criticos.md](./bugs-criticos.md) |
| Implementações | ? Completo + novas implementações | [implementacoes-completas.md](./implementacoes-completas.md) |
| Melhorias | ⚠️ 3 implementadas, 4 pendentes | [melhorias-pendentes.md](./melhorias-pendentes.md) |
| Funcionalidades Futuras | ⚠️ 3 parciais, 3 pendentes | [funcionalidades-futuras.md](./funcionalidades-futuras.md) |
| Ideias | ?? 7 ideias | [ideias-opcionais.md](./ideias-opcionais.md) |
| Tasks Específicas | ⚠️ 1 parcial, 1 não iniciado | [09_manual_decision_refactor.md](./09_manual_decision_refactor.md), [10_user_kyc_flow.md](./10_user_kyc_flow.md) |
