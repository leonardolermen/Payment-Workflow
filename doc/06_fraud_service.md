# Fraud Service - Plano de Implementação

## Objetivo
Analisar as tentativas de pagamento *on the fly*, buscando por padrões anormais para aprovar ou declinar precocemente a transação.

## Especificações Técnicas
- **Tecnologia**: Spring Boot.
- **Armazenamento**: PostgreSQL (para guardar score history ou regras persistidas) + Redis (Cache de blacklist).

## O que e como Analisa?
O `Payment Service` faz uma chamada REST Síncrona POST `/fraud/analyze` enviando o modelo unificado de pagamento.
O serviço aplica um Rules Engine simples (podendo evoluir para Drools ou IA futuramente).

**Gatilhos de Rejeição e Score alto (Fraud):**
1. O Payment method pertence a uma Flag de usuário suspeito.
2. Frequência de transações: O usuário já tentou X transações nos últimos 3 minutos em sequência (rate tracking usando Redis).
3. Valor muito desproporcional.

## Retorno
Se *Score* > limite tolerância, devolve `Status = REJECTED`. Caso contrário, `Status = APPROVED`.

O `Payment Service` atua baseando nesta resposta para continuar o fluxo (ou publicar no Kafka) ou droppar na mesma hora notificando o end-user no client HTTP.
