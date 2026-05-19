# [ ] Funcionalidades Futuras

Funcionalidades planejadas para implementação futura (prioridade baixa).

---

## 4.1 Kafka - Event-Driven Architecture

- [ ] Criar `KafkaTopicsConfig.java` com beans `NewTopic`
- [ ] Criar `PaymentEventProducer` no core-service
- [ ] Criar `PaymentEvent` DTO no commons
- [ ] Refatorar `POST /payments` para assíncrono (PROCESSING + Kafka)
- [ ] Criar `PaymentRequestedConsumer` no fraud-service
- [ ] Criar `FraudResultProducer` no fraud-service
- [ ] Criar `FraudCompletedConsumer` no core-service

---

## 4.2 Notificações

- [ ] Criar `TransactionCompletedConsumer`
- [ ] Implementar webhook simulado
- [ ] Implementar e-mail simulado (log)
- [ ] Adicionar container Mailhog no docker-compose
- [ ] Implementar retry com Spring Retry
- [ ] Implementar Dead Letter Topic
- [ ] Criar endpoint `POST /admin/dlq/replay`

---

## 4.3 Testes

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

- [ ] Adicionar SpringDoc OpenAPI (`springdoc-openapi-starter-webmvc-ui`)
- [ ] Acessível em `/swagger-ui.html` de cada serviço
- [ ] Anotar controllers com `@Operation`, `@ApiResponse`
- [ ] Configurar rota no api-gateway para Swagger unificado

---

## 4.5 Observabilidade

- [ ] Adicionar `spring-boot-starter-actuator`
- [ ] Expor `/actuator/health` em ambos os serviços
- [ ] Adicionar `micrometer-registry-prometheus` para métricas
- [ ] Adicionar container Prometheus + Grafana no docker-compose
- [ ] Dashboards: taxa de pagamentos por status, latência p99, taxa de rejeição

---

## 4.6 Listagens com Paginação

- [ ] Adicionar `GET /payments` com `Pageable`
- [ ] Adicionar `GET /users` com `Pageable`
- [ ] Adicionar `findByStatus` no `PaymentRepository`
