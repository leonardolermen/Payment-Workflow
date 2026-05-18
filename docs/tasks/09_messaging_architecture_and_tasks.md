# PayFlow - Arquitetura de Comunicação e Mensageria

Este documento detalha o padrão arquitetural de comunicação entre os microsserviços do ecosistema PayFlow, justificando as decisões com base em modelos de mercado (Big Techs) e listando as tarefas exatas necessárias para implementação.

---

## 1. O Desafio: Core Service vs Anti-Fraud

Quando um pagamento é iniciado, o `core-service` precisa validadá-lo no `fraud-service`. Existem duas abordagens principais para resolver isso na indústria:

### 1.1. Síncrono (OpenFeign / REST)
- **Como Funciona:** A API trava a requisição, chama o serviço de Fraude via HTTP, espera a resposta (`APPROVED`/`REJECTED`) e devolve a resposta final ao cliente em tempo real.
- **Vantagem:** Muito mais simples de implementar. O cliente (Front-end) já sai com o status final (Aprovado/Negado) instantaneamente.
- **Desvantagem:** Acoplamento forte. Se o servidor do antifraude estiver lento (devido a modelos complexos de Inteligência Artificial) ou cair, o pagamento será recusado (a não ser que haja um fallback rigoroso). 
- **Exemplo de Uso:** Transações de validação instantânea, como o PIX no Brasil, onde as verificações de restrições ou bloqueios por comportamento acontecem na casa dos milissegundos.

### 1.2. Assíncrono (Mensageria / Kafka) - O Padrão de Alta Escalabilidade
- **Como Funciona:** A API recebe a requisição, salva o status como `PROCESSING` no banco de dados e avisa o cliente (*"Seu pagamento foi recebido e está sendo analisado"*). Por trás dos panos, o Core publica um evento `payment-requested` no Kafka. O serviço de Fraude lê, analisa no seu próprio tempo, e publica outra mensagem: `fraud-analysis-completed`. O Core ouve e prossegue (aprovando ou travando o fluxo).
- **Vantagem:** Resiliência e tolerância a falhas. Se o antifraude cair, os pagamentos ainda são absorvidos tranquilamente pela API e a fila represa até que o serviço de fraude reinicie e comece a processar em lote.
- **Exemplo de Uso:** É assim que empresas de e-commerce e pagamentos processuais agem. Na Amazon ou Mercado Livre, a tela de checkout retorna "Pedido Concluído", e minutos depois chega o push de "Pagamento Aprovado/Recusado".

**Decisão para o PayFlow**: O padrão **Assíncrono** com Kafka traz robustez, desacoplamento e um valor imenso para composição de um portfólio maduro, sendo então a arquitetura **oficial** a partir daqui.

---

## 2. O Ecossistema do Kafka no PayFlow

A adoção de fluxos orientados a eventos (Event-Driven) não se restringe à fraude. O Kafka será vital e aplicado em todas estas passagens pelo sistema:

1. **Comunicação Segura de Decisão de Fraude (`payment-requested` & `fraud-analysis-completed`)**: Como descrito, para enviar e receber respostas da validação.
2. **Liquidação e Transferência Inter-Bancária**: O evento de efetivação desperta *workers* no Core isolados das requisições web. Eles fazem a movimentação de saldo rodando em threads em *background* com os devidos locks de base de dados.
3. **Módulo Isolado de Notificações (`transaction-completed-topic`)**: E-mails, SMS, Webhooks para sistemas externos podem ser disparados separadamente assim que uma transação vira `SUCCESS` ou `FAILED`. A requisição do cliente jamais ficará esperando um envio lento de e-mail.
4. **Filas de Mensagens Mortas (DLQ / DLT)**: Se a tentativa de bater num webhook de notificação tomar Timeout depois de vários "Retries", a comunicação será jogada em um "Tópico de Servidor Morto" para evitar que bloqueie os pagamentos atuais e permitir reinjeção no futuro.

---

## 3. Plano Tático (Task List Expandida)

Estas são as tasks altamente granulares e técnicas para a consolidação dessa arquitetura de mensageria:

### Fase A: Preparação e Startup do Message Broker
- [ ] **Configurar Conexões (`core-service` + `fraud-service`)**:
  - Adicionar a dependência Spring Kafka (`spring-kafka`) no pom.xml.
  - Implementar credenciais e host no `application.yml` (`spring.kafka.bootstrap-servers: localhost:9092`).
  - Configurar Consumer e Producer JSON (utilizando `JsonSerializer` ou Jackson serializers customizados).
- [ ] **Definição de Tópicos Automáticos (Infra via Código)**:
  - Criar no código (ex: `KafkaTopicsConfig.java`) os Beans do tipo `NewTopic` para garantir que o contêiner ao subir declare os tópicos: 
    - `payflow.payment.requested`
    - `payflow.fraud.completed`
    - `payflow.transaction.completed`

### Fase B: O Ponto de Entrada (API e Producer - Core)
- [ ] **Refatorar o Endpoint REST (`POST /payments`)**:
  - Salvar o histórico de intenção pagamento (Tabela `payments`) com `status = PROCESSING`.
  - O CoreService publicará um registro do DTO (`PaymentEvent`) no tópico `payflow.payment.requested`.
  - A API deve retornar `202 Accepted` ao cliente.

### Fase C: O Motor de Análise (Consumer e Producer - Fraud)
- [ ] **Ouvir o Pedido (Fraud Consumer)**:
  - Escrever `@KafkaListener(topics = "payflow.payment.requested", groupId = "fraud-group")` recebendo a intenção.
- [ ] **Executar Lógica Anti-Fraude**:
  - Validar e checar valores ou bloqueios mockados (ex: Se > $10.000, rejeita; Se CPF da blacklist, rejeita). Salva na tabela interna do `fraud-service`.
- [ ] **Pós Processamento (Fraud Producer)**:
  - Disparar para `payflow.fraud.completed` um DTO contendo o Status `APPROVED` ou `REJECTED`, referenciando o `payment_id` analisado.

### Fase D: Liquidação Concorrente (Consumer - Core)
- [ ] **Ouvir a Devolutiva do Fraude (Core Consumer)**:
  - `@KafkaListener(topics = "payflow.fraud.completed")`.
- [ ] **Tratar Reprovação**:
  - Se for `REJECTED`: Fazer `UPDATE` no status banco de dados para `FAILED` e enviar status final pro tópico de `transaction.completed`.
- [ ] **Tratar Execução do Saldo (Transação ACID)**:
  - Se for `APPROVED`: Fazer a buscar dos saldos do mandante e destinatário travando a linha no Postgres (`Pessimistic Lock / FOR UPDATE`). Realizar o débito e o crédito (Transação `@Transactional`).
  - Marcar a situação final como `SUCCESS` e Disparar os dados fechados e persistidos no DB em `payflow.transaction.completed`.

### Fase E: Sistemas Isolados: Notificações e Dead Letters
- [ ] **Configurar Servidor/Componente de Notificação**:
  - Novo consumer escutando `@KafkaListener(topics = "payflow.transaction.completed")`.
- [ ] **Disparar "Webhooks"**:
  - Implementar RestTemplate ou WebClient para disparar uma requisição pro back-end do Pagador informando que o boleto/fatura foi paga com sucesso.
- [ ] **Lógica de Fallback (Retry / DLQ)**:
  - Anotar o disparo por Webhook com o Spring Retry (ex: tentar até 3x, com 2 segundos de deplay).
  - Em `Recovery` Exception (se as 3x continuarem falhando 500 do servidor de destino), usar a lógica de enviar essa mensagem descartada para o tópico `payflow.notification.failed.dlq` pra não perdermos a garantia da mensageria (At-least-once semantic).
