# User KYC Flow - Dev Higao

## Objetivo

Implementar fluxo de KYC (Know Your Customer) para novos usuários, onde ao criar um usuário ele inicia com status EM_ANALISE, é enviado via Kafka para o fraud-service passar por validações básicas, e então é enviado de volta para o core-service para liberação das transações.

---

## Problema Atual

Atualmente, novos usuários são criados com status ACTIVE imediatamente após o registro, sem qualquer validação de risco ou KYC. Isso permite que:

- Usuários mal-intencionados criem contas instantaneamente
- Não haja verificação básica de identidade
- O sistema não tenha histórico para avaliar risco de novos usuários
- Transações possam ser realizadas imediatamente sem triagem

---

## Solução Proposta

Implementar um fluxo assíncrono de KYC usando Kafka entre core-service e raud-service:

1. Usuário é criado com status EM_ANALISE
2. Evento é publicado no Kafka para o fraud-service
3. Fraud Service executa regras básicas de KYC
4. Fraud Service publica resultado de volta no Kafka
5. Core Service atualiza status do usuário (APPROVED ou REJECTED)
6. Usuário só pode realizar transações após aprovação

---

## Arquitetura Proposta

Core Service (AuthService/register) -> Kafka (payflow.user.created) -> Fraud Service (KYCAnalysis)
Fraud Service (RiskRuleEngine) -> Kafka (payflow.user.kyc.result) -> Core Service (UserConsumer/update status)

---

## Fluxo de Trabalho

### 1. Criação de Usuário (Core Service)

1. Usuário faz POST /auth/register com dados básicos
2. AuthService valida e cria usuário com status EM_ANALISE
3. Usuário é persistido no banco
4. Evento é publicado em payflow.user.created
5. JWT é gerado e retornado (usuário pode fazer login, mas não pode transacionar)

### 2. Análise KYC (Fraud Service)

1. Fraud Service recebe evento em payflow.user.created
2. KYCAnalysisService busca dados do usuário via Feign
3. RiskRuleEngine executa regras de KYC
4. Score é calculado
5. Resultado é publicado em payflow.user.kyc.result

### 3. Atualização de Status (Core Service)

1. Core Service recebe resultado em payflow.user.kyc.result
2. UserService atualiza status do usuário:
   - Se APPROVED: ACTIVE - pode transacionar normalmente
   - Se REJECTED: BLOCKED - não pode transacionar
3. Notificação é enviada ao usuário (email ou webhook)

---

## Detalhes de Implementação

### Passo 1: Adicionar Novo Status ao Enum User_Status

Adicionar status EM_ANALISE e BLOCKED ao enum User_Status no módulo commons.

**Dica:** Verificar o enum atual e adicionar os novos status

---

### Passo 2: Criar Tópicos Kafka

Criar configuração de tópicos:
- payflow.user.created - Evento de novo usuário criado
- payflow.user.kyc.result - Resultado da análise KYC

**Dica:** Usar NewTopic bean como nos outros tópicos do projeto

---

### Passo 3: Criar DTOs

Criar DTOs no módulo commons:

**UserCreatedEvent:**
- userId (UUID)
- name (String)
- email (String)
- document (String)
- documentType (String)
- createdAt (Instant)

**UserKycResultEvent:**
- userId (UUID)
- 
result (enum: APPROVED/REJECTED/MANUAL_REVIEW)
- score (int)
- 	riggeredRules (List<String>)
- reason (String)
- analyzedAt (Instant)

**Dica:** Usar Lombok para reduzir código boilerplate

---

### Passo 4: Criar Producer no Core Service

Criar componente que publica eventos de usuário criado no Kafka.

**O que fazer:**
- Injetar KafkaTemplate<String, Object>
- Publicar em payflow.user.created

**Dica:** Seguir o padrão dos outros producers do projeto

---

### Passo 5: Atualizar AuthService

Modificar o método 
egister para:
- Criar usuário com status EM_ANALISE em vez de ACTIVE
- Publicar evento de usuário criado via producer
- Gerar JWT normalmente

**O que considerar:**
- Usuário pode fazer login mesmo em EM_ANALISE
- Mas não pode realizar transações até ser aprovado

---

### Passo 6: Criar Consumer no Fraud Service

Criar consumer para receber eventos de usuário criado.

**O que fazer:**
- Anotar com @KafkaListener no tópico payflow.user.created
- Delegar para KYCAnalysisService

**Dica:** Usar o mesmo padrão do AlertConsumerService existente

---

### Passo 7: Criar KYCAnalysisService no Fraud Service

Criar serviço que orquestra a análise KYC.

**O que fazer:**
- Buscar dados do usuário via Feign (se necessário)
- Executar regras de KYC usando RiskRuleEngine
- Calcular score
- Determinar resultado baseado em threshold
- Publicar resultado via producer
- Registrar log de análise

**Thresholds sugeridos:**
- Score ≥ 70 → REJECTED
- Score 40-69 → MANUAL_REVIEW
- Score < 40 → APPROVED

---

### Passo 8: Criar Interface KYCRule

Criar interface para regras de KYC.

**Métodos:**
- String code() - Código identificador da regra
- int weight() - Peso da regra no score
- oolean matches(UserContext userContext) - Se a regra se aplica

**Dica:** Seguir o padrão da interface RiskRule existente

---

### Passo 9: Criar UserContext

Criar classe para encapsular dados do usuário para análise.

**Campos:**
- userId (UUID)
- name (String)
- email (String)
- document (String)
- documentType (String)
- createdAt (Instant)

**Dica:** Usar @Builder do Lombok

---

### Passo 10: Implementar Regras de KYC

Implementar pelo menos 5 regras básicas:

**Regra 1: Email Temporário (peso 50)**
- Detectar emails de serviços temporários (tempmail.com, guerrillamail.com, etc.)

**Regra 2: Nome Suspeito (peso 30)**
- Detectar nomes comuns de teste (test, admin, root, user, demo, etc.)

**Regra 3: Documento Inválido (peso 80)**
- Validar CPF/CNPJ usando algoritmos de validação

**Regra 4: Documento Duplicado (peso 60)**
- Verificar se já existe outro usuário com mesmo documento

**Regra 5: Email Duplicado (peso 40)**
- Verificar se já existe outro usuário com mesmo email

**Dica:** Criar cada regra como um componente @Component que implementa KYCRule

---

### Passo 11: Criar Producer no Fraud Service

Criar componente que publica resultados KYC no Kafka.

**O que fazer:**
- Injetar KafkaTemplate<String, Object>
- Publicar em payflow.user.kyc.result

---

### Passo 12: Criar Consumer no Core Service

Criar consumer para receber resultados KYC.

**O que fazer:**
- Anotar com @KafkaListener no tópico payflow.user.kyc.result
- Atualizar status do usuário baseado no resultado
- Enviar notificação ao usuário

**Status mapping:**
- APPROVED → ACTIVE
- REJECTED → BLOCKED
- MANUAL_REVIEW → EM_ANALISE (continua aguardando)

---

### Passo 13: Criar NotificationService

Criar serviço para enviar notificações aos usuários.

**O que fazer:**
- Enviar email sobre resultado do KYC
- Logar a notificação por enquanto

**Dica:** Implementar envio real de email como melhoria futura

---

### Passo 14: Atualizar PaymentService

Validar status do usuário antes de permitir transações.

**O que fazer:**
- No início de createPayment, verificar se o usuário está ACTIVE
- Se não estiver, lançar exceção informando o status atual

**Dica:** Usar o enum User_Status para validação

---

### Passo 15: Adicionar Endpoint para Consultar Status KYC

Criar endpoint para consulta de status KYC do usuário.

**Endpoint:**
- GET /users/{id}/kyc-status

**Dica:** Reutilizar o endpoint GET /users/{id} existente ou criar um específico

---

## Kafka Topics Envolvidos

### Novos Tópicos
- payflow.user.created - Evento de novo usuário criado
- payflow.user.kyc.result - Resultado da análise KYC

---

## DTOs e Enums

### Novos DTOs (commons)
- UserCreatedEvent - Evento de criação de usuário
- UserKycResultEvent - Resultado da análise KYC

### Enum Atualizado
- User_Status - Adicionar EM_ANALISE e BLOCKED

---

## Fluxo Completo Detalhado

### Cenário 1: Usuário Aprovado Automaticamente

1. Usuário faz POST /auth/register com dados válidos
2. Core Service cria usuário com status EM_ANALISE
3. Core Service publica em payflow.user.created
4. Fraud Service recebe evento
5. Fraud Service executa regras de KYC
6. Fraud Service calcula score = 15 (baixo risco)
7. Fraud Service publica em payflow.user.kyc.result com APPROVED
8. Core Service recebe resultado
9. Core Service atualiza usuário para ACTIVE
10. Core Service envia notificação ao usuário
11. Usuário pode agora realizar transações

### Cenário 2: Usuário Rejeitado

Mesmo fluxo acima, mas:
6. Fraud Service calcula score = 85 (alto risco)
7. Fraud Service publica em payflow.user.kyc.result com REJECTED
9. Core Service atualiza usuário para BLOCKED
11. Usuário não pode realizar transações

### Cenário 3: Usuário Requer Revisão Manual

Mesmo fluxo acima, mas:
6. Fraud Service calcula score = 55 (risco médio)
7. Fraud Service publica em payflow.user.kyc.result com MANUAL_REVIEW
9. Core Service mantém usuário como EM_ANALISE
10. Analista revisa manualmente via endpoint específico
11. Analista aprova/rejeita manualmente

---

## Regras de KYC Sugeridas

| Regra | Peso | Descrição |
|-------|------|-----------|
| TemporaryEmailRule | 50 | Detecta emails temporários |
| SuspiciousNameRule | 30 | Detecta nomes suspeitos (test, admin, etc.) |
| InvalidDocumentRule | 80 | Valida CPF/CNPJ |
| DuplicateDocumentRule | 60 | Detecta múltiplas contas com mesmo documento |
| DuplicateEmailRule | 40 | Detecta múltiplas contas com mesmo email |

### Thresholds
- Score ≥ 70 → REJECTED
- Score 40-69 → MANUAL_REVIEW
- Score < 40 → APPROVED

---

## Benefícios

1. **Segurança**: Nova camada de validação antes de permitir transações
2. **Prevenção de Fraude**: Detecta usuários suspeitos antes de causar danos
3. **Conformidade**: Atende requisitos de KYC/AML
4. **Escalabilidade**: Processamento assíncrono via Kafka
5. **Rastreabilidade**: Todas as análises são registradas
6. **Flexibilidade**: Fácil adicionar novas regras de KYC

---

## Testes Sugeridos

### Unit Tests
- Testar cada regra de KYC individualmente
- Testar cálculo de score e threshold
- Testar fluxo de análise KYC

### Integration Tests
- Testar fluxo completo: registro → KYC → aprovação/rejeição
- Testar retry e DLQ em caso de falha
- Testar diferentes cenários de score

---

## Considerações Adicionais

### Idempotência
- O consumer de resultado KYC deve ser idempotente
- Usar userId como chave de idempotência

### Retry e DLQ
- Configurar retry com backoff exponencial
- Configurar DLQ para mensagens que falham após retries

### Performance
- Cache de usuários duplicados para evitar chamadas repetidas ao Core Service
- Limite de tempo para análise KYC (SLA)

### Monitoramento
- Métricas: tempo de análise KYC, taxa de aprovação/rejeição
- Alertas: alta taxa de rejeição pode indicar problema nas regras

### Evolução Futura
- Integração com serviços externos de KYC (ex: bureau de crédito)
- Upload de documentos para verificação manual
- Biometria facial
- Verificação de endereço
- Score de crédito externo