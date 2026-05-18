# Notification Service - Plano de Implementação

## Objetivo
Encarregar-se da parte fundamental de comunicação externa, avisando os stakeholders que o fluxo do microserviço e saga se concluiu.

## Especificações Técnicas
- **Tecnologia**: Spring Boot + Spring Email (JavaMail) e Clients HTTP (WebClient/RestTemplate) + Kafka Consumer.

## Responsabilidade do Fluxo
1. Consumer ouve o tópico final `TransactionCompleted`.
2. Prepara um e-mail HTML sumarizado informando sobre o comprovante da transação simulando disparo SMTP.
3. **Webhooks Externos**: No padrão Stripe, muitas empresas usam essa API e enviam uma URL de callback (webhook) durante a criação da account da plataforma. Este serviço vai realizar POST na URL remota registrada caso ela exista. Ex:
`POST https://loja-cliente.com/webhooks/payflow` com corpo do Payment concluído.

## Desafios (Retentativas)
Se uma chamada de Webhook externo falhar pela rede da empresa de fora instável, este serviço deve alavancar o Retries. Resilience4j é aplicável aqui, junto a uma fila programada (DLQ) para que o webhook não se perca até exaurir.
