# PayFlow — Visão Geral

## O que é

**PayFlow** é uma plataforma de processamento de pagamentos baseada em microsserviços. Cada pagamento passa por um pipeline controlado que envolve validação, análise antifraude, execução de transferência e notificações assíncronas.

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.2 |
| Segurança | Spring Security + JWT (HS256) |
| Comunicação síncrona | Spring Cloud OpenFeign |
| Mensageria | Apache Kafka |
| Gateway | Spring Cloud Gateway (WebFlux) |
| Banco de dados | PostgreSQL 15 |
| Cache / Rate limit | Redis 7 |
| Migrações | Flyway |
| Build | Maven (multi-módulo) |

## Módulos

| Módulo | Porta | Responsabilidade |
|---|---|---|
| `api-gateway` | 8080 | Entrada única: roteamento, JWT, rate limiting, tracing |
| `core-service` | 8081 | Auth, usuários, pagamentos, revisão manual, histórico |
| `fraud-service` | 8082 | Análise de risco, score, logs de fraude |
| `commons` | — | DTOs e enums compartilhados entre serviços |

## Índice da Documentação

| Arquivo | Conteúdo |
|---|---|
| `01_architecture.md` | Diagrama de arquitetura e comunicação entre serviços |
| `02_infrastructure.md` | Docker Compose, bancos de dados, portas |
| `03_security.md` | JWT, autenticação interna, filtros de segurança |
| `04_api_gateway.md` | Rotas, filtros, rate limiting |
| `05_endpoints_auth.md` | `POST /auth/register`, `POST /auth/login` |
| `06_endpoints_payments.md` | CRUD de pagamentos, pipeline completo |
| `07_endpoints_users.md` | Usuários, revisão manual, histórico |
| `08_endpoints_fraud.md` | Análise antifraude, logs |
| `09_kafka.md` | Tópicos, producers, consumers, DLQs |
| `10_flows.md` | Fluxos de negócio passo a passo |
| `11_models_dtos_enums.md` | Entidades, DTOs, Enums |
| `12_error_handling.md` | Tratamento de erros, formato de resposta |
