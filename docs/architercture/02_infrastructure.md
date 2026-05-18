# PayFlow — Infraestrutura

## Subir o ambiente

```bash
docker-compose up -d
```

## Containers

| Container | Imagem | Porta | Descrição |
|---|---|---|---|
| `payflow-postgres` | postgres:15-alpine | 5432 | Banco de dados principal |
| `payflow-redis` | redis:7-alpine | 6379 | Cache e rate limiting |
| `payflow-zookeeper` | confluentinc/cp-zookeeper:7.4.0 | 2181 | Coordenação do Kafka |
| `payflow-kafka` | confluentinc/cp-kafka:7.4.0 | 9092 | Broker de mensagens |
| `payflow-kafka-ui` | provectuslabs/kafka-ui | 8090 | UI para monitoramento do Kafka |

## Bancos de dados

| Serviço | Database | Usuário |
|---|---|---|
| `core-service` | `payflow_db` | `postgres` |
| `fraud-service` | `fraud_db` | `postgres` |

Os bancos são criados automaticamente pelo `init-db.sql` na inicialização do container.  
As migrações de schema são gerenciadas pelo **Flyway** (`baseline-on-migrate: true`).

## Kafka UI

Acesse `http://localhost:8090` para visualizar tópicos, mensagens e consumer groups.

Cluster configurado: `payflow-cluster` apontando para `kafka:29092`.

## Variáveis de ambiente relevantes

| Variável | Padrão (dev) | Descrição |
|---|---|---|
| `INTERNAL_API_TOKEN` | `dev-internal-shared-secret-change-me` | Token compartilhado entre serviços |
| `JWT_SECRET` | secret Base64 embutido | Secret de assinatura JWT |

> Em produção, essas variáveis devem ser gerenciadas por um secret manager (AWS Secrets Manager, Vault, etc.).

## Portas locais resumidas

| Serviço | URL |
|---|---|
| API Gateway | `http://localhost:8080` |
| core-service | `http://localhost:8081` |
| fraud-service | `http://localhost:8082` |
| PostgreSQL | `localhost:5432` |
| Redis | `localhost:6379` |
| Kafka | `localhost:9092` |
| Kafka UI | `http://localhost:8090` |
