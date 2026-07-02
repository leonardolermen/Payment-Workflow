# Manual Decision Refactor - Dev Jorge

## Status: ✅ QUASE COMPLETO

## Progresso Atual

### ✅ Implementado
- [x] **Tópico Kafka payflow.manual.decision** criado em KafkaTopicsConfig
- [x] **DTO ManualReviewDecision** criado no módulo commons
- [x] **Producer FraudEventProducer** criado no fraud-service
- [x] **ManualReviewService** criado no fraud-service com processPaymentDecision
- [x] **ManualAnalyzeController** no fraud-service com endpoint PUT /manual-analyze/payment/{paymentId}
- [x] **Consumer ManualDecisionConsumerService** criado no core-service
- [x] **Métodos no PaymentService**: approveManualPayment e rejectManualPayment implementados
- [x] **Strategy Pattern** para tratamento de status (PaymentStatusHandlerFactory)
- [x] **DLQ configurada** para payflow.manual.decision.dlq
- [x] **ManualReviewController removido do Core Service** (não existe mais)
- [x] **ManualReviewService removido do Core Service** (não existe mais)
- [x] **Webhook notifications** implementadas para aprovação/rejeição manual
- [x] **Status PENDING_REVIEW** adicionado ao Enum_Payment (commons)
- [x] **Handlers atualizados** (PendingReviewHandler e ManualAnalysisHandler) para usar PENDING_REVIEW
- [x] **Validação de status** adicionada em approveManualPayment e rejectManualPayment
- [x] **FraudAnalysisService** atualizado para retornar PENDING_REVIEW (score 30-70)
- [x] **Transaction persistida corretamente** após aprovação/rejeição manual (confirmado em testes)

### ❌ Pendente
- [ ] Implementar endpoint PUT /manual-analyze/user/{userId} no fraud-service

**Dicas de implementação para endpoint de usuário:**

**1. Estrutura do DTO de decisão de usuário:**
- Pode reutilizar `ManualReviewDecision` ou criar `UserReviewDecision` específico
- Campos necessários: userId, decision (APPROVED/REJECTED), reviewerId, reason
- Considerar adicionar campos específicos para usuário: riskLevel, notes

**2. Lógica no ManualReviewService:**
- Criar método `processUserDecision(UUID userId, String decision, String reviewerId, String reason)`
- Buscar logs de fraude associados ao usuário via `FraudLogRepository.findByPaymentId` ou criar método específico
- Atualizar status nos logs de fraude do usuário (Status_Fraud)
- Publicar evento no Kafka (reutilizar `payflow.manual.decision` ou criar tópico específico `payflow.user.decision`)
- Considerar impacto em pagamentos futuros desse usuário

**3. Considerações sobre persistência:**
- Verificar se existe tabela específica para logs de fraude por usuário ou apenas por pagamento
- Se apenas por pagamento, buscar todos os pagamentos do usuário e atualizar os logs
- Considerar criar tabela `user_fraud_status` para rastrear status de fraude por usuário

**4. No Core Service (consumer):**
- Criar consumer para receber decisões de usuário do Kafka
- Adicionar métodos em `UserService`: `approveUserManual(UUID userId, String reason)` e `rejectUserManual(UUID userId, String reason)`
- Atualizar status do usuário (enum `User_Status`)
- Considerar bloquear/desbloquear pagamentos futuros desse usuário
- Verificar se há pagamentos pendentes desse usuário que precisam ser reavaliados

**5. Fluxo de decisão de usuário:**
- Aprovação: usuário pode continuar fazendo pagamentos normalmente
- Rejeição: usuário pode ser bloqueado ou marcado como "suspeito" para análise futura
- Considerar notificar o usuário sobre a decisão

**6. Testes sugeridos:**
- Testar aprovação de usuário e verificar se pagamentos futuros são processados
- Testar rejeição de usuário e verificar se pagamentos são bloqueados
- Testar notificação ao usuário sobre a decisão
- Testar idempotência (decisão duplicada para mesmo usuário)

---

## O que falta implementar

Apenas o endpoint de análise manual de usuário no fraud-service.