# PayFlow - Plano Detalhado de Tarefas (Tasks de Implementação)

Este documento centraliza todas as etapas técnicas de alto a baixo nível exigidas para finalizar o ecossistema PayFlow. Siga esta ordem ou atribua as *epics* de forma paralela.

---

## 🟢 Fase 1: Modelagem de Dados e Entidades (JPA)
A fundação do sistema. Começaremos desenhando as tabelas e entidades principais no `core-service` e no `fraud-service`.

### 1.1 Entidades do `core-service`
- [ ] **Criar Entidade `User` (Perfil de cliente)**
  - Campos: `id` (UUID), `name`, `email` (único), `password` (criptografada), `cpfCnpj` (único), `balance` (Decimal), `status` (ACTIVE, INACTIVE), `createdAt`.
  - Configurar mapeamento `@Table(name = "users")` e restrições.
- [ ] **Criar Entidade `Payment` (Intenção de Pagamento)**
  - Campos: `id` (UUID), `payerId` (Vínculo c/ User do pagador), `payeeId` (Vínculo c/ User recebedor), `amount` (Decimal), `status` (PENDING, REJECTED, SUCCESS, FAILED), `idempotencyKey` (Unique), `createdAt`.
  - Configurar mapeamento `@Table(name = "payments")`.
- [ ] **Criar Entidade `Transaction` (Liquidação Efetiva)**
  - Campos: `id` (UUID), `paymentId` (Vínculo c/ Payment), `status` (SUCCESS, FAILED), `reason` (varchar para logs de falha), `executedAt`.
  - Configurar mapeamento `@Table(name = "transactions")`.
- [ ] **Criar Script Migration `V1`**
  - Mapear a criação dessas entidades no script Flyway `V1__init.sql` no `core-service`.

### 1.2 Entidades do `fraud-service`
- [ ] **Criar Entidade `FraudAnalysisLog`**
  - Campos: `id` (UUID), `paymentId` (UUID de referência do core), `score` (Decimal/Double), `status` (APPROVED, REJECTED), `reason`, `evaluatedAt`.
  - Configurar mapeamento na Base do Fraud (`@Table(name = "fraud_analysis_logs")`).
- [ ] **Criar Script Migration `V1` do Fraud**
  - Escrever DDL correspondente no arquivo `V1__init.sql` do `fraud-service`.

---

## 🟡 Fase 2: Regras de Banco de Dados e Repository Pattern
- [ ] Criar `UserRepository`, `PaymentRepository`, e `TransactionRepository` no `core-service` estendendo `JpaRepository`.
- [ ] Criar `FraudLogRepository` no `fraud-service`.
- [ ] Escrever queries customizadas (ex: Buscar usuário por e-mail, buscar pagamento por Idempotency-key).

---

## 🔵 Fase 3: Segurança, Gateway e Autenticação
- [ ] **Configurar API Gateway (`api-gateway`)**:
  - Definir rotas YAML no `application.yml` apontando `/core/**` para `localhost:8081` e `/fraud/**` para `localhost:8082`.
  - Aplicar políticas simples de CORS.
- [ ] **Módulo de Autenticação (`core-service` ou microserviço extra)**:
  - Add Spring Security e Biblioteca Jwt (ex: `jjwt`).
  - Implementar o filtro `JwtAuthenticationFilter` para validar tokens nos Headers das requests REST.
  - Implementar os endpoints `POST /auth/register` (Cria usuário encriptando senha c/ BCrypt) e `POST /auth/login` (Gera token JWT e devolve ao cliente).

---

## 🟣 Fase 4: O Coração do Pagamento (Payment API síncrona)
- [ ] **Configurar Redis**: Adicionar dependência do `spring-boot-starter-data-redis` no `core-service`.
- [ ] **Implementar Endpoint `POST /payments` (`core-service`)**:
  - Extrair o Header `Idempotency-Key` e validar no cache temporal (TTL de 24h). Devolver pagamento retido se a chave já existir.
  - Criar registro na tabela `payments` com status PENDING.
- [ ] **Integração Feign / REST c/ Fraud Service**:
  - Criar `FraudClient` (via `@FeignClient` ou `RestClient`) no `core-service`.
  - Implementar API no `fraud-service` (`POST /fraud/analyze` recebendo DTO do pagamento).
  - Configurar regras lógicas no `fraud-service` (ex: recusa se valor for maior que 10.000). Salva análise na tabela. Retorna Status (APPROVED / REJECTED) pro core.
- [ ] **Atualização Conforme Antifraude**:
  - Se Fraud Service == REJECTED, atualizar Payment pra REJECTED e retornar `400`.
  - Se Fraud Service == APPROVED, o fluxo continua (commit no banco como PENDING).

---

## 🟠 Fase 5: Mensageria (Event-Driven Flow c/ Kafka)
- [ ] **Producers**: 
  - Configurar classe produtora no `core-service` para postar no tópico `payment-created-topic` após aprovar no antifraude.
- [ ] **Configurar Tópicos**:
  - Via Docker-compose ou código Java (`NewTopic`), expor os tópicos padrão `payment-created-topic` e `transaction-completed-topic`.

---

## 🟤 Fase 6: Liquidação Financeira (Transaction Logic)
*(Assumindo que essa lógica ficará no `core-service` ou em um módulo novo focado em consumer)*
- [ ] **Kafka Consumer de Pagamentos**:
  - Escrever `@KafkaListener(topics = "payment-created-topic")` no componente de transação.
- [ ] **Lógica de Lock Otimista/Pessimista de Saldo**:
  - Pegar o `payerId` (remetente), travar linha (`@Lock(LockModeType.PESSIMISTIC_WRITE)`).
  - Verificar se saldo é suficiente.
  - Deduzir balance do payer, transferir balance na conta do `payee`.
- [ ] **Salvar Transação**:
  - Criar `Transaction` no BD da liquidação com os registros da operação aprovada ou negada por saldo insuficiente.
- [ ] **Emissão de Conclusão**:
  - Produzir evento de sucesso/falha no tópico `transaction-completed-topic`.

---

## 🔴 Fase 7: Sistema de Notificações e Webhooks
- [ ] **Criar / Configurar Notification Consumer (`core-service` ou microserviço à parte)**:
  - Ouver `@KafkaListener(topics = "transaction-completed-topic")`.
- [ ] **Disparo de Serviços Externos**:
  - Atualizar status final da linha na tabela `payments` (SUCCESS ou FAILED).
  - Implementar envio simulado de Webhook (`RestTemplate` fazendo POST pra URL de log do cliente).
  - Envio simulado de e-mail (Log ou serviço JavaMailSender).
- [ ] **Tratamento de Falhas e Dead Letter Queue**:
  - Configurar Resilience4j + DLT (Dead Letter Topic). Se a notificação falhar 3x por erro 500 do Webhook, droppar no tópico `-dql` do Kafka.
