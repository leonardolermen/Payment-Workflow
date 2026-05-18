# PayFlow — Como Estender o Projeto

Guia prático para adicionar novos elementos seguindo os padrões estabelecidos.

---

## Novo endpoint com fluxo completo

```
1. DTO de request
   └─ Compartilhado entre serviços? → commons/dto/
   └─ Exclusivo do core-service?    → core-service/dto/

2. Entidade JPA (se necessário)
   └─ model/XyzEntity.java
   └─ model/factory/XyzFactory.java
   └─ repository/XyzRepository.java

3. DTO de response
   └─ commons/dto/ ou core-service/dto/
   └─ dto/factory/XyzResponseFactory.java

4. Builder (se a construção envolver múltiplos objetos)
   └─ builder/XyzBuilder.java

5. Service
   └─ services/XyzService.java  (@Service, @RequiredArgsConstructor)

6. Controller
   └─ controller/XyzController.java  (delega ao service, usa factory no return)

7. Testes
   └─ test/builder/XyzBuilderTest.java
   └─ test/factory/XyzFactoryTest.java
   └─ test/services/XyzServiceTest.java
```

---

## Novo status de fraude + handler

```
1. Adicionar valor ao enum Status_Fraud em commons/enums/fraud/Status_Fraud.java

2. Criar handler em strategy/handlers/NovoStatusHandler.java:
   @Component
   @RequiredArgsConstructor
   public class NovoStatusHandler implements PaymentStatusHandler {
       @Override
       public void handle(Payment payment, FraudAnalysisResponse response) {
           payment.setStatus(Enum_Payment.PENDING); // ou outro status
           paymentRepository.save(payment);
           // lógica específica
       }
   }

3. Registrar na PaymentStatusHandlerFactory:
   - Injetar NovoStatusHandler no construtor
   - Adicionar ao Map: Status_Fraud.NOVO_STATUS, novoStatusHandler

4. Nenhuma alteração no PaymentService ou nos outros handlers.
```

---

## Novo evento Kafka

```
1. Criar DTO do evento em commons/dto/ (se compartilhado com fraud-service)
   ou em core-service/dto/ (se local)

2. Criar Builder em builder/NovoEventoBuilder.java:
   public class NovoEventoBuilder {
       private NovoEventoBuilder() {}

       public static NovoEvento fromXxx(Payment payment, ...) {
           return NovoEvento.builder()
                   .campo(payment.getCampo())
                   .build();
       }
   }

3. Publicar no Handler (nunca no Service):
   NovoEvento event = NovoEventoBuilder.fromXxx(payment, ...);
   kafkaTemplate.send("payflow.novo-topico", event);

4. Consumir no AlertConsumerService:
   @KafkaListener(topics = "payflow.novo-topico", groupId = "grupo-novo")
   public void handleNovoEvento(NovoEvento event) { ... }

5. Declarar o tópico no KafkaTopicsConfig:
   @Bean
   public NewTopic novoTopico() {
       return TopicBuilder.name("payflow.novo-topico").partitions(3).replicas(1).build();
   }
```

---

## Novo Builder

```
Quando criar:
  - Construção envolve 2+ objetos de domínio
  - Há derivação de campo (string → enum, timestamp automático)
  - O mapeamento se repete em mais de um lugar

Onde criar:
  - core-service/builder/NomeBuilder.java

Estrutura mínima:
  public class NomeBuilder {
      private NomeBuilder() {}

      public static Target fromSource(Source source) {
          return Target.builder()
                  .campo(source.getCampo())
                  .build();
      }
  }

Teste obrigatório:
  - test/builder/NomeBuilderTest.java
  - Verificar todos os campos mapeados
  - Verificar campos derivados (status, timestamp, source)
```

---

## Nova Factory de entidade

```
Quando criar:
  - Dados chegando via request HTTP precisam virar entidade JPA
  - Há defaults de domínio a definir (status inicial, createdAt, etc.)

Onde criar:
  - core-service/model/factory/NomeFactory.java

Estrutura com customizer (opcional):
  public final class NomeFactory {
      private NomeFactory() {}

      public static Entidade fromRequest(RequestDTO request) {
          return Entidade.builder()
                  .campo(request.getCampo())
                  .status(StatusEnum.ATIVO)      // default
                  .createdAt(LocalDateTime.now()) // default
                  .build();
      }

      // Sobrecarga com customizer para variações
      public static Entidade fromRequest(
              RequestDTO request,
              Function<Entidade.EntidadeBuilder, Entidade.EntidadeBuilder> customizer) {
          var builder = Entidade.builder()...;
          if (customizer != null) builder = customizer.apply(builder);
          return builder.build();
      }
  }
```

---

## Checklist antes de fazer PR

- [ ] Lógica de negócio está no `service`, não no `controller` ou `builder`
- [ ] DTOs de response são criados via `factory`, não inline
- [ ] Construção de objetos de domínio está em `builder/` ou `factory/`
- [ ] Novo status de fraude tem handler isolado registrado na factory
- [ ] Eventos Kafka são construídos via `Builder` e publicados no `Handler`
- [ ] Novos `Builder/Factory` têm testes unitários
- [ ] Injeção de dependência via construtor (`@RequiredArgsConstructor`)
- [ ] Nenhuma string mágica de status — use enums
