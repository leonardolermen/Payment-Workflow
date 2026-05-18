# User Service - Plano de Implementação

## Objetivo
Gerenciar as informações cruciais e perfis dos usuários da plataforma, mantendo seus dados cadastrais, documentação (CPFs/CNPJs) e saldos/status.

## Especificações Técnicas
- **Tecnologia**: Spring Web + Spring Data JPA.
- **Banco de Dados**: PostgreSQL (Table: `users`).

## Responsabilidades
1. **Gerenciamento de Perfis**: Cadastro, Atualização (PUT), Remoção (DELETE Lógico - "Active: false") de usuários.
2. **Fornecimento de Sincronia de Dados**: Quando um pagamento for ser estipulado, o `Payment Service` fará um request via REST Template / Feign Client para `User Service` afim de validar dados demográficos e limites.

## Endpoints Sugeridos
- `GET /users/{id}`: Detalhes da conta (Uso interno na rede ou externo autorizado).
- `POST /users`: Criação de perfil associada ao onboarding.
- `PUT /users/{id}`: Atualização de cadastro.

## Plano de Execução Breve
1. Modelar a entidade `User` com validações rigídas (Jakarta Validation).
2. Criar Crud via Repository Pattern.
3. Expor e documentar API Rest.
