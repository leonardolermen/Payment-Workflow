# [ ] Funcionalidades Futuras

Funcionalidades planejadas para implementação futura (prioridade baixa).

---

## 4.1 Kafka - Event-Driven Architecture

### Status: ⚠️ PARCIALMENTE IMPLEMENTADO

- [x] Criar `KafkaTopicsConfig.java` com beans `NewTopic` (8 tópicos configurados)
- [x] Criar `FraudEventProducer` no fraud-service
- [x] Criar `PaymentAlertEvent` DTO no commons
- [ ] Criar `PaymentEventProducer` no core-service
- [ ] Criar `PaymentEvent` DTO no commons
- [ ] Refatorar `POST /payments` para assíncrono (PROCESSING + Kafka)
- [ ] Criar `PaymentRequestedConsumer` no fraud-service
- [ ] Criar `FraudResultProducer` no fraud-service
- [ ] Criar `FraudCompletedConsumer` no core-service

**Tópicos Kafka Implementados:**
- payflow.payment.requested
- payflow.fraud.completed
- payflow.payment.alerts
- payflow.transaction.completed
- payflow.review.completed
- payflow.payment.alerts.dlq
- payflow.review.completed.dlq
- payflow.manual.decision
- payflow.manual.decision.dlq

---

## 4.2 Notificações

### Status: ⚠️ PARCIALMENTE IMPLEMENTADO

- [x] Criar `AlertConsumerService` para processar alertas
- [x] Criar `ManualDecisionConsumerService` para processar decisões manuais
- [x] Implementar Dead Letter Topic (DLQ) para falhas
- [x] Implementar retry com backoff configurado no KafkaConfig
- [ ] Implementar webhook simulado
- [ ] Implementar e-mail simulado (log)
- [ ] Adicionar container Mailhog no docker-compose
- [ ] Criar endpoint `POST /admin/dlq/replay`

---

## 4.3 Testes

### Status: ❌ NÃO IMPLEMENTADO

- [ ] `PaymentServiceTest` com JUnit 5 + Mockito
  - Cenário: idempotência detectada → deve lançar `409 CONFLICT`
  - Cenário: saldo insuficiente → deve lançar `400 BAD_REQUEST`
  - Cenário: auto-transferência → deve lançar `400 BAD_REQUEST`
  - Cenário: fraude REJECTED → deve salvar FAILED e lançar exceção
  - Cenário: fraude APPROVED → deve debitar/creditar e salvar SUCCESS
- [ ] `FraudAnalysisServiceTest` testando cada regra
  - Testar cada regra de score individualmente (valor alto, conta nova, status inativo)
  - Testar limites de threshold (score 29 → APPROVED, score 30 → MANUAL_ANALYSIS, score 71 → REJECTED)
- [ ] `AuthServiceTest`: registro com e-mail duplicado, login com credenciais inválidas
- [ ] `JwtServiceTest`: geração de token, validação, token expirado
- [ ] Testcontainers + PostgreSQL para testes de repositório
- [ ] `PaymentFlowIntegrationTest`: fluxo ponta-a-ponta

---

## 4.4 Documentação de API

### Status: ❌ NÃO IMPLEMENTADO

- [ ] Adicionar SpringDoc OpenAPI (`springdoc-openapi-starter-webmvc-ui`)
- [ ] Acessível em `/swagger-ui.html` de cada serviço
- [ ] Anotar controllers com `@Operation`, `@ApiResponse`
- [ ] Configurar rota no api-gateway para Swagger unificado

---

## 4.5 Observabilidade

### Status: ⚠️ PARCIALMENTE IMPLEMENTADO

- [x] Adicionar `spring-boot-starter-actuator` (dependência adicionada)
- [x] Adicionar `micrometer-tracing-bridge-otel` para tracing distribuído
- [x] Adicionar `opentelemetry-exporter-otlp` para exportação de métricas
- [ ] Expor `/actuator/health` em ambos os serviços
- [ ] Adicionar `micrometer-registry-prometheus` para métricas
- [ ] Adicionar container Prometheus + Grafana no docker-compose
- [ ] Dashboards: taxa de pagamentos por status, latência p99, taxa de rejeição

---

## 4.6 Listagens com Paginação

### Status: ⚠️ PARCIALMENTE IMPLEMENTADO

- [x] Adicionar `GET /payments` com listagem completa
- [x] Adicionar `GET /payments/users/{userId}` com listagem por usuário
- [x] Adicionar `GET /payments/status/{status}` com listagem por status
- [ ] Adicionar `GET /payments` com `Pageable`
- [ ] Adicionar `GET /users` com `Pageable`
- [ ] Adicionar `findByStatus` no `PaymentRepository`
