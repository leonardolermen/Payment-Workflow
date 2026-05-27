# Script para gerar várias requests ao payment-workflow para testar logs

$baseUrl = "http://localhost:8080"
$workspaceId = "ws_dev"

# Função para registrar usuário
function Register-User {
    param($email, $password)
    
    $body = @{
        email = $email
        password = $password
    } | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method POST -Body $body -ContentType "application/json"
        Write-Host "Usuário registrado: $email" -ForegroundColor Green
        return $response
    } catch {
        Write-Host "Erro ao registrar $email" -ForegroundColor Yellow
    }
}

# Função para login
function Login-User {
    param($email, $password)
    
    $body = @{
        email = $email
        password = $password
    } | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method POST -Body $body -ContentType "application/json"
        Write-Host "Login realizado: $email" -ForegroundColor Green
        return $response.token
    } catch {
        Write-Host "Erro ao fazer login $email" -ForegroundColor Yellow
        return $null
    }
}

# Função para criar pagamento
function Create-Payment {
    param($token, $amount, $cardNumber, $cardHolder, $expiry, $cvv)
    
    $body = @{
        amount = $amount
        card = @{
            number = $cardNumber
            holder = $cardHolder
            expiry = $expiry
            cvv = $cvv
        }
    } | ConvertTo-Json
    
    $headers = @{
        Authorization = "Bearer $token"
    }
    
    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/api/payments" -Method POST -Body $body -Headers $headers -ContentType "application/json"
        Write-Host "Pagamento criado: $($response.id) - Amount: $amount" -ForegroundColor Cyan
        return $response
    } catch {
        Write-Host "Erro ao criar pagamento: $_" -ForegroundColor Red
    }
}

# Função para buscar pagamento (gera logs de leitura)
function Get-Payment {
    param($token, $paymentId)
    
    $headers = @{
        Authorization = "Bearer $token"
    }
    
    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/api/payments/$paymentId" -Method GET -Headers $headers
        Write-Host "Pagamento consultado: $paymentId" -ForegroundColor Gray
        return $response
    } catch {
        Write-Host "Erro ao buscar pagamento $paymentId" -ForegroundColor Yellow
    }
}

# Função para tentar pagamento inválido (gera logs de erro)
function Create-InvalidPayment {
    param($token)
    
    $body = @{
        amount = -100  # Valor inválido
        card = @{
            number = "invalid"
            holder = "Test"
            expiry = "12/25"
            cvv = "123"
        }
    } | ConvertTo-Json
    
    $headers = @{
        Authorization = "Bearer $token"
    }
    
    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/api/payments" -Method POST -Body $body -Headers $headers -ContentType "application/json"
    } catch {
        Write-Host "Erro esperado (teste de log de erro): $_" -ForegroundColor Red
    }
}

Write-Host "`n=== Iniciando testes de logs ===`n" -ForegroundColor Magenta

# Registrar usuários
Write-Host "Registrando usuários..." -ForegroundColor Yellow
Register-User -email "test1@example.com" -password "password123"
Register-User -email "test2@example.com" -password "password123"

# Login
Write-Host "`nFazendo login..." -ForegroundColor Yellow
$token1 = Login-User -email "test1@example.com" -password "password123"
$token2 = Login-User -email "test2@example.com" -password "password123"

if (-not $token1) {
    Write-Host "Falha ao obter token. Abortando." -ForegroundColor Red
    exit 1
}

# Gerar vários pagamentos
Write-Host "`nCriando pagamentos..." -ForegroundColor Yellow
for ($i = 1; $i -le 10; $i++) {
    $amount = Get-Random -Minimum 10 -Maximum 1000
    $cardNumber = "4111111111111111"
    $cardHolder = "Test User $i"
    $expiry = "12/25"
    $cvv = "123"
    
    Create-Payment -token $token1 -amount $amount -cardNumber $cardNumber -cardHolder $cardHolder -expiry $expiry -cvv $cvv
    Start-Sleep -Milliseconds 200
}

# Gerar alguns pagamentos com o segundo usuário
Write-Host "`nCriando pagamentos com segundo usuário..." -ForegroundColor Yellow
for ($i = 1; $i -le 5; $i++) {
    $amount = Get-Random -Minimum 50 -Maximum 500
    Create-Payment -token $token2 -amount $amount -cardNumber "5555555555554444" -cardHolder "Second User" -expiry "06/26" -cvv "456"
    Start-Sleep -Milliseconds 200
}

# Gerar alguns erros (logs WARN/ERROR)
Write-Host "`nGerando erros para testar logs de erro..." -ForegroundColor Yellow
for ($i = 1; $i -le 3; $i++) {
    Create-InvalidPayment -token $token1
    Start-Sleep -Milliseconds 100
}

Write-Host "`n=== Testes concluídos ===" -ForegroundColor Magenta
Write-Host "Verifique o TraceFlow em http://localhost:5173 para ver os traces e logs" -ForegroundColor Cyan
