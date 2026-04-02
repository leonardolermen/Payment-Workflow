# API Gateway - Plano de Implementação

## Objetivo
Atuar como único ponto de entrada para o ecossistema do PayFlow. Responsável por esconder a topologia interna.

## Especificações Técnicas
- **Tecnologia Base**: Spring Boot + Spring Cloud Gateway
- **Porta Padrão sugerida**: 8080

## Responsabilidades
1. **Roteamento Inteligente**: Redirecionar requisições para o backend correto (ex: `/auth/**` para o Auth Service, `/payments/**` para Payment Service).
2. **Rate Limiting (Possível melhoria)**: Implementar limite de requisições por IP utilizando suporte a Redis nativo do Spring Cloud Gateway para evitar ataques DDoS.
3. **Filtro de Log e Observabilidade**: Repassar trace IDs se utilizar OpenTelemetry em requisições de cabeçalho para seguir a cadeia do request.
4. **CORS Configuration**: Concentrar regras de origens e métodos HTTP (GET, POST, OPTIONS, etc).

## Plano de Execução Breve
1. Criar microserviço base.
2. Configurar rotas e *path-rewrites* no arquivo `application.yml`.
3. Associar Circuit Breaker/Timeouts nas rotas de microsserviços demorados.
