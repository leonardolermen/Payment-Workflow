# API Keys Management - Dev TBD

## Status: ❌ NÃO INICIADO

## Objetivo

Implementar um sistema de gerenciamento de API Keys para permitir múltiplas contas com tokens internos específicos, controle granular de acesso por rota, e capacidade de cadastrar/revogar tokens dinamicamente sem restart dos serviços.

---

## Problema Atual

O sistema atual usa um único token compartilhado (`INTERNAL_API_TOKEN`) para comunicação entre serviços, o que apresenta várias limitações:

- **Token único compartilhado**: Todos os serviços (core-service, fraud-service) usam o mesmo token
- **Sem controle granular**: Não é possível saber qual serviço está fazendo a requisição
- **Sem múltiplas contas**: Não há sistema para cadastrar diferentes tokens internos
- **Token injetado em tudo**: O `InternalTokenRelayFilter` injeta o token em TODAS as requisições, inclusive de usuários externos
- **Sem auditoria**: Não há registro de qual serviço acessou qual rota e quando
- **Sem revogação**: Para revogar um token, é necessário mudar a configuração e restartar todos os serviços
- **Sem expiração**: Tokens não expiram, representando risco de segurança

---

## Solução Proposta

Implementar um sistema de API Keys armazenadas no banco de dados, permitindo:

1. **Múltiplas API Keys**: Cada serviço ou cliente tem sua própria chave
2. **Controle granular**: Definir quais rotas cada API Key pode acessar
3. **Gerenciamento dinâmico**: Cadastrar/revogar keys sem restart
4. **Auditoria completa**: Registrar todos os acessos
5. **Expiração automática**: Keys podem ter data de expiração
6. **Validação no Gateway**: Gateway valida API Key antes de injetar token interno

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
│  │  1. RequestTracingFilter (-300)                       │  │
│  │  2. JwtAuthenticationFilter (-200)                    │  │
│  │  3. ApiKeyValidationFilter (-150) [NOVO]              │  │
│  │  4. InternalTokenRelayFilter (-100)                   │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │            ApiKeyService [NOVO]                        │  │
│  │  • validateApiKey()                                   │  │
│  │  • createApiKey()                                      │  │
│  │  • revokeApiKey()                                      │  │
│  │  • listApiKeys()                                       │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                               │
├─────────────────────────────────────────────────────────────┤
│  Database (api-gateway_db)                                  │
│  • api_keys table                                          │
│  • api_key_audit_log table                                 │
└─────────────────────────────────────────────────────────────┘
```

---

## Fluxo de Trabalho

### 1. Criação de API Key

```
Admin → Gateway (POST /admin/api-keys)
       ↓ (com JWT admin)
       ApiKeyService.createApiKey()
       ↓
       Salva no banco (hash da key)
       ↓
       Retorna key gerada (única oportunidade de ver)
```

### 2. Uso de API Key por Serviço

```
Serviço → Gateway (X-Api-Key: <key>)
         ↓
         ApiKeyValidationFilter.valida()
         ↓
         Busca key no banco
         ↓
         Verifica: ativa, não expirada, rota permitida
         ↓
         InternalTokenRelayFilter injeta X-Internal-Token
         ↓
         Serviço backend
```

### 3. Revogação de API Key

```
Admin → Gateway (DELETE /admin/api-keys/{id})
       ↓ (com JWT admin)
       ApiKeyService.revokeApiKey()
       ↓
       Marca como inativa no banco
       ↓
       Key não funciona mais
```

---

## Detalhes de Implementação

### Passo 1: Criar Tabela api_keys

**Arquivo:** `api-gateway/src/main/resources/db/migration/V1__create_api_keys_table.sql`

Criar tabela para armazenar API Keys:

```sql
CREATE TABLE api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    key_hash VARCHAR(255) NOT NULL UNIQUE,
    service_name VARCHAR(100),
    allowed_routes TEXT[], -- Rotas que podem acessar (ex: {"/api/core/**", "/api/fraud/**"})
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    expires_at TIMESTAMP,
    created_by VARCHAR(100),
    last_used_at TIMESTAMP,
    CONSTRAINT check_expiration CHECK (expires_at IS NULL OR expires_at > created_at)
);

CREATE INDEX idx_api_keys_key_hash ON api_keys(key_hash);
CREATE INDEX idx_api_keys_service_name ON api_keys(service_name);
CREATE INDEX idx_api_keys_is_active ON api_keys(is_active);
```

**Dica:** Armazenar apenas hash da key (SHA-256), nunca a key em texto

---

### Passo 2: Criar Tabela api_key_audit_log

**Arquivo:** `api-gateway/src/main/resources/db/migration/V2__create_api_key_audit_log.sql`

Criar tabela para auditoria de acessos:

```sql
CREATE TABLE api_key_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    api_key_id UUID NOT NULL REFERENCES api_keys(id),
    route VARCHAR(255) NOT NULL,
    method VARCHAR(10) NOT NULL,
    status_code INTEGER,
    success BOOLEAN NOT NULL,
    client_ip VARCHAR(45),
    trace_id VARCHAR(100),
    accessed_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_audit_log_api_key_id ON api_key_audit_log(api_key_id);
CREATE INDEX idx_audit_log_accessed_at ON api_key_audit_log(accessed_at);
```

**Dica:** Esta tabela permite rastrear quem acessou o que e quando

---

### Passo 3: Adicionar Dependência PostgreSQL

**Arquivo:** `api-gateway/pom.xml`

Adicionar dependência do PostgreSQL e Flyway:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jdbc</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

**Dica:** O gateway é WebFlux, então usar spring-boot-starter-data-jdbc (não JPA)

---

### Passo 4: Configurar DataSource no application.yml

**Arquivo:** `api-gateway/src/main/resources/application.yml`

Adicionar configuração do banco de dados:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/api_gateway_db
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
```

**Dica:** Criar banco api_gateway_db separado do core-service e fraud-service

---

### Passo 5: Criar Entidade ApiKey

**Arquivo:** `api-gateway/src/main/java/com/payflow/apigateway/model/ApiKey.java`

Criar entidade para representar API Key:

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {
    private UUID id;
    private String name;
    private String keyHash;
    private String serviceName;
    private List<String> allowedRoutes;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String createdBy;
    private LocalDateTime lastUsedAt;
}
```

**Dica:** Usar Lombok para reduzir código boilerplate

---

### Passo 6: Criar Entidade ApiKeyAuditLog

**Arquivo:** `api-gateway/src/main/java/com/payflow/apigateway/model/ApiKeyAuditLog.java`

Criar entidade para log de auditoria:

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyAuditLog {
    private UUID id;
    private UUID apiKeyId;
    private String route;
    private String method;
    private Integer statusCode;
    private boolean success;
    private String clientIp;
    private String traceId;
    private LocalDateTime accessedAt;
}
```

---

### Passo 7: Criar Repository ApiKeyRepository

**Arquivo:** `api-gateway/src/main/java/com/payflow/apigateway/repository/ApiKeyRepository.java`

Criar repository para acessar dados de API Keys:

```java
@Repository
public interface ApiKeyRepository {
    
    Optional<ApiKey> findByKeyHash(String keyHash);
    
    List<ApiKey> findAllActive();
    
    List<ApiKey> findByServiceName(String serviceName);
    
    void updateLastUsedAt(UUID id, LocalDateTime lastUsedAt);
    
    void revokeApiKey(UUID id);
    
    boolean existsByKeyHash(String keyHash);
}
```

**Dica:** Implementar usando JdbcTemplate (WebFlux não tem JPA reativo completo)

---

### Passo 8: Criar Repository ApiKeyAuditLogRepository

**Arquivo:** `api-gateway/src/main/java/com/payflow/apigateway/repository/ApiKeyAuditLogRepository.java`

Criar repository para logs de auditoria:

```java
@Repository
public interface ApiKeyAuditLogRepository {
    
    void save(ApiKeyAuditLog auditLog);
    
    List<ApiKeyAuditLog> findByApiKeyId(UUID apiKeyId, int limit);
    
    List<ApiKeyAuditLog> findByDateRange(LocalDateTime start, LocalDateTime end, int limit);
}
```

---

### Passo 9: Criar ApiKeyService

**Arquivo:** `api-gateway/src/main/java/com/payflow/apigateway/service/ApiKeyService.java`

Criar serviço para gerenciar API Keys:

```java
@Service
@RequiredArgsConstructor
public class ApiKeyService {
    
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyAuditLogRepository auditLogRepository;
    
    @Value("${api.key.prefix:pf_}")
    private String keyPrefix;
    
    public ApiKey createApiKey(CreateApiKeyRequest request) {
        String rawKey = generateApiKey();
        String keyHash = hashApiKey(rawKey);
        
        ApiKey apiKey = ApiKey.builder()
            .name(request.getName())
            .keyHash(keyHash)
            .serviceName(request.getServiceName())
            .allowedRoutes(request.getAllowedRoutes())
            .isActive(true)
            .expiresAt(request.getExpiresAt())
            .createdBy(request.getCreatedBy())
            .createdAt(LocalDateTime.now())
            .build();
        
        apiKeyRepository.save(apiKey);
        
        // Retornar a key raw apenas na criação (única oportunidade)
        return apiKey.toBuilder().keyHash(rawKey).build();
    }
    
    public Optional<ApiKey> validateApiKey(String rawKey, String route) {
        String keyHash = hashApiKey(rawKey);
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyHash(keyHash);
        
        if (apiKeyOpt.isEmpty()) {
            return Optional.empty();
        }
        
        ApiKey apiKey = apiKeyOpt.get();
        
        // Verificar se está ativa
        if (!apiKey.isActive()) {
            return Optional.empty();
        }
        
        // Verificar se não expirou
        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }
        
        // Verificar se rota é permitida
        if (!isRouteAllowed(route, apiKey.getAllowedRoutes())) {
            return Optional.empty();
        }
        
        // Atualizar lastUsedAt
        apiKeyRepository.updateLastUsedAt(apiKey.getId(), LocalDateTime.now());
        
        return Optional.of(apiKey);
    }
    
    public void revokeApiKey(UUID id) {
        apiKeyRepository.revokeApiKey(id);
    }
    
    public List<ApiKey> listApiKeys() {
        return apiKeyRepository.findAllActive();
    }
    
    private String generateApiKey() {
        return keyPrefix + UUID.randomUUID().toString().replace("-", "");
    }
    
    private String hashApiKey(String rawKey) {
        return DigestUtils.sha256Hex(rawKey);
    }
    
    private boolean isRouteAllowed(String route, List<String> allowedRoutes) {
        if (allowedRoutes == null || allowedRoutes.isEmpty()) {
            return true; // Se não tem restrição, permite tudo
        }
        
        AntPathMatcher matcher = new AntPathMatcher();
        return allowedRoutes.stream().anyMatch(pattern -> matcher.match(pattern, route));
    }
}
```

**Dica:** A key retornada no createApiKey tem o valor raw em keyHash (hack temporário)

---

### Passo 10: Criar DTO CreateApiKeyRequest

**Arquivo:** `api-gateway/src/main/java/com/payflow/apigateway/dto/CreateApiKeyRequest.java`

Criar DTO para criação de API Key:

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateApiKeyRequest {
    @NotBlank
    private String name;
    
    private String serviceName;
    
    private List<String> allowedRoutes;
    
    private LocalDateTime expiresAt;
    
    @NotBlank
    private String createdBy;
}
```

---

### Passo 11: Criar DTO ApiKeyResponse

**Arquivo:** `api-gateway/src/main/java/com/payflow/apigateway/dto/ApiKeyResponse.java`

Criar DTO para resposta de API Key:

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyResponse {
    private UUID id;
    private String name;
    private String serviceName;
    private List<String> allowedRoutes;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String createdBy;
    private LocalDateTime lastUsedAt;
    // NÃO incluir keyHash na resposta
}
```

**Dica:** Nunca retornar o hash da key em respostas de listagem

---

### Passo 12: Criar ApiKeyValidationFilter

**Arquivo:** `api-gateway/src/main/java/com/payflow/apigateway/filter/ApiKeyValidationFilter.java`

Criar filtro para validar API Key:

```java
@Slf4j
@Component
@Order(-150)
public class ApiKeyValidationFilter implements GlobalFilter, Ordered {
    
    private final ApiKeyService apiKeyService;
    private final ApiKeyAuditLogRepository auditLogRepository;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String apiKey = exchange.getRequest().getHeaders().getFirst("X-Api-Key");
        
        if (apiKey == null || apiKey.isBlank()) {
            // Se não tem API Key, continua sem validar (para usuários externos)
            return chain.filter(exchange);
        }
        
        String path = exchange.getRequest().getPath().value();
        Optional<ApiKey> apiKeyOpt = apiKeyService.validateApiKey(apiKey, path);
        
        if (apiKeyOpt.isEmpty()) {
            log.warn("gateway.apikey.invalid traceId={} path={}",
                exchange.getRequest().getHeaders().getFirst("X-Trace-Id"), path);
            
            // Logar tentativa falha
            logAudit(null, path, exchange, false, 401);
            
            return buildErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "API Key inválida ou expirada");
        }
        
        ApiKey validKey = apiKeyOpt.get();
        
        // Adicionar header com informações da API Key
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
            .header("X-Api-Key-Service", validKey.getServiceName())
            .header("X-Api-Key-Id", validKey.getId().toString())
            .build();
        
        log.debug("gateway.apikey.ok service={} path={}", validKey.getServiceName(), path);
        
        return chain.filter(exchange.mutate().request(mutatedRequest).build())
            .doFinally(signal -> {
                // Logar acesso bem-sucedido
                logAudit(validKey.getId(), path, exchange, true, 
                    exchange.getResponse().getStatusCode() != null ? 
                    exchange.getResponse().getStatusCode().value() : 0);
            });
    }
    
    private void logAudit(UUID apiKeyId, String route, ServerWebExchange exchange, boolean success, int statusCode) {
        String clientIp = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (clientIp == null && exchange.getRequest().getRemoteAddress() != null) {
            clientIp = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        
        String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
        
        ApiKeyAuditLog auditLog = ApiKeyAuditLog.builder()
            .apiKeyId(apiKeyId)
            .route(route)
            .method(exchange.getRequest().getMethod().name())
            .statusCode(statusCode)
            .success(success)
            .clientIp(clientIp)
            .traceId(traceId)
            .accessedAt(LocalDateTime.now())
            .build();
        
        auditLogRepository.save(auditLog);
    }
    
    private Mono<Void> buildErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> body = Map.of(
            "status", status.value(),
            "message", message,
            "path", exchange.getRequest().getPath().value(),
            "timestamp", LocalDateTime.now().toString()
        );
        
        DataBuffer buffer = response.bufferFactory().wrap(new ObjectMapper().writeValueAsBytes(body));
        return response.writeWith(Mono.just(buffer));
    }
    
    @Override
    public int getOrder() {
        return -150;
    }
}
```

**Dica:** Se não tiver X-Api-Key, continua normal (para usuários com JWT)

---

### Passo 13: Atualizar InternalTokenRelayFilter

**Arquivo:** `api-gateway/src/main/java/com/payflow/apigateway/filter/InternalTokenRelayFilter.java`

Modificar para injetar token apenas se tiver API Key válida:

```java
@Override
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String apiKeyService = exchange.getRequest().getHeaders().getFirst("X-Api-Key-Service");
    
    // Se não tem API Key válida, não injeta token interno
    if (apiKeyService == null || apiKeyService.isBlank()) {
        return chain.filter(exchange);
    }
    
    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
        .header(INTERNAL_TOKEN_HEADER, internalToken)
        .build();
    
    return chain.filter(exchange.mutate().request(mutatedRequest).build());
}
```

**Dica:** Token interno só é injetado para serviços com API Key válida

---

### Passo 14: Criar AdminApiKeyController

**Arquivo:** `api-gateway/src/main/java/com/payflow/apigateway/controller/AdminApiKeyController.java`

Criar controller administrativo para gerenciar API Keys:

```java
@RestController
@RequestMapping("/admin/api-keys")
@RequiredArgsConstructor
public class AdminApiKeyController {
    
    private final ApiKeyService apiKeyService;
    
    @PostMapping
    public ResponseEntity<ApiKey> createApiKey(@RequestBody CreateApiKeyRequest request) {
        ApiKey apiKey = apiKeyService.createApiKey(request);
        return ResponseEntity.ok(apiKey);
    }
    
    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> listApiKeys() {
        List<ApiKey> apiKeys = apiKeyService.listApiKeys();
        List<ApiKeyResponse> responses = apiKeys.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeApiKey(@PathVariable UUID id) {
        apiKeyService.revokeApiKey(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{id}/audit")
    public ResponseEntity<List<ApiKeyAuditLog>> getAuditLogs(@PathVariable UUID id) {
        // Implementar busca de logs de auditoria
        return ResponseEntity.ok(List.of());
    }
    
    private ApiKeyResponse toResponse(ApiKey apiKey) {
        return ApiKeyResponse.builder()
            .id(apiKey.getId())
            .name(apiKey.getName())
            .serviceName(apiKey.getServiceName())
            .allowedRoutes(apiKey.getAllowedRoutes())
            .isActive(apiKey.isActive())
            .createdAt(apiKey.getCreatedAt())
            .expiresAt(apiKey.getExpiresAt())
            .createdBy(apiKey.getCreatedBy())
            .lastUsedAt(apiKey.getLastUsedAt())
            .build();
    }
}
```

**Dica:** Proteger este controller com JWT e role ADMIN

---

### Passo 15: Adicionar Rota Admin ao Gateway

**Arquivo:** `api-gateway/src/main/resources/application.yml`

Adicionar rota para endpoints administrativos:

```yaml
spring:
  cloud:
    gateway:
      routes:
        # ─── Admin endpoints (protegidos com JWT + role ADMIN) ───────────────
        - id: admin-api-keys
          uri: http://localhost:8080
          predicates:
            - Path=/admin/api-keys/**
          filters:
            - StripPrefix=0
```

**Dica:** Esta rota aponta para o próprio gateway (self-hosted admin)

---

### Passo 16: Proteger Admin Endpoints com Role

**Arquivo:** `api-gateway/src/main/java/com/payflow/apigateway/filter/JwtAuthenticationFilter.java`

Modificar para validar role ADMIN em rotas administrativas:

```java
private boolean isAdminPath(String path) {
    return PATH_MATCHER.match("/admin/**", path);
}

// No código principal, após validar token:
if (isAdminPath(path)) {
    String role = jwtService.extractRole(token); // Precisa implementar
    if (!"ADMIN".equals(role)) {
        return buildErrorResponse(exchange, HttpStatus.FORBIDDEN, "Acesso negado");
    }
}
```

**Dica:** Precisa adicionar claim role no JWT do core-service

---

### Passo 17: Criar API Key Inicial para Core Service

**Script SQL ou endpoint inicial:**

```sql
INSERT INTO api_keys (name, key_hash, service_name, allowed_routes, is_active, created_by)
VALUES (
    'Core Service',
    'sha256_hash_of_pf_core_service_key',
    'core-service',
    ARRAY['/api/core/**'],
    true,
    'system'
);
```

**Dica:** Gerar key: `pf_core_service_` + UUID, depois fazer hash SHA-256

---

### Passo 18: Criar API Key Inicial para Fraud Service

```sql
INSERT INTO api_keys (name, key_hash, service_name, allowed_routes, is_active, created_by)
VALUES (
    'Fraud Service',
    'sha256_hash_of_pf_fraud_service_key',
    'fraud-service',
    ARRAY['/api/fraud/**'],
    true,
    'system'
);
```

---

### Passo 19: Atualizar Core Service para Usar API Key

**Arquivo:** `core-service/src/main/resources/application.yml`

Adicionar configuração de API Key:

```yaml
api:
  key: ${CORE_SERVICE_API_KEY:pf_core_service_default_key}
```

**Dica:** Passar via variável de ambiente em produção

---

### Passo 20: Atualizar Fraud Service para Usar API Key

**Arquivo:** `fraud-service/src/main/resources/application.yml`

Adicionar configuração de API Key:

```yaml
api:
  key: ${FRAUD_SERVICE_API_KEY:pf_fraud_service_default_key}
```

---

### Passo 21: Configurar Feign Client para Enviar API Key

**Arquivo:** `core-service/src/main/java/com/payflow/coreservice/config/FeignConfig.java`

Adicionar interceptor para enviar API Key:

```java
@Bean
public RequestInterceptor apiKeyInterceptor(@Value("${api.key}") String apiKey) {
    return template -> {
        if (apiKey != null && !apiKey.isBlank()) {
            template.header("X-Api-Key", apiKey);
        }
    };
}
```

**Dica:** Isso faz com que chamadas Feign incluam a API Key automaticamente

---

## Estrutura de Arquivos

```
api-gateway/
├── src/main/resources/db/migration/
│   ├── V1__create_api_keys_table.sql
│   └── V2__create_api_key_audit_log.sql
├── src/main/java/com/payflow/apigateway/
│   ├── model/
│   │   ├── ApiKey.java
│   │   └── ApiKeyAuditLog.java
│   ├── repository/
│   │   ├── ApiKeyRepository.java
│   │   └── ApiKeyAuditLogRepository.java
│   ├── service/
│   │   ├── ApiKeyService.java
│   │   └── JwtService.java
│   ├── filter/
│   │   ├── RequestTracingFilter.java
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── ApiKeyValidationFilter.java [NOVO]
│   │   └── InternalTokenRelayFilter.java [ATUALIZADO]
│   ├── controller/
│   │   └── AdminApiKeyController.java [NOVO]
│   ├── dto/
│   │   ├── CreateApiKeyRequest.java [NOVO]
│   │   └── ApiKeyResponse.java [NOVO]
│   └── exception/
│       └── GatewayExceptionHandler.java
└── pom.xml [ATUALIZADO]
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
  datasource:
    url: jdbc:postgresql://localhost:5432/api_gateway_db
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
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

        # ─── Admin endpoints (self-hosted) ───────────────────────────────────
        - id: admin-api-keys
          uri: http://localhost:8080
          predicates:
            - Path=/admin/api-keys/**
          filters:
            - StripPrefix=0

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

# ─── Token interno service-to-service (legado, será removido) ─────────────────
internal-api:
  token: ${INTERNAL_API_TOKEN:dev-internal-shared-secret-change-me}

# ─── API Keys ───────────────────────────────────────────────────────────────
api:
  key:
    prefix: pf_

# ─── Rotas públicas (sem JWT) ────────────────────────────────────────────────
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

### Segurança
- **Múltiplas keys**: Cada serviço tem sua própria chave
- **Controle granular**: Keys podem acessar apenas rotas específicas
- **Expiração**: Keys podem ter data de expiração
- **Revogação**: Keys podem ser revogadas dinamicamente

### Auditoria
- **Rastreabilidade completa**: Todo acesso é logado
- **Identificação**: Sabe exatamente qual serviço acessou o que
- **Forense**: Possível investigar acessos suspeitos

### Gestão
- **Dinâmico**: Cadastrar/revogar sem restart
- **Centralizado**: Gerenciamento via API administrativa
- **Flexível**: Fácil adicionar novos serviços

---

## Testes Sugeridos

### Unit Tests
- Testar geração de API Key
- Testar hash de API Key
- Testar validação de API Key
- Testar verificação de rota permitida
- Testar expiração de API Key

### Integration Tests
- Testar criação de API Key via endpoint admin
- Testar uso de API Key em requisição
- Testar revogação de API Key
- Testar auditoria de acessos
- Testar rota não permitida

### Security Tests
- Testar acesso sem API Key
- Testar API Key inválida
- Testar API Key expirada
- Testar API Key revogada
- Testar rota não permitida para a key

---

## Considerações de Produção

### Segurança
- Usar secrets manager para armazenar API Keys dos serviços
- Rotacionar keys periodicamente
- Implementar rate limiting por API Key
- Monitorar tentativas de uso de keys inválidas

### Performance
- Cache de API Keys válidas (Redis)
- Índices no banco para consultas rápidas
- Limpeza periódica de logs de auditoria antigos

### Backup
- Backup do banco api_gateway_db
- Não é possível recuperar keys perdidas (só criar novas)

---

## Rollback

Se necessário, rollback pode ser feito por:
1. Remover ApiKeyValidationFilter do pipeline
2. Reverter InternalTokenRelayFilter para injetar token sempre
3. Remover tabelas do banco (ou marcar como inativas)
4. Remover dependências do pom.xml

**Dica:** Manter branch de backup antes de começar

---

## Observações

- **Key única**: A key raw só é retornada na criação. Perder = criar nova
- **Hash**: Nunca armazenar a key em texto, apenas hash SHA-256
- **Compatibilidade**: Mantém INTERNAL_API_TOKEN como fallback durante migração
- **Performance**: Considerar cache de keys válidas para reduzir acesso ao banco
- **Auditoria**: Logs podem crescer muito, implementar limpeza periódica
