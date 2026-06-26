# Guia de Testes: Strategy Pattern com Anti-Fraude

## 📋 Visão Geral

Este documento explica como testar o Strategy Pattern implementado para os 5 status do anti-fraude e como verificar o funcionamento do Kafka.

## 🎯 Objetivos dos Testes

1. **Testar todos os 5 status** do anti-fraude
2. **Verificar handlers** específicos para cada status
3. **Confirmar alertas Kafka** sendo enviados
4. **Validar sistema de aprovação manual**
5. **Testar fluxo completo** de pagamento

## 🚀 Como Rodar os Testes

### **1. Subir Infraestrutura**

```bash
# Subir Kafka e PostgreSQL
docker-compose up -d

# Verificar containers
docker ps

# Verificar tópicos Kafka
docker exec -it payflow-kafka kafka-topics --list --bootstrap-server localhost:9092
```

### **2. Iniciar Aplicação**

```bash
# Na pasta core-service
mvn spring-boot:run

# Ou via IDE
# Run -> Spring Boot Application
```

### **3. Verificar Logs de Inicialização**

```
✅ Kafka Topics Created:
   - payflow.payment.alerts
   - payflow.review.completed
   - payflow.payment.requested
   - payflow.fraud.completed
   - payflow.transaction.completed

✅ Handlers Registered:
   - ApprovedHandler
   - RejectedHandler
   - PendingReviewHandler
   - ManualAnalysisHandler
   - SuspiciousHandler
```

## 👥 Criar Usuários para Testes

### **⚠️ IMPORTANTE: Problema de Autenticação Identificado**

O erro `403 Forbidden` ocorre porque:

1. **Porta correta:** A aplicação roda na porta `8081` (veja application.yml)
2. **Segurança habilitada:** Todos os endpoints exigem autenticação JWT
3. **UserController não tem POST:** Não existe endpoint POST /users

### **✅ Solução Correta: Usar AuthController**

Use o endpoint de registro que já existe:

---

### **1. Criar Usuário Payer (João Silva)**

#### **Como Testar via Postman:**

**Método:** `POST`
**URL:** `http://localhost:8081/auth/register`
**Headers:**
```
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "name": "João Silva",
  "email": "joao.silva@teste.com",
  "document": "12345678901",
  "documentType": "CPF",
  "password": "senha123",
  "balance": 100000.00
}
```

#### **Resultado Esperado:**
- **Status HTTP:** `200 OK`
- **Response esperada:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": 1,
    "uuid": "550e8400-e29b-41d4-a716-446655440001",
    "name": "João Silva",
    "email": "joao.silva@teste.com",
    "document": "12345678901",
    "balance": 100000.00,
    "createdAt": "2024-01-01T10:00:00"
  }
}
```

---

### **2. Criar Usuário Payee (Maria Santos)**

#### **Como Testar via Postman:**

**Método:** `POST`
**URL:** `http://localhost:8081/auth/register`
**Headers:**
```
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "name": "Maria Santos",
  "email": "maria.santos@teste.com",
  "document": "98765432109",
  "documentType": "CPF",
  "password": "senha123",
  "balance": 5000.00
}
```

#### **Resultado Esperado:**
- **Status HTTP:** `200 OK`
- **Response esperada:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": 2,
    "uuid": "550e8400-e29b-41d4-a716-446655440002",
    "name": "Maria Santos",
    "email": "maria.santos@teste.com",
    "document": "98765432109",
    "balance": 5000.00,
    "createdAt": "2024-01-01T10:00:00"
  }
}
```

---

### **3. Salvar Tokens para Próximos Testes**

Guarde os tokens retornados no registro:
- **Token João:** `eyJhbGciOiJIUzI1NiJ9...`
- **Token Maria:** `eyJhbGciOiJIUzI1NiJ9...`

---

### **4. Verificar Saldos Iniciais (com autenticação)**

#### **Como Testar via Postman:**

**Método:** `GET`
**URL:** `http://localhost:8081/users/550e8400-e29b-41d4-a716-446655440001`
**Headers:**
```
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

#### **Resultado Esperado (Payer):**
```json
{
  "id": 1,
  "uuid": "550e8400-e29b-41d4-a716-446655440001",
  "name": "João Silva",
  "email": "joao.silva@teste.com",
  "document": "12345678901",
  "balance": 100000.00,
  "createdAt": "2024-01-01T10:00:00"
}
```

#### **Como Testar via Postman:**

**Método:** `GET`
**URL:** `http://localhost:8081/users/550e8400-e29b-41d4-a716-446655440002`
**Headers:**
```
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

#### **Resultado Esperado (Payee):**
```json
{
  "id": 2,
  "uuid": "550e8400-e29b-41d4-a716-446655440002",
  "name": "Maria Santos",
  "email": "maria.santos@teste.com",
  "document": "98765432109",
  "balance": 5000.00,
  "createdAt": "2024-01-01T10:00:00"
}
```

---

### **3. Verificar Saldos Iniciais**

#### **Como Testar via Postman:**

**Método:** `GET`
**URL:** `http://localhost:8080/users/550e8400-e29b-41d4-a716-446655440001`

#### **Resultado Esperado (Payer):**
```json
{
  "id": 1,
  "uuid": "550e8400-e29b-41d4-a716-446655440001",
  "name": "João Silva",
  "email": "joao.silva@teste.com",
  "document": "12345678901",
  "balance": 100000.00,
  "createdAt": "2024-01-01T10:00:00"
}
```

#### **Como Testar via Postman:**

**Método:** `GET`
**URL:** `http://localhost:8080/users/550e8400-e29b-41d4-a716-446655440002`

#### **Resultado Esperado (Payee):**
```json
{
  "id": 2,
  "uuid": "550e8400-e29b-41d4-a716-446655440002",
  "name": "Maria Santos",
  "email": "maria.santos@teste.com",
  "document": "98765432109",
  "balance": 5000.00,
  "createdAt": "2024-01-01T10:00:00"
}
```

---

### **4. Testar Transferência Bem-Sucedida**

#### **Como Testar via Postman:**

**Método:** `POST`
**URL:** `http://localhost:8081/payments`
**Headers:**
```
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Body (JSON):**
```json
{
  "idempotencyKey": "test-transfer-001",
  "payerId": "550e8400-e29b-41d4-a716-446655440001",
  "payeeId": "550e8400-e29b-41d4-a716-446655440002",
  "amount": 1000.00
}
```

#### **Resultado Esperado:**
- **Status HTTP:** `200 OK`
- **Response esperada:**
```json
{
  "id": 123,
  "uuid": "payment-uuid-gerado",
  "payerId": "550e8400-e29b-41d4-a716-446655440001",
  "payeeId": "550e8400-e29b-41d4-a716-446655440002",
  "amount": 1000.00,
  "status": "SUCCESS",
  "createdAt": "2024-01-01T10:00:00"
}
```

---

### **5. Verificar Detalhes da Transferência**

#### **Como Testar via Postman:**

**Método:** `GET`
**URL:** `http://localhost:8081/payments/{payment-uuid-gerado}`
**Headers:**
```
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

*Substitua `{payment-uuid-gerado}` pelo UUID retornado na criação do pagamento*

#### **Resultado Esperado:**
```json
{
  "id": 123,
  "uuid": "payment-uuid-gerado",
  "payerId": "550e8400-e29b-41d4-a716-446655440001",
  "payeeId": "550e8400-e29b-41d4-a716-446655440002",
  "amount": 1000.00,
  "status": "SUCCESS",
  "createdAt": "2024-01-01T10:00:00"
}
```

---

### **6. Verificar Saldos Finais**

#### **Como Testar via Postman:**

**Método:** `GET`
**URL:** `http://localhost:8081/users/550e8400-e29b-41d4-a716-446655440001`
**Headers:**
```
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

#### **Resultado Esperado (Payer - após transferência):**
```json
{
  "id": 1,
  "uuid": "550e8400-e29b-41d4-a716-446655440001",
  "name": "João Silva",
  "email": "joao.silva@teste.com",
  "document": "12345678901",
  "balance": 99000.00,
  "createdAt": "2024-01-01T10:00:00"
}
```

#### **Como Testar via Postman:**

**Método:** `GET`
**URL:** `http://localhost:8081/users/550e8400-e29b-41d4-a716-446655440002`
**Headers:**
```
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

#### **Resultado Esperado (Payee - após transferência):**
```json
{
  "id": 2,
  "uuid": "550e8400-e29b-41d4-a716-446655440002",
  "name": "Maria Santos",
  "email": "maria.santos@teste.com",
  "document": "98765432109",
  "balance": 6000.00,
  "createdAt": "2024-01-01T10:00:00"
}
```

---

### **6. Testar Saldo Insuficiente**

#### **Como Testar via Postman:**

**Método:** `POST`
**URL:** `http://localhost:8080/payments`
**Headers:**
```
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "idempotencyKey": "test-insufficient-001",
  "payerId": "550e8400-e29b-41d4-a716-446655440001",
  "payeeId": "550e8400-e29b-41d4-a716-446655440002",
  "amount": 150000.00
}
```

#### **Resultado Esperado:**
- **Status HTTP:** `400 BAD REQUEST`
- **Mensagem:** `"Saldo insuficiente"`
- **Saldos inalterados**

## 🧪 Testes por Status

### **STATUS 1: APPROVED**

#### **Como Testar via Postman:**

**Método:** `POST`
**URL:** `http://localhost:8080/payments`
**Headers:**
```
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "idempotencyKey": "test-approved-001",
  "payerId": "550e8400-e29b-41d4-a716-446655440001",
  "payeeId": "550e8400-e29b-41d4-a716-446655440002",
  "amount": 100.00
}
```

#### **Resultado Esperado:**
- **Status HTTP:** `200 OK`
- **Status final:** `SUCCESS`
- **Saldo transferido:** Payer (-100) → Payee (+100)
- **Response esperada:**
```json
{
  "id": 123,
  "uuid": "payment-uuid-gerado",
  "payerId": "550e8400-e29b-41d4-a716-446655440001",
  "payeeId": "550e8400-e29b-41d4-a716-446655440002",
  "amount": 100.00,
  "status": "SUCCESS",
  "createdAt": "2024-01-01T10:00:00"
}
```
- **Logs esperados:**
  ```
  ✅ Pagamento aprovado e processado com sucesso
  💰 Transferência realizada: Payer -> Payee, Amount: 100.00
  ```

#### **Verificação no Banco:**
```sql
SELECT * FROM payments WHERE idempotency_key = 'test-approved-001';
SELECT * FROM users WHERE uuid IN ('550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440002');
```

---

### **STATUS 2: REJECTED**

#### **Como Testar via Postman:**

**Método:** `POST`
**URL:** `http://localhost:8080/payments`
**Headers:**
```
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "idempotencyKey": "test-rejected-001",
  "payerId": "550e8400-e29b-41d4-a716-446655440001",
  "payeeId": "550e8400-e29b-41d4-a716-446655440002",
  "amount": 999999.00
}
```

#### **Resultado Esperado:**
- **Status HTTP:** `400 BAD REQUEST`
- **Mensagem:** `"Pagamento não autorizado"`
- **Status final:** `FAILED`
- **Sem transferência:** Saldos inalterados
- **Logs esperados:**
  ```
  ⚠️ Pagamento rejeitado pelo anti-fraude
  📄 ALERTA: Pagamento rejeitado - UUID: xxx, Amount: 999999.00
  ```

#### **Verificação no Banco:**
```sql
SELECT * FROM payments WHERE status = 'FAILED' AND idempotency_key = 'test-rejected-001';
```

---

### **STATUS 3: PENDING_REVIEW**

#### **Como Testar via Postman:**

**Método:** `POST`
**URL:** `http://localhost:8080/payments`
**Headers:**
```
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "idempotencyKey": "test-pending-001",
  "payerId": "550e8400-e29b-41d4-a716-446655440001",
  "payeeId": "550e8400-e29b-41d4-a716-446655440002",
  "amount": 5000.00
}
```

#### **Resultado Esperado:**
- **Status HTTP:** `200 OK`
- **Status final:** `PENDING`
- **Sem transferência:** Aguardando análise
- **Response esperada:**
```json
{
  "id": 124,
  "uuid": "payment-uuid-gerado",
  "payerId": "550e8400-e29b-41d4-a716-446655440001",
  "payeeId": "550e8400-e29b-41d4-a716-446655440002",
  "amount": 5000.00,
  "status": "PENDING",
  "createdAt": "2024-01-01T10:00:00"
}
```
- **Alerta Kafka enviado:** `payflow.payment.alerts`
- **Logs esperados:**
  ```
  🚨 Pagamento em análise manual: xxx | Alerta enviado via Kafka
  📥 ALERTA RECEBIDO: PENDING_REVIEW | Pagamento: xxx
  📧 Enviando email para equipe de análise
  ```

#### **Verificação no Kafka:**
```bash
# Consumir alertas em tempo real
docker exec -it payflow-kafka kafka-console-consumer \
  --topic payflow.payment.alerts \
  --bootstrap-server localhost:9092 \
  --from-beginning
```

#### **Verificação API de Análise:**
```http
GET http://localhost:8080/api/manual-review/pending
```

---

### **STATUS 4: MANUAL_ANALYSIS**

#### **Como Testar via Postman:**

**Método:** `POST`
**URL:** `http://localhost:8080/payments`
**Headers:**
```
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "idempotencyKey": "test-manual-001",
  "payerId": "550e8400-e29b-41d4-a716-446655440001",
  "payeeId": "550e8400-e29b-41d4-a716-446655440002",
  "amount": 10000.00
}
```

#### **Resultado Esperado:**
- **Status HTTP:** `200 OK`
- **Status final:** `PENDING`
- **Alerta Kafka enviado:** `MANUAL_ANALYSIS`
- **Response esperada:**
```json
{
  "id": 125,
  "uuid": "payment-uuid-gerado",
  "payerId": "550e8400-e29b-41d4-a716-446655440001",
  "payeeId": "550e8400-e29b-41d4-a716-446655440002",
  "amount": 10000.00,
  "status": "PENDING",
  "createdAt": "2024-01-01T10:00:00"
}
```
- **Logs esperados:**
  ```
  🔍 Pagamento em análise manual detalhada: xxx | Alerta enviado via Kafka
  📥 ALERTA RECEBIDO: MANUAL_ANALYSIS | Pagamento: xxx
  📧 Enviando email para a equipe de análise manual
  ```

---

### **STATUS 5: SUSPICIOUS**

#### **Como Testar via Postman:**

**Método:** `POST`
**URL:** `http://localhost:8080/payments`
**Headers:**
```
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "idempotencyKey": "test-suspicious-001",
  "payerId": "550e8400-e29b-41d4-a716-446655440001",
  "payeeId": "550e8400-e29b-41d4-a716-446655440002",
  "amount": 50000.00
}
```

#### **Resultado Esperado:**
- **Status HTTP:** `200 OK`
- **Status final:** `PENDING`
- **Alerta Kafka enviado:** `SUSPICIOUS`
- **Response esperada:**
```json
{
  "id": 126,
  "uuid": "payment-uuid-gerado",
  "payerId": "550e8400-e29b-41d4-a716-446655440001",
  "payeeId": "550e8400-e29b-41d4-a716-446655440002",
  "amount": 50000.00,
  "status": "PENDING",
  "createdAt": "2024-01-01T10:00:00"
}
```
- **Logs esperados:**
  ```
  🚨 Atividade suspeita detectada: xxx | Alerta crítico enviado via Kafka
  📥 ALERTA RECEBIDO: SUSPICIOUS | Pagamento: xxx
  📧 Enviando email para equipe de segurança
  ```

## 🔧 Testar Sistema de Aprovação Manual

### **1. Listar Pagamentos Pendentes**

#### **Como Testar via Postman:**

**Método:** `GET`
**URL:** `http://localhost:8080/api/manual-review/pending`
**Headers:**
```
Content-Type: application/json
```

#### **Resposta Esperada:**
```json
[
  {
    "paymentId": "uuid-do-pagamento-que-ficou-pendente",
    "payerId": "550e8400-e29b-41d4-a716-446655440001",
    "payeeId": "550e8400-e29b-41d4-a716-446655440002",
    "amount": 5000.00,
    "status": "PENDING",
    "createdAt": "2024-01-01T10:00:00"
  }
]
```

---

### **2. Aprovar Pagamento Manualmente**

#### **Como Testar via Postman:**

**Método:** `POST`
**URL:** `http://localhost:8080/api/manual-review/decision`
**Headers:**
```
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "paymentId": "uuid-do-pagamento-que-ficou-pendente",
  "reviewerId": "analista-001",
  "decision": "APPROVED",
  "reason": "Cliente verificado, histórico limpo",
  "notes": "Documentação apresentada e validada"
}
```

#### **Resultado Esperado:**
- **Status HTTP:** `200 OK`
- **Status muda para:** `SUCCESS`
- **Transferência realizada:** Saldos atualizados
- **Logs esperados:**
  ```
  🔄 Notificando sistema externo da decisão: APPROVED | Pagamento: xxx | Analista: analista-001
  ✅ Sistema externo notificado com sucesso
  💰 Transferência realizada: Payer -> Payee, Amount: 5000.00
  ```

---

### **3. Rejeitar Pagamento Manualmente**

#### **Como Testar via Postman:**

**Método:** `POST`
**URL:** `http://localhost:8080/api/manual-review/decision`
**Headers:**
```
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "paymentId": "uuid-do-pagamento-que-ficou-pendente",
  "reviewerId": "analista-001",
  "decision": "REJECTED",
  "reason": "Fraude confirmado após investigação",
  "notes": "Múltiplas tentativas suspeitas detectadas"
}
```

#### **Resultado Esperado:**
- **Status HTTP:** `200 OK`
- **Status muda para:** `FAILED`
- **Sem transferência:** Saldos inalterados
- **Logs esperados:**
  ```
  🔄 Notificando sistema externo da decisão: REJECTED | Pagamento: xxx | Analista: analista-001
  ✅ Sistema externo notificado com sucesso
  ⚠️ Pagamento rejeitado pelo anti-fraude
  ```

---

### **4. Verificar Histórico de Decisões**

#### **Como Testar via Postman:**

**Método:** `GET`
**URL:** `http://localhost:8080/api/manual-review/history`
**Headers:**
```
Content-Type: application/json
```

#### **Resposta Esperada:**
```json
[
  {
    "paymentId": "uuid-do-pagamento-aprovado",
    "reviewerId": "analista-001",
    "decision": "APPROVED",
    "reason": "Cliente verificado",
    "notes": "Documentação ok"
  },
  {
    "paymentId": "uuid-do-pagamento-rejeitado",
    "reviewerId": "analista-002",
    "decision": "REJECTED",
    "reason": "Fraude confirmado",
    "notes": "Atividade suspeita detectada"
  }
]
```

## 📊 Monitoramento Kafka em Tempo Real

### **1. Consumir Alertas de Pagamento**

#### **Como Testar via Postman:**

**Método:** `POST`
**URL:** `http://localhost:8080/api/manual-review/decision`
**Headers:**
```
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "paymentId": "uuid-do-pagamento-que-ficou-pendente",
  "reviewerId": "analista-001",
  "decision": "APPROVED",
  "reason": "Cliente verificado, histórico limpo",
  "notes": "Documentação apresentada e validada"
}
```

#### **Resultado Esperado:**
- **Status HTTP:** `200 OK`
- **Status muda para:** `SUCCESS`
- **Transferência realizada:** Saldos atualizados
- **Logs esperados:**
  ```
  🔄 Notificando sistema externo da decisão: APPROVED | Pagamento: xxx | Analista: analista-001
  ✅ Sistema externo notificado com sucesso
  💰 Transferência realizada: Payer -> Payee, Amount: 5000.00
  ```

---

### **2. Monitoramento Kafka em Tempo Real**

#### **Via Docker (Terminal):**
```bash
# Terminal 1: Alertas de pagamento
docker exec -it payflow-kafka kafka-console-consumer \
  --topic payflow.payment.alerts \
  --bootstrap-server localhost:9092 \
  --from-beginning

# Terminal 2: Decisões manuais
docker exec -it payflow-kafka kafka-console-consumer \
  --topic payflow.review.completed \
  --bootstrap-server localhost:9092 \
  --from-beginning
```

#### **Mensagem de Alerta Esperada (PENDING_REVIEW):**
```json
{
  "paymentId": "uuid-do-pagamento-que-ficou-pendente",
  "payerId": "550e8400-e29b-41d4-a716-446655440001",
  "payeeId": "550e8400-e29b-41d4-a716-446655440002",
  "amount": 5000.00,
  "alertType": "PENDING_REVIEW",
  "reason": "Pagamento requer análise manual",
  "timestamp": "2024-01-01T10:00:00"
}
```

#### **Mensagem de Decisão Esperada:**
```json
{
  "paymentId": "uuid-do-pagamento-aprovado",
  "reviewerId": "analista-001",
  "decision": "APPROVED",
  "reason": "Cliente verificado, histórico limpo",
  "notes": "Documentação apresentada e validada"
}
```

#### **Verificação no Kafka UI:**
Se tiver Kafka UI (como Conduktor), acesse:
- **Topic:** `payflow.payment.alerts`
- **Topic:** `payflow.review.completed`
- **Consumer Group:** `alert-group` e `review-notification-group`

## 🐛 Troubleshooting Comum

### **Problema 1: Handler não encontrado**
```
❌ IllegalArgumentException: Status inválido: PENDING_REVIEW
```
**Solução:** Verifique se o handler foi registrado na factory

### **Problema 2: Kafka não envia mensagem**
```
❌ Timeout ao enviar mensagem para Kafka
```
**Solução:** Verifique se o Kafka está rodando e os tópicos criados

### **Problema 3: Pagamento fica em PENDING**
```
❌ Pagamento não muda de status após decisão manual
```
**Solução:** Verifique se o handler está sendo chamado corretamente

### **Problema 4: Saldo não atualizado**
```
❌ Transferência não refletida no saldo
```
**Solução:** Verifique se ApprovedHandler está sendo executado

## ✅ Checklist de Validação

### **Para Cada Teste:**
- [ ] Status do pagamento atualizado corretamente
- [ ] Saldos transferidos (se aprovado)
- [ ] Alerta Kafka enviado (se aplicável)
- [ ] Logs gerados corretamente
- [ ] API de análise manual funcional
- [ ] Histórico de decisões salvo

### **Validação Final:**
- [ ] Todos os 5 status testados
- [ ] Kafka funcionando para ambos os tópicos
- [ ] Sistema de aprovação manual completo
- [ ] Sem erros nos logs
- [ ] Performance aceitável

## 🎯 Cenários de Teste Adicionais

### **Teste de Carga:**
```bash
# Múltiplos pagamentos simultâneos
for i in {1..10}; do
  curl -X POST http://localhost:8080/payments \
    -H "Content-Type: application/json" \
    -d '{"idempotencyKey": "load-test-'$i'", "payerId": "payer-'$i'", "payeeId": "payee-'$i'", "amount": 100.00}' &
done
```

### **Teste de Idempotência:**
```bash
# Mesma requisição duplicada
curl -X POST http://localhost:8080/payments \
  -H "Content-Type: application/json" \
  -d '{"idempotencyKey": "test-duplicate", "payerId": "payer", "payeeId": "payee", "amount": 100.00}'

# Segunda vez - deve retornar 409
curl -X POST http://localhost:8080/payments \
  -H "Content-Type: application/json" \
  -d '{"idempotencyKey": "test-duplicate", "payerId": "payer", "payeeId": "payee", "amount": 100.00}'
```

---

## 📝 Resumo do Strategy Pattern

### **Componentes Implementados:**
1. **PaymentStatusHandler** - Interface estratégica
2. **PaymentStatusHandlerFactory** - Factory de handlers
3. **5 Handlers** - Um para cada status de fraude
4. **Sistema de Aprovação Manual** - API completa
5. **Integração Kafka** - Alertas e notificações
6. **Logging** - Monitoramento completo

### **Benefícios Alcançados:**
- ✅ **Desacoplamento** - Cada handler independente
- ✅ **Extensibilidade** - Fácil adicionar novos status
- ✅ **Manutenibilidade** - Lógica centralizada
- ✅ **Assincronismo** - Alertas via Kafka
- ✅ **Auditoria** - Sistema de aprovação manual
- ✅ **Monitoramento** - Logs detalhados

**O sistema está pronto para produção!** 🚀
