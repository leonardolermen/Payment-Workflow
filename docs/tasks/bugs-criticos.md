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
- [ ] **Criar migration `V5`** corrigindo a tabela `transactions`:
  - Adicionar `payer_id UUID NOT NULL`
  - Adicionar `payee_id UUID NOT NULL`
  - Alterar `payment_id` para `UUID`
- [ ] **Corrigir modelo Java** para alinhar com a migration corrigida
- [ ] **Implementar persistência de Transaction** no fluxo de pagamento:
  - No `PaymentService`, após marcar SUCCESS: criar registro `Transaction` com status SUCCESS, reason "Aprovado", payerId, payeeId, paymentId, executedAt
  - Após marcar FAILED (fraude ou saldo): criar registro `Transaction` com status FAILED e reason descritivo

---

## 1.2 MANUAL_ANALYSIS Não Tratado no PaymentService

### Problema
- ⚠️ O `fraud-service` pode retornar status `MANUAL_ANALYSIS` (score 30–70), mas o `PaymentService` só checa `REJECTED`
- Pagamentos de risco médio estão sendo aprovados silenciosamente

### Solução
- [ ] **Tratar status `MANUAL_ANALYSIS`**:
  - Salvar pagamento como status `PENDING_REVIEW` e não realizar o débito/crédito
  - Aguardar decisão manual via `ManualReviewService`
- [ ] Adicionar status `PENDING_REVIEW` no enum `Enum_Payment`

---

## 1.3 Concorrência no Saldo (Double-Spend Risk)

### Problema
- 🐛 Atualmente `userRepository.save(payer)` e `userRepository.save(payee)` dentro de `@Transactional` sem lock explícito
- Em cenário de concorrência (dois pagamentos simultâneos do mesmo usuário), pode ocorrer double-spend

### Solução
- [ ] Adicionar query com `@Lock(PESSIMISTIC_WRITE)` no `UserRepository`: `findByUuidForUpdate`
- [ ] Usar essa query ao buscar payer e payee no `PaymentService`
