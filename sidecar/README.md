# Tracer Sidecar

A lightweight reverse proxy sidecar that enables **Zero-Code Observability** by capturing HTTP request/response payloads and forwarding them to the Tracer collector.

## Como Funciona

```
┌─────────────────┐      ┌──────────────────┐      ┌─────────────────┐
│  Core Service   │─────▶│  Sidecar Proxy   │─────▶│  Target Service │
│  (Java)         │      │  (Go)            │      │  (Node/Python)  │
└─────────────────┘      └──────────────────┘      └─────────────────┘
                                │
                                ▼
                        ┌──────────────────┐
                        │ Tracer        │
                        │ Collector        │
                        └──────────────────┘
```

O sidecar:
1. Intercepta todas as requisições HTTP
2. Captura request/response payloads (com sanitização de dados sensíveis)
3. Gera spans no formato Tracer
4. Envia para o collector automaticamente

## Configuração

### Variáveis de Ambiente

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `SIDECAR_PORT` | Porta que o sidecar escuta | `8080` |
| `SIDECAR_TARGET_URL` | URL do serviço de destino | `http://localhost:8081` |
| `TRACER_COLLECTOR_URL` | URL do collector Tracer | `http://localhost:4317` |
| `OTEL_SERVICE_NAME` | Nome do serviço nos spans | `sidecar-service` |
| `TRACER_API_KEY` | API Key para autenticação | - |
| `SIDECAR_CAPTURE_BODY` | Habilitar captura de payloads | `true` |
| `SIDECAR_MAX_BODY_SIZE` | Tamanho máximo do body (bytes) | `1048576` (1MB) |
| `SIDECAR_SENSITIVE_KEYS` | Campos sensíveis para sanitizar | `password,token,secret,...` |

## Uso no Docker Compose

### Padrão Zero-Code

Em vez de chamar o serviço diretamente, o cliente chama o sidecar:

```yaml
# Serviço original (não expõe porta externamente)
notification-service:
  build: ./notification-service
  networks:
    - payflow_net

# Sidecar (expõe a porta)
notification-service-sidecar:
  build: ./sidecar
  ports:
    - "8084:8080"
  environment:
    - SIDECAR_TARGET_URL=http://notification-service:8084
    - SIDECAR_CAPTURE_BODY=true
```

O `core-service` continua chamando `http://notification-service:8084` (que agora é o sidecar), e o sidecar encaminha para o serviço real.

## Vantagens

1. **Zero-Code**: Não requer alterações no código-fonte dos serviços
2. **Universal**: Funciona com qualquer linguagem/framework
3. **Seguro**: Sanitização automática de dados sensíveis (senhas, tokens, CVV)
4. **Leve**: Implementado em Go, uso mínimo de recursos
5. **Transparente**: Propagação automática de contexto de tracing

## Headers de Tracing

O sidecar propaga automaticamente os headers:
- `X-Tracer-Trace-Id` / `X-B3-Traceid`
- `X-Tracer-Span-Id` / `X-B3-Spanid`
- `X-B3-Parentspanid`

## Payload Capturado

```json
{
  "id": "span-uuid",
  "trace_id": "trace-uuid",
  "service_name": "notification-service",
  "operation_name": "POST /notifications/send",
  "payloads": {
    "request_body": "{\"userId\":\"123\",\"message\":\"Hello\"}",
    "response_body": "{\"status\":\"sent\"}",
    "response_status": 200
  }
}
```
