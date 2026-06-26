# API Gateway Implementation - Dev TBD

## Status: ⚠️ PARCIALMENTE IMPLEMENTADO

## Progresso Atual

### ✅ Implementado
- [x] Projeto api-gateway criado com Spring Cloud Gateway
- [x] Dependências configuradas (Gateway, WebFlux, Security, Redis, JWT)
- [x] Configuração de rotas básicas (core-auth, core-service, fraud-service)
- [x] JwtAuthenticationFilter implementado
- [x] RequestTracingFilter implementado
- [x] InternalTokenRelayFilter implementado
- [x] RateLimiterConfig com IP KeyResolver
- [x] GatewayExceptionHandler para tratamento de erros
- [x] CORS configurado globalmente
- [x] application.yml com configurações básicas

### ❌ Pendente
- [ ] Verificar e testar todas as rotas
- [ ] Validar autenticação JWT em todas as rotas protegidas
- [ ] Testar rate limiting com Redis
- [ ] Validar propagação de trace ID
- [ ] Testar token interno em chamadas entre serviços
- [ ] Ajustar configurações de CORS para produção
- [ ] Documentar endpoints do gateway

---

## Objetivo

Configurar o API Gateway como ponto de entrada único para o sistema PayFlow, roteando requisições para os serviços backend (core-service e fraud-service), autenticando via JWT e aplicando rate limiting básico.

---

## Problema Atual

O API Gateway já foi criado mas precisa ser validado e ajustado para funcionar corretamente como ponto de entrada do sistema, garantindo que:

- Rotas estejam configuradas corretamente
- Autenticação JWT funcione em rotas protegidas
- Rate limiting proteja contra abuso
- Tracing permita rastreabilidade
- Comunicação entre serviços funcione via token interno

---

## Solução Proposta

Validar e ajustar a configuração existente do API Gateway para garantir que ele funcione corretamente como ponto de entrada do sistema, sem adicionar complexidades extras (circuit breaker, retry, etc.) que podem ser implementadas como melhorias futuras.

---

## Arquitetura Proposta

```
┌─────────────────────────────────────────────────────────────┐
│                     API Gateway :8080                        │
│                  Spring Cloud Gateway + WebFlux              │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              Global Filters (Pipeline)                │  │
│  │  1. RequestTracingFilter (-300) [EXISTENTE]           │  │
│  │  2. JwtAuthenticationFilter (-200) [EXISTENTE]        │  │
│  │  3. InternalTokenRelayFilter (-100) [EXISTENTE]       │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                    Routes                              │  │
│  ├───────────────────────────────────────────────────────┤  │
│  │ core-auth    → core-service:8081 (pública)             │  │
│  │ core-service → core-service:8081 (JWT + Rate Limit)    │  │
│  │ fraud-service → fraud-service:8082 (JWT)                │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                               │
├─────────────────────────────────────────────────────────────┤
│  Infrastructure                                             │
│  • Redis (Rate Locking)                                    │
└─────────────────────────────────────────────────────────────┘
```

---

## Fluxo de Trabalho

### 1. Requisição Pública (Login/Registro)
```
Cliente → Gateway (/api/core/auth/**)
         ↓ (sem JWT)
         Core Service (login/register)
```

### 2. Requisição Protegida (Pagamentos)
```
Cliente → Gateway (/api/core/payments/**)
         ↓ (com JWT)
         JwtAuthenticationFilter (valida)
         ↓
         InternalTokenRelayFilter (adiciona X-Internal-Token)
         ↓
         Core Service (processa pagamento)
```

### 3. Requisição para Fraud Service
```
Cliente → Gateway (/api/fraud/**)
         ↓ (com JWT)
         JwtAuthenticationFilter (valida)
         ↓
         InternalTokenRelayFilter (adiciona X-Internal-Token)
         ↓
         Fraud Service (análise de fraude)
```

---

## Detalhes de Implementação

### Passo 1: Verificar Estrutura do Projeto

**Arquivos a verificar:**
- `api-gateway/pom.xml` - Dependências
- `api-gateway/src/main/resources/application.yml` - Configurações
- `api-gateway/src/main/java/com/payflow/apigateway/` - Código Java

**Dica:** Confirmar que todas as classes e configurações existentes estão corretas

---

### Passo 2: Validar Configuração de Rotas

**Arquivo:** `api-gateway/src/main/resources/application.yml`

Verificar se as rotas estão configuradas corretamente:

```yaml
spring:
  cloud:
    gateway:
      routes:
        # ─── Rota pública (sem JWT) ─────────────────────────────────────────
        - id: core-auth
          uri: http://localhost:8081
          predicates:
            - Path=/api/core/auth/**
          filters:
            - StripPrefix=2

        # ─── core-service (protegido com JWT) ────────────────────────────────
        - id: core-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/core/**
          filters:
            - StripPrefix=2
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 20
                redis-rate-limiter.burstCapacity: 40
                key-resolver: "#{@ipKeyResolver}"

        # ─── fraud-service (protegido com JWT) ──────────────────────────────
        - id: fraud-service
          uri: http://localhost:8082
          predicates:
            - Path=/api/fraud/**
          filters:
            - StripPrefix=1
```

**Dica:** O StripPrefix=2 remove `/api/core` e StripPrefix=1 remove `/api`

---

### Passo 3: Validar JwtAuthenticationFilter

**Arquivo:** `api-gateway/src/main/java/com/payflow/apigateway/filter/JwtAuthenticationFilter.java`

Verificar se o filtro:
- Valida token JWT em rotas protegidas
- Permite acesso a rotas públicas (/api/core/auth/**)
- Injeta header X-User-Id com o username do token
- Retorna 401 quando token é inválido ou ausente

**Dica:** O filtro deve ter ordem -200 para executar antes do InternalTokenRelayFilter

---

### Passo 4: Validar RequestTracingFilter

**Arquivo:** `api-gateway/src/main/java/com/payflow/apigateway/filter/RequestTracingFilter.java`

Verificar se o filtro:
- Gera trace ID se não existir
- Propaga trace ID via header X-Trace-Id
- Loga request e response com trace ID
- Tem ordem -300 (primeiro a executar)

**Dica:** O trace ID é essencial para rastreabilidade distribuída

---

### Passo 5: Validar InternalTokenRelayFilter

**Arquivo:** `api-gateway/src/main/java/com/payflow/apigateway/filter/InternalTokenRelayFilter.java`

Verificar se o filtro:
- Injeta header X-Internal-Token em todos os requests
- Usa o token configurado em internal-api.token
- Tem ordem -100 (executa após JWT auth)

**Dica:** Este token é usado pelos serviços backend para validar chamadas internas

---

### Passo 6: Validar RateLimiterConfig

**Arquivo:** `api-gateway/src/main/java/com/payflow/apigateway/config/RateLimiterConfig.java`

Verificar se o KeyResolver:
- Usa IP real (suporta X-Forwarded-For)
- Fallback para IP do remote address
- Retorna "unknown" se não conseguir determinar IP

**Dica:** Rate limiting requer Redis rodando

---

### Passo 7: Validar Configuração JWT

**Arquivo:** `api-gateway/src/main/resources/application.yml`

Verificar se:
- jwt.secret está configurado (deve ser igual ao do core-service)
- Usa variável de ambiente JWT_SECRET com valor padrão

```yaml
jwt:
  secret: ${JWT_SECRET:c2VjdXJlX3NlY3JldF9rZXlfZm9yX3BheWZsb3dfYXBwbGljYXRpb25fMjAyNA==}
```

**Dica:** O mesmo secret deve ser usado em todos os serviços

---

### Passo 8: Validar Configuração Token Interno

**Arquivo:** `api-gateway/src/main/resources/application.yml`

Verificar se:
- internal-api.token está configurado
- Usa variável de ambiente INTERNAL_API_TOKEN

```yaml
internal-api:
  token: ${INTERNAL_API_TOKEN:dev-internal-shared-secret-change-me}
```

**Dica:** Este token deve ser igual ao configurado nos serviços backend

---

### Passo 9: Validar Configuração Redis

**Arquivo:** `api-gateway/src/main/resources/application.yml`

Verificar se:
- Redis host e port estão configurados
- Aponta para localhost:6379 (dev) ou host de produção

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

**Dica:** Redis deve estar rodando para rate limiting funcionar

---

### Passo 10: Validar Configuração CORS

**Arquivo:** `api-gateway/src/main/resources/application.yml`

Verificar se CORS está configurado globalmente:

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOriginPatterns: "*"
            allowedMethods: "GET,POST,PUT,DELETE,OPTIONS"
            allowedHeaders: "*"
            allowCredentials: true
```

**Dica:** Em produção, restringir allowedOriginPatterns para domínios específicos

---

### Passo 11: Validar Rotas Públicas

**Arquivo:** `api-gateway/src/main/resources/application.yml`

Verificar se a lista de rotas públicas está configurada:

```yaml
gateway:
  public-paths:
    - /api/core/auth/**
```

**Dica:** Esta lista é usada pelo JwtAuthenticationFilter para pular validação JWT

---

### Passo 12: Validar GatewayExceptionHandler

**Arquivo:** `api-gateway/src/main/java/com/payflow/apigateway/exception/GatewayExceptionHandler.java`

Verificar se:
- Trata erros do gateway adequadamente
- Retorna respostas JSON consistentes
- Inclui trace ID nas respostas de erro

**Dica:** Deve ter @Order(-1) para ter prioridade alta

---

### Passo 13: Testar Rota Pública (Login)

**Teste:**
```bash
curl -X POST http://localhost:8080/api/core/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}'
```

**Esperado:** Token JWT retornado pelo core-service

**Dica:** Se funcionar, rota pública está OK

---

### Passo 14: Testar Rota Protegida sem JWT

**Teste:**
```bash
curl http://localhost:8080/api/core/payments
```

**Esperado:** 401 Unauthorized com mensagem "Token de autenticação não fornecido"

**Dica:** Se retornar 401, JWT auth está funcionando

---

### Passo 15: Testar Rota Protegida com JWT

**Teste:**
```bash
curl http://localhost:8080/api/core/payments \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

**Esperado:** Requisição roteada para core-service com header X-User-Id

**Dica:** Verificar logs do gateway para confirmar

---

### Passo 16: Testar Rota Fraud Service

**Teste:**
```bash
curl http://localhost:8080/api/fraud/analyze \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"paymentId":"uuid"}'
```

**Esperado:** Requisição roteada para fraud-service

**Dica:** Verificar se StripPrefix=1 está funcionando corretamente

---

### Passo 17: Testar Rate Limiting

**Teste:** Fazer múltiplas requisições rapidamente para uma rota protegida

**Esperado:** Após 40 requisições (burstCapacity), receber 429 Too Many Requests

**Dica:** Rate limiting usa Redis, então Redis deve estar rodando

---

### Passo 18: Testar Propagação de Trace ID

**Teste:** Fazer uma requisição e verificar logs

**Esperado:** Trace ID presente em logs do gateway e dos serviços backend

**Dica:** Trace ID deve ser o mesmo em todos os serviços

---

## Estrutura de Arquivos

```
api-gateway/
├── pom.xml
├── src/main/java/com/payflow/apigateway/
│   ├── ApiGatewayApplication.java
│   ├── config/
│   │   ├── GatewayProperties.java
│   │   ├── SecurityConfig.java
│   │   └── RateLimiterConfig.java
│   ├── filter/
│   │   ├── RequestTracingFilter.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── InternalTokenRelayFilter.java
│   ├── service/
│   │   └── JwtService.java
│   └── exception/
│       └── GatewayExceptionHandler.java
└── src/main/resources/
    └── application.yml
```

---

## Configuração application.yml

```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  data:
    redis:
      host: localhost
      port: 6379
  cloud:
    gateway:
      routes:
        # ─── Rotas públicas (sem JWT) ─────────────────────────────────────────
        - id: core-auth
          uri: http://localhost:8081
          predicates:
            - Path=/api/core/auth/**
          filters:
            - StripPrefix=2

        # ─── core-service (protegido) ─────────────────────────────────────────
        - id: core-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/core/**
          filters:
            - StripPrefix=2
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 20
                redis-rate-limiter.burstCapacity: 40
                key-resolver: "#{@ipKeyResolver}"

        # ─── fraud-service (protegido) ────────────────────────────────────────
        - id: fraud-service
          uri: http://localhost:8082
          predicates:
            - Path=/api/fraud/**
          filters:
            - StripPrefix=1

      globalcors:
        cors-configurations:
          '[/**]':
            allowedOriginPatterns: "*"
            allowedMethods: "GET,POST,PUT,DELETE,OPTIONS"
            allowedHeaders: "*"
            allowCredentials: true

# ─── JWT (mesmo secret do core-service) ─────────────────────────────────────
jwt:
  secret: ${JWT_SECRET:c2VjdXJlX3NlY3JldF9rZXlfZm9yX3BheWZsb3dfYXBwbGljYXRpb25fMjAyNA==}

# ─── Token interno service-to-service ────────────────────────────────────────
internal-api:
  token: ${INTERNAL_API_TOKEN:dev-internal-shared-secret-change-me}

# ─── Rotas públicas (sem JWT) — lista usada pelo JwtAuthenticationFilter ─────
gateway:
  public-paths:
    - /api/core/auth/**

# ─── Logging ─────────────────────────────────────────────────────────────────
logging:
  level:
    com.payflow.apigateway: INFO
    org.springframework.cloud.gateway: WARN
    org.springframework.security: WARN
```

---

## Benefícios

### Centralização
- **Ponto único de entrada**: Clientes acessam apenas o gateway
- **Roteamento transparente**: Clientes não conhecem endpoints internos

### Segurança
- **Autenticação centralizada**: JWT validado no gateway
- **Rate limiting**: Proteção contra abuso
- **Token interno**: Comunicação segura entre serviços

### Observabilidade
- **Tracing distribuído**: Trace ID propagado para todos os serviços
- **Logging centralizado**: Todos os requests passam pelo gateway

---

## Testes Sugeridos

### Testes Manuais
- Testar login via gateway (rota pública)
- Testar acesso a rota protegida sem JWT (deve retornar 401)
- Testar acesso a rota protegida com JWT válido
- Testar rate limiting (múltiplas requisições)
- Testar propagação de trace ID

### Testes de Integração
- Testar fluxo completo: login → criar pagamento → análise de fraude
- Testar comunicação entre serviços via token interno
- Testar comportamento quando Redis está down

---

## Considerações de Produção

### Variáveis de Ambiente
- `JWT_SECRET`: Deve usar secret manager (AWS Secrets Manager, Vault, etc.)
- `INTERNAL_API_TOKEN`: Deve usar secret manager
- `REDIS_HOST`: Host do Redis em produção

### CORS
- Em produção, restringir `allowedOriginPatterns` para domínios específicos
- Remover wildcard `"*"` para segurança

### HTTPS/TLS
- Configurar HTTPS no gateway em produção
- Usar certificados SSL válidos

---

## Melhorias Futuras (Não Implementar Agora)

As seguintes melhorias podem ser implementadas no futuro, mas não são necessárias agora:

- **Circuit Breaker**: Proteção contra falhas em cascata
- **Retry Logic**: Tratamento automático de falhas transitórias
- **Timeout Configuration**: Evitar requests pendentes
- **Metrics Avançadas**: Monitoramento com Prometheus/Grafana
- **Service Discovery**: Eureka/Consul para load balancing
- **Rate Limiting por Usuário**: Mais granular que por IP
- **Fallback Endpoints**: Respostas amigáveis em falhas
- **API Keys Management**: Sistema de múltiplas chaves internas (ver task 12)

---

## Rollback

Se necessário, rollback pode ser feito por:
1. Reverter alterações no application.yml
2. Remover classes Java criadas
3. Reverter para versão anterior do código

**Dica:** Manter branch de backup antes de começar

---

## Observações

- **Ordem dos Filtros**: Tracing (-300) → JWT Auth (-200) → Internal Token (-100)
- **Redis Obrigatório**: Rate limiting requer Redis rodando
- **Secret Compartilhado**: JWT_SECRET deve ser igual em todos os serviços
- **Testes em Dev**: Validar tudo em ambiente de desenvolvimento antes de produção
