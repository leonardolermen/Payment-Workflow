# Auth Service - Plano de Implementação

## Objetivo
Gerenciar toda a política de Autenticação e Autorização do PayFlow, além de distribuir e validar tokens.

## Especificações Técnicas
- **Tecnologia**: Spring Boot + Spring Security.
- **Autenticação**: Stateless, via JWT (JSON Web Token) acompanhado de Refresh Tokens (guardados possivelmente no Redis para fácil revogação).

## Endpoints Principais
1. `POST /auth/register` (se não for no User Service) ou delega integração para o UserService via Mensageria / Rest na hora do singup.
2. `POST /auth/login` - Recebe e-mail/senha, valida e devolve access_token e refresh_token.
3. `POST /auth/refresh` - Emite novo access_token se o refresh_token não estiver expirado.

## Responsabilidades e Integrações
1. **Banco de Dados Relacional**: Persistir credenciais encriptadas (Bcrypt). (Pode ser integrado na mesma base do UserService ou ter base própria para Credenciais puras).
2. **Fornecimento de Chaves Públicas/Segredos HMAC**: Centraliza a lógica de emissão JWT. Os outros microsserviços devem validar o JWT em solicitações que não passarem pelo middleware.

## Plano de Execução Breve
1. Subir a estrutura Spring Security + JWT.
2. Criar tabela de Roles/Authorities e tabela local para Auth.
3. Expor os endpoints rest de token provider.
