# Manual Decision Refactor - Dev Jorge

## Status: ⚠️ PARCIALMENTE IMPLEMENTADO

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

### ❌ Pendente
- [ ] Remover ManualReviewController do Core Service (se existir)
- [ ] Remover ManualReviewService do Core Service (se existir)
- [ ] Renomear ManualAnalyzeController para ManualReviewController no fraud-service
- [ ] Implementar endpoint PUT /manual-analyze/user/{userId} no fraud-service
- [ ] Adicionar status PENDING_REVIEW no enum Enum_Payment (se necessário)
- [ ] Persistir Transaction após aprovação/rejeição manual

---

## Objetivo

Mover completamente a lógica de decisões manuais de pagamentos do core-service para o fraud-service, utilizando Kafka para comunicação assíncrona entre os serviços.

---

## Problema Atual

Atualmente, a lógica de decisão manual está distribuída entre dois serviços:

**Core Service:**
- ManualReviewController - Endpoint para revisão manual
- ManualReviewService - Lógica de processamento de decisões
- PaymentStatusHandlerFactory - Strategy Pattern para tratar diferentes status

**Fraud Service:**
- ManualAnalyzeController - Endpoints para análise manual de pagamentos e usuários

Esta duplicação gera:
- Acoplamento entre serviços
- Lógica espalhada em múltiplos lugares
- Dificuldade de manutenção
- Fluxo síncrono onde assíncrono seria mais apropriado

---

## Solução Proposta

Centralizar toda a lógica de decisão manual no raud-service e usar Kafka para comunicação assíncrona com o core-service.

---

## Arquitetura Proposta

Core Service -> Kafka (payflow.payment.alerts) -> Fraud Service (FraudAnalysis)
Fraud Service (ManualReview) -> Kafka (payflow.manual.decision) -> Core Service (PaymentConsumer)

---

## Fluxo de Trabalho

### 1. Pagamento Requer Análise Manual

**Fraud Service → Kafka → Core Service**

1. FraudAnalysisService detecta que o pagamento precisa de análise manual (score 30-70)
2. Publica evento em payflow.payment.alerts com AlertType.MANUAL_ANALYSIS
3. Core Service recebe via AlertConsumerService (já existe)
4. Status do pagamento é atualizado para PENDING_REVIEW
5. Pagamento fica aguardando decisão manual

### 2. Decisão Manual Realizada

**Fraud Service → Kafka → Core Service**

1. Analista usa endpoint do Fraud Service para tomar decisão
2. ManualReviewService (no fraud-service) processa a decisão
3. Publica evento em payflow.manual.decision com dados da decisão
4. Core Service recebe via novo consumer
5. Core Service executa a ação baseada na decisão (aprovar/rejeitar)
6. Core Service persiste Transaction com o desfecho

---

## Detalhes de Implementação

### Passo 1: Criar Novo Tópico Kafka

Criar configuração de tópico payflow.manual.decision usando NewTopic bean.

**Dica:** Verificar como outros tópicos estão configurados no projeto (ex: payflow.payment.alerts)

---

### Passo 2: Criar DTO para Decisão Manual

Criar DTO no módulo commons com campos necessários:
- paymentId (UUID)
- decision (enum: APPROVED/REJECTED)
- reason (String)

reviewerId (UUID - opcional)
- 
reviewedAt (Instant)

**Dica:** Usar Lombok para reduzir código boilerplate (@Data, @Builder, etc.)

---

### Passo 3: Criar Producer no Fraud Service

Criar componente que publica eventos no Kafka usando KafkaTemplate.

**Dica:** Injetar KafkaTemplate<String, Object> e usar o método send()

---

### Passo 4: Atualizar ManualReviewService no Fraud Service

Mover lógica de processamento de decisões do Core Service para Fraud Service.

**O que fazer:**
- Atualizar o log de análise no fraud-service
- Publicar decisão para o core-service via producer
- Remover lógica do Core Service

**Dica:** Usar o FraudLogRepository para atualizar o status da análise

---

### Passo 5: Atualizar ManualAnalyzeController no Fraud Service

Centralizar endpoints de decisão manual no Fraud Service.

**Endpoints a manter/atualizar:**
- PUT /manual-review/payment/{paymentId} - Decisão de pagamento
- PUT /manual-review/user/{userId} - Decisão de usuário

**Dica:** Remover ManualReviewController do Core Service

---

### Passo 6: Criar Consumer no Core Service

Criar novo consumer para receber decisões manuais do Kafka.

**O que fazer:**
- Anotar com @KafkaListener no tópico payflow.manual.decision
- Delegar para PaymentService para executar a ação

**Dica:** Usar o mesmo padrão do AlertConsumerService existente

---

### Passo 7: Adicionar Métodos no PaymentService

Criar métodos para processar decisões manuais:
- approveManualPayment(UUID paymentId, String reason)
- rejectManualPayment(UUID paymentId, String reason)

**O que considerar:**
- Validar que o pagamento está em PENDING_REVIEW
- Para aprovação: validar saldo novamente, executar débito/crédito
- Para rejeição: apenas atualizar status
- Persistir Transaction em ambos os casos

**Dica:** Usar @Transactional para garantir consistência

---

### Passo 8: Remover Lógica Antiga do Core Service

**Arquivos a remover/modificar:**
1. Remover ManualReviewController - Endpoints movidos para fraud-service
2. Remover ManualReviewService - Lógica movida para fraud-service
3. Remover PaymentStatusHandlerFactory e handlers relacionados
4. Renomear ManualAnalyzeController do fraud-service para ManualReviewController
5. Manter AlertConsumerService - Apenas para receber alertas iniciais

---

### Passo 9: Atualizar Enum Enum_Payment

Adicionar status PENDING_REVIEW se não existir.

**Dica:** Verificar o enum atual e adicionar o novo status se necessário

---

### Passo 10: Atualizar FraudAnalysisService

Garantir que pagamentos com score 30-70 sejam marcados como MANUAL_ANALYSIS.

**Dica:** Verificar a lógica de threshold atual

---

## Kafka Topics Envolvidos

### Tópicos Existentes
- payflow.payment.alerts - Alertas de fraude (já existe)

### Novo Tópico
- payflow.manual.decision - Decisões manuais de pagamento/usuário

---

## DTOs e Enums

### Novo DTO (commons)
- ManualDecisionEvent - Evento de decisão manual

### Enums Existentes
- AlertType - Já tem MANUAL_ANALYSIS
- Status_Fraud - Já tem MANUAL_ANALYSIS

---

## Fluxo Completo Detalhado

### Cenário 1: Pagamento Requer Análise Manual

1. Usuário cria pagamento via POST /payments
2. Core Service salva como PENDING
3. Core Service chama Fraud Service via Feign
4. Fraud Service analisa e retorna score 45 (exemplo)
5. Core Service recebe MANUAL_ANALYSIS
6. Core Service atualiza pagamento para PENDING_REVIEW
7. Core Service publica evento em payflow.payment.alerts
8. Analista acessa endpoint do Fraud Service
9. Analista aprova pagamento via PUT /manual-review/payment/{paymentId}
10. Fraud Service processa decisão e atualiza log
11. Fraud Service publica em payflow.manual.decision
12. Core Service recebe decisão via consumer
13. Core Service executa débito/crédito
14. Core Service marca pagamento como SUCCESS
15. Core Service persiste Transaction

### Cenário 2: Pagamento Rejeitado Manualmente

Mesmo fluxo acima, mas com decisão REJECTED no passo 9.

---

## Benefícios

1. **Separação de Responsabilidades**: Fraud Service é responsável por todas as decisões de fraude
2. **Desacoplamento**: Core Service não precisa saber sobre lógica de revisão manual
3. **Escalabilidade**: Kafka permite processamento assíncrono e escalável
4. **Rastreabilidade**: Todos os eventos são registrados no Kafka
5. **Manutenibilidade**: Lógica centralizada em um único serviço
6. **Resiliência**: Se um serviço estiver down, mensagens ficam no Kafka

---

## Testes Sugeridos

### Unit Tests
- Testar publicação de eventos no producer
- Testar processamento de decisões no service
- Testar consumo de decisões no consumer
- Testar métodos de aprovação/rejeição no PaymentService

### Integration Tests
- Testar fluxo completo: pagamento → análise → decisão manual → aprovação/rejeição
- Testar retry e DLQ em caso de falha no consumer

---

## Migração

### Passos
1. Implementar novo código em paralelo com o antigo
2. Criar novo tópico Kafka
3. Implementar producer e consumer
4. Testar fluxo completo em ambiente de desenvolvimento
5. Atualizar documentação
6. Remover código antigo do Core Service
7. Deploy em staging para testes
8. Deploy em produção

### Rollback
- Manter código antigo comentado por 1-2 semanas
- Ter script para reverter alterações no Kafka se necessário

---

## Observações

- **Idempotência**: O consumer deve ser idempotente - se receber a mesma decisão duas vezes, não deve processar duplicado
- **DLQ**: Configurar Dead Letter Queue para decisões que falham no processamento
- **Retry**: Configurar retry com backoff exponencial no consumer
- **Logging**: Adicionar logs detalhados em cada etapa do fluxo
- **Monitoring**: Monitorar latência entre decisão e processamento