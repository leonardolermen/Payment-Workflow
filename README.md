# 💳 PayFlow — Plataforma de Pagamentos com Microserviços

## 🚀 Visão Geral

O **PayFlow** é uma plataforma de pagamentos distribuída inspirada em soluções como Stripe, desenvolvida com **Java + Spring Boot**, utilizando arquitetura de **microserviços** e comunicação **orientada a eventos**.

O sistema permite:

* Criação e gerenciamento de pagamentos
* Processamento de transações
* Notificações via webhook/email
* Análise de fraude
* Comunicação resiliente entre serviços

---

## 🧱 Arquitetura

A aplicação segue o padrão de microserviços com:

* API Gateway como ponto de entrada
* Comunicação síncrona (REST)
* Comunicação assíncrona (Kafka)

### 📌 Diagrama

```
                        ┌──────────────────────┐
                        │     API Gateway      │
                        └─────────┬────────────┘
                                  │
         ┌────────────────────────┼────────────────────────┐
         │                        │                        │
         ▼                        ▼                        ▼
┌────────────────┐      ┌────────────────┐      ┌────────────────┐
│  Auth Service  │      │ User Service   │      │ Payment Service│
└────────────────┘      └────────────────┘      └──────┬─────────┘
                                                      │
                                                      ▼
                                             ┌────────────────┐
                                             │ Fraud Service  │
                                             └──────┬─────────┘
                                                    │
                                                    ▼
                                           ┌────────────────────┐
                                           │       Kafka        │
                                           └──────┬─────────────┘
                                                  │
                   ┌──────────────────────────────┼──────────────────────────────┐
                   ▼                              ▼                              ▼
        ┌────────────────────┐        ┌────────────────────┐        ┌────────────────────┐
        │ Transaction Service│        │ Notification Serv. │        │   Dead Letter Q    │
        └────────────────────┘        └────────────────────┘        └────────────────────┘
```

---

## 🧩 Microserviços

### 🔐 Auth Service

Responsável por autenticação e autorização.

**Funcionalidades:**

* Login / Registro
* JWT + Refresh Token
* Controle de roles

---

### 👤 User Service

Gerencia os usuários da plataforma.

**Funcionalidades:**

* CRUD de usuários
* Integração com Auth Service

---

### 💰 Payment Service

Serviço central da aplicação.

**Responsabilidades:**

* Criar pagamentos
* Validar dados
* Garantir idempotência
* Publicar eventos (`PaymentCreated`)

---

### 🧾 Transaction Service

Processa os pagamentos.

**Responsabilidades:**

* Consumir eventos
* Processar transações
* Atualizar status:

  * `PENDING`
  * `SUCCESS`
  * `FAILED`

---

### 📡 Notification Service

Responsável por comunicação externa.

**Funcionalidades:**

* Webhooks (estilo Stripe)
* Simulação de envio de e-mails

---

### 🛡️ Fraud Service

Sistema de análise de fraude.

**Regras exemplo:**

* Valor alto
* Frequência de transações
* Usuários suspeitos

---

## 🔄 Fluxo de Pagamento

1. Cliente envia requisição:

```http
POST /payments
```

2. Payment Service:

* Valida usuário
* Verifica idempotência
* Salva pagamento (`PENDING`)
* Publica evento `PaymentCreated`

3. Transaction Service:

* Consome evento
* Processa pagamento
* Publica `TransactionCompleted`

4. Notification Service:

* Envia webhook/email

---

## 🔗 Comunicação

### 🟢 Síncrona (REST)

* Payment → User
* Payment → Fraud

### 🟡 Assíncrona (Kafka)

* PaymentCreated
* TransactionCompleted

---

## ⚙️ Tecnologias

### Backend

* Java 17+
* Spring Boot
* Spring Cloud

### Mensageria

* Apache Kafka

### Banco de Dados

* PostgreSQL

### Cache

* Redis

### Infra

* Docker
* Docker Compose

---

## 🔥 Conceitos Aplicados

* Microserviços
* Event-driven architecture
* Idempotência
* Consistência eventual
* Circuit Breaker (Resilience4j)
* Retry + Dead Letter Queue
* API Gateway
* Service Discovery (opcional)

---

## 📁 Estrutura do Projeto

```
payflow/
 ├── auth-service/
 ├── user-service/
 ├── payment-service/
 ├── transaction-service/
 ├── notification-service/
 ├── fraud-service/
 ├── api-gateway/
 ├── docker-compose.yml
```

---

## 🧪 Testes

* Testes unitários (JUnit + Mockito)
* Testes de integração
* Testes de fluxo entre serviços

---

## 📊 Observabilidade (opcional)

* Prometheus (métricas)
* Grafana (dashboard)
* ELK Stack (logs)

---

## 🐳 Como rodar o projeto

```bash
docker-compose up --build
```

---

## 📬 Exemplo de requisição

```json
{
  "amount": 100.00,
  "currency": "BRL",
  "customerId": "123",
  "paymentMethod": "CREDIT_CARD"
}
```

---

## 💡 Diferenciais do Projeto

* Arquitetura escalável e resiliente
* Comunicação assíncrona com eventos
* Simulação de sistema real de pagamentos
* Estrutura semelhante a sistemas financeiros modernos

---

## 👨‍💻 Autor

Desenvolvido por [Seu Nome]

---

## 📌 Possíveis melhorias futuras

* Kubernetes
* OpenTelemetry (tracing)
* Rate limiting
* Multi-tenancy
* Integração com gateways reais

---
