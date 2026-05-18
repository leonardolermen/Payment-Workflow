# PayFlow — Endpoints de Autenticação

Base path: `/auth`  
Via gateway: `/api/core/auth/**`  
Autenticação: **não requer JWT**

---

## POST /auth/register

Registra um novo usuário e retorna um JWT.

**Request**
```json
{
  "name": "João Silva",
  "email": "joao@email.com",
  "password": "senha123",
  "confirmPassword": "senha123",
  "document": "123.456.789-00",
  "documentType": "CPF",
  "balance": 5000.00
}
```

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| `name` | String | ✅ | 3–100 caracteres |
| `email` | String | ✅ | formato e-mail válido |
| `password` | String | ✅ | 6–100 caracteres |
| `confirmPassword` | String | ✅ | igual a `password` |
| `document` | String | ✅ | — |
| `documentType` | Enum | ✅ | `CPF` ou `CNPJ` |
| `balance` | BigDecimal | ❌ | saldo inicial |

**Response 200**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "João Silva",
  "email": "joao@email.com",
  "expiresAt": "2026-05-19T12:00:00"
}
```

**Erros**

| Status | Motivo |
|---|---|
| `400` | Campos inválidos ou senhas não conferem |
| `409` | E-mail já cadastrado |
| `409` | Documento já cadastrado |

---

## POST /auth/login

Autentica e retorna um JWT.

**Request**
```json
{
  "email": "joao@email.com",
  "password": "senha123"
}
```

**Response 200** — mesmo formato de `/register`

**Erros**

| Status | Motivo |
|---|---|
| `400` | Campos inválidos |
| `401` | Credenciais incorretas |
| `404` | Usuário não encontrado |
