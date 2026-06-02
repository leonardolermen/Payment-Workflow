# 🚨 Bugs Técnicos Críticos

Bugs técnicos que precisam ser corrigidos com prioridade alta.

---

## 1.1 Entidade Transaction Incompleta

### Problema
- 🐛 **Bug na migration V3**: colunas `payer_id` e `payee_id` existem no modelo Java mas **não foram adicionadas na tabela** `transactions` do script SQL
- 🐛 **`paymentId` é `String`** no modelo mas deveria ser `UUID` para consistência
- 🐛 **`@PrePersist` não seta `executedAt`** — campo `NOT NULL` no banco ficará nulo
- 🐛 **`Transaction` nunca é persistida**: após SUCCESS ou FAILED, nenhum registro é gravado na tabela

### Solução
- [ ] **Criar migration `V8`** corrigindo a tabela `transactions`:
  - Adicionar `payer_id UUID NOT NULL`
  - Adicionar `payee_id UUID NOT NULL`
  - Alterar `payment_id` para `UUID`
- [ ] **Corrigir modelo Java** para alinhar com a migration corrigida
- [ ] **Implementar persistência de Transaction** no fluxo de pagamento:
  - No `PaymentService`, após marcar SUCCESS: criar registro `Transaction` com status SUCCESS, reason "Aprovado", payerId, payeeId, paymentId, executedAt
  - Após marcar FAILED (fraude ou saldo): criar registro `Transaction` com status FAILED e reason descritivo

---

## 1.2 MANUAL_ANALYSIS Não Tratado no PaymentService

### Status: ✅ CORRIGIDO

### Solução Implementada
- ✅ **Tratamento de status `MANUAL_ANALYSIS`** implementado via Strategy Pattern:
  - `ManualAnalysisHandler` criado e registrado no `PaymentStatusHandlerFactory`
  - Pagamento é salvo como status `PENDING` e alerta é enviado via Kafka
  - Decisão manual é processada via `ManualDecisionConsumerService`
- ✅ Status `MANUAL_ANALYSIS` existe no enum `Status_Fraud`

---

## 1.3 Concorrência no Saldo (Double-Spend Risk)

### Status: ✅ CORRIGIDO

### Solução Implementada
- ✅ Query com `@Lock(PESSIMISTIC_WRITE)` adicionada no `UserRepository`: `findByUuidForUpdate`
- ✅ `PaymentService` usa essa query ao buscar payer e payee no método `findUser`
- ✅ Lock pessimista previne double-spend em cenários de concorrência
