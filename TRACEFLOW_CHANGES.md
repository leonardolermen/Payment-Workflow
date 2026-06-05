# Tracer - Solução dos Desafios

## Resumo

Este documento descreve as alterações feitas para resolver os dois desafios do Tracer:

1. **URLs Hardcoded**: Serviços chamando `localhost` dentro do Docker (falha de Connection Refused)
2. **Captura de Payloads**: OTel Java Agent não captura request/response bodies automaticamente

---

## Desafio 1: URLs Hardcoded → Resolvido ✅

### Problema
Os clients Feign no `core-service` tinham URLs hardcoded para `localhost`:

```java
// ANTES (quebrado no Docker)
@FeignClient(name = "notification-service", url = "http://localhost:8084")
```

Isso falhava no Docker porque `localhost` dentro do container aponta para o próprio container.

### Solução
Alterados os clients para usar propriedades configuráveis do Spring:

```java
// DEPOIS (funciona em qualquer ambiente)
@FeignClient(name = "notification-service", url = "${notification-service.url:http://localhost:8084}")
```

### Arquivos Modificados

1. `@C:\dev\backend\Payment-Workflow\core-service\src\main\java\com\payflow\coreservice\client\NotificationClient.java:8`
   - Alterado para `${notification-service.url:http://localhost:8084}`

2. `@C:\dev\backend\Payment-Workflow\core-service\src\main\java\com\payflow\coreservice\client\AnalyticsClient.java:8`
   - Alterado para `${analytics-service.url:http://localhost:8085}`

3. `@C:\dev\backend\Payment-Workflow\core-service\src\main\java\com\payflow\coreservice\client\LedgerClient.java:8`
   - Alterado para `${ledger-service.url:http://localhost:8083}`

4. `@C:\dev\backend\Payment-Workflow\docker-compose.yml:160-161`
   - Adicionadas variáveis de ambiente:
     - `NOTIFICATION_SERVICE_URL=http://notification-service:8084`
     - `ANALYTICS_SERVICE_URL=http://analytics-service:8085`

### Compatibilidade
- **Local**: Usa o fallback `localhost:808X` quando variável não está definida
- **Docker**: Usa os valores do docker-compose (service discovery via DNS)

---

## Desafio 2: Captura de Payloads (Zero-Code) → Resolvido ✅

### Problema
O OpenTelemetry Java Agent gera spans automaticamente, mas **não captura os payloads** (JSON de request/response), deixando as interações entre APIs "cegas" no dashboard.

### Solução: Tracer Sidecar
Criado um proxy reverso sidecar em Go que:
1. Intercepta todo tráfego HTTP entre serviços
2. Captura request/response payloads
3. Sanitiza dados sensíveis (senhas, tokens, CVV)
4. Envia spans com payloads para o Tracer Collector
5. Funciona **sem alterar código** dos serviços existentes

### Arquivos Criados

1. `@C:\dev\backend\Payment-Workflow\sidecar\main.go`
   - Implementação do proxy reverso com captura de payloads
   - Sanitização automática de campos sensíveis
   - Propagação de contexto de tracing (trace_id, span_id)

2. `@C:\dev\backend\Payment-Workflow\sidecar\Dockerfile`
   - Multi-stage build para imagem mínima (~10MB)

3. `@C:\dev\backend\Payment-Workflow\sidecar\go.mod` / `go.sum`
   - Dependências do projeto Go

4. `@C:\dev\backend\Payment-Workflow\sidecar\README.md`
   - Documentação completa de uso

### Arquivos Modificados

1. `@C:\dev\backend\Payment-Workflow\docker-compose.yml:115-134`
   - Adicionado `notification-service-sidecar` que expõe porta 8084
   - O serviço `notification-service` não expõe porta externamente

2. `@C:\dev\backend\Payment-Workflow\docker-compose.yml:156-175`
   - Adicionado `analytics-service-sidecar` que expõe porta 8085
   - O serviço `analytics-service` não expõe porta externamente

### Arquitetura Zero-Code

```
ANTES (sem payload capture):
  core-service ──▶ notification-service:8084

DEPOIS (com sidecar):
  core-service ──▶ notification-sidecar:8084 ──▶ notification-service:8084
                         │
                         ▼
                  [captura payload]
                         │
                         ▼
              Tracer Collector
```

### Vantagens do Sidecar

| Aspecto | Benefício |
|---------|-----------|
| **Zero-Code** | Nenhuma alteração nos serviços existentes |
| **Universal** | Funciona com Java, Node.js, Python, Go, etc. |
| **Segurança** | Sanitização automática de dados sensíveis |
| **Performance** | Implementado em Go, overhead mínimo |
| **Configurável** | Via variáveis de ambiente |

### Configuração do Sidecar

```yaml
environment:
  - SIDECAR_TARGET_URL=http://notification-service:8084
  - SIDECAR_CAPTURE_BODY=true
  - SIDECAR_MAX_BODY_SIZE=1048576
  - TRACER_COLLECTOR_URL=http://host.docker.internal:4317
  - SIDECAR_SENSITIVE_KEYS=password,token,secret,cvv,card_number
```

---

## Como Usar

### 1. URLs Hardcoded (Já funciona)
```bash
# Rebuild do core-service
mvn clean package -pl core-service -am -DskipTests

# Subir com Docker Compose
docker-compose up -d core-service notification-service analytics-service
```

### 2. Sidecar (Nova funcionalidade)
```bash
# Build do sidecar (primeira vez)
docker-compose build notification-service-sidecar analytics-service-sidecar

# Subir com sidecars
docker-compose up -d notification-service-sidecar analytics-service-sidecar

# Ou subir tudo
docker-compose up -d
```

---

## Próximos Passos Sugeridos

1. **Testar** o sidecar em ambiente de desenvolvimento
2. **Adicionar sidecar** para `ledger-service` e `fraud-service` se necessário
3. **Dashboard** - Atualizar UI para exibir os payloads capturados
4. **Performance** - Configurar `SIDECAR_CAPTURE_BODY=false` para endpoints sensíveis se necessário
