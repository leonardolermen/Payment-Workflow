/**
 * PayFlow — Script de Testes Completo
 * =====================================
 * Cenários cobertos:
 *  1. Registro de usuários (payer PF, payer PJ, payee)
 *  2. Login e captura de tokens JWT
 *  3. Pagamentos aprovados (valor normal, valor alto)
 *  4. Pagamentos rejeitados (saldo insuficiente, usuário suspeito)
 *  5. Análise manual (score médio)
 *  6. Leitura de pagamentos por ID, status, usuário
 *  7. Consulta de histórico e reviews manuais
 *
 * Pré-requisitos: Node.js. Não precisa de dependências externas.
 *
 * Como rodar:
 *   node test-payflow.js
 *
 * Porta padrão do core-service: 8081
 */

const http = require('http')

const BASE_URL  = process.env.CORE_URL  || 'http://localhost:8081'
const FRAUD_URL = process.env.FRAUD_URL || 'http://localhost:8082'

// ─── HTTP helper ─────────────────────────────────────────────────────────────

function req(method, url, body, token) {
  return new Promise((resolve, reject) => {
    const parsed  = new URL(url)
    const payload = body ? JSON.stringify(body) : null

    const opts = {
      hostname: parsed.hostname,
      port:     parsed.port || 80,
      path:     parsed.pathname + parsed.search,
      method,
      headers: {
        'Content-Type': 'application/json',
        ...(payload  ? { 'Content-Length': Buffer.byteLength(payload) } : {}),
        ...(token    ? { 'Authorization': `Bearer ${token}` }           : {}),
      },
    }

    const r = http.request(opts, (res) => {
      let data = ''
      res.on('data', d => data += d)
      res.on('end', () => {
        const traceId = res.headers['x-traceflow-trace-id'] || null
        try {
          resolve({ status: res.statusCode, body: JSON.parse(data), traceId })
        } catch {
          resolve({ status: res.statusCode, body: data, traceId })
        }
      })
    })
    r.on('error', reject)
    if (payload) r.write(payload)
    r.end()
  })
}

// ─── Logger ───────────────────────────────────────────────────────────────────

const colors = { reset:'\x1b[0m', green:'\x1b[32m', red:'\x1b[31m', yellow:'\x1b[33m', cyan:'\x1b[36m', gray:'\x1b[90m', bold:'\x1b[1m' }
const c = (color, txt) => `${colors[color]}${txt}${colors.reset}`

function step(title) {
  console.log(`\n${c('bold',c('cyan','━━━ ' + title + ' '))}\n`)
}

function log(label, result, traceId) {
  const ok = result.status < 400
  const icon = ok ? c('green', '✓') : c('red', '✗')
  const statusColor = ok ? 'green' : 'red'
  console.log(`  ${icon}  [${c(statusColor, result.status)}] ${label}`)
  if (traceId) {
    console.log(`     ${c('gray', '→ trace:')} ${c('yellow', traceId)}`)
    console.log(`     ${c('gray', '→ dashboard:')} http://localhost:5173/traces/${traceId}`)
  }
  if (!ok && result.body) {
    console.log(`     ${c('gray','error:')} ${typeof result.body === 'object' ? JSON.stringify(result.body) : result.body}`)
  }
  return result
}

function sleep(ms) { return new Promise(r => setTimeout(r, ms)) }

// ─── Test runners ─────────────────────────────────────────────────────────────

async function registerOrLogin(name, email, doc, docType, balance, password = 'senha123') {
  // Tenta logar (usuário já existe de rodada anterior)
  const loginRes = await req('POST', `${BASE_URL}/auth/login`, { email, password })
  if (loginRes.status === 200) {
    log(`Login (já existe) "${name}"`, loginRes, loginRes.traceId)
    return loginRes
  }
  // Registra novo usuário
  const r = await req('POST', `${BASE_URL}/auth/register`, {
    name, email, password, confirmPassword: password,
    document: doc, documentType: docType, balance,
  })
  log(`Registrar "${name}" (${docType})`, r, r.traceId)
  return r
}

async function login(email, password = 'senha123') {
  const r = await req('POST', `${BASE_URL}/auth/login`, { email, password })
  log(`Login "${email}"`, r, r.traceId)
  return r.body?.token || null
}

async function pay(token, payerId, payeeId, amount, description) {
  const r = await req('POST', `${BASE_URL}/payments`, {
    payerId, payeeId, amount,
    currency: 'BRL',
    description: description || `Pagamento de ${payerId} para ${payeeId}`,
  }, token)
  log(`Pagamento R$ ${amount.toFixed(2).padStart(10)} — ${description}`, r, r.traceId)
  return r
}

async function getPaymentById(token, id) {
  const r = await req('GET', `${BASE_URL}/payments/${id}`, null, token)
  log(`GET /payments/${id.slice(0,8)}...`, r, r.traceId)
  return r
}

async function getPaymentsByUser(token, userId) {
  const r = await req('GET', `${BASE_URL}/payments/users/${userId}`, null, token)
  log(`GET /payments/users/${userId.slice(0,8)}... (${Array.isArray(r.body) ? r.body.length : '?'} pagamentos)`, r, r.traceId)
  return r
}

async function getPaymentsByStatus(token, status) {
  const r = await req('GET', `${BASE_URL}/payments/status/${status}`, null, token)
  log(`GET /payments/status/${status} (${Array.isArray(r.body) ? r.body.length : '?'} pagamentos)`, r, r.traceId)
  return r
}

// ─── Main ─────────────────────────────────────────────────────────────────────

async function main() {
  console.log(c('bold', '\n🔬 PayFlow Test Suite — TraceFlow Observability Demo\n'))
  console.log(c('gray', `  Core:  ${BASE_URL}`))
  console.log(c('gray', `  Fraud: ${FRAUD_URL}`))
  console.log(c('gray', `  Dashboard: http://localhost:5173\n`))

  const paymentIds  = []
  const traceIds    = []

  // ────────────────────────────────────────────────────────────────────────────
  step('1. Criando usuários')
  // ────────────────────────────────────────────────────────────────────────────

  const rAlice   = await registerOrLogin('Alice Costa',        'alice@payflow.dev',   '111.222.333-44',     'CPF',  10000.00)
  const rBob     = await registerOrLogin('Bob Pereira',        'bob@payflow.dev',     '222.333.444-55',     'CPF',  5000.00)
  const rCarla   = await registerOrLogin('Carla Ltda',         'carla@payflow.dev',   '11.222.333/0001-44', 'CNPJ', 50000.00)
  const rDana    = await registerOrLogin('Dana Investimentos', 'dana@payflow.dev',    '33.444.555/0001-66', 'CNPJ', 100000.00)
  const rEduardo = await registerOrLogin('Eduardo Silva',      'eduardo@payflow.dev', '444.555.666-77',     'CPF',  500.00)
  const rFelipe  = await registerOrLogin('Felipe Viana',       'felipe@payflow.dev',  '555.666.777-88',     'CPF',  200.00)

  // tokens e IDs extraídos direto da resposta do registro
  const tokenAlice   = rAlice.body?.token
  const tokenBob     = rBob.body?.token
  const tokenCarla   = rCarla.body?.token
  const tokenDana    = rDana.body?.token
  const tokenEduardo = rEduardo.body?.token

  const aliceId   = rAlice.body?.userId
  const bobId     = rBob.body?.userId
  const carlaId   = rCarla.body?.userId
  const danaId    = rDana.body?.userId
  const eduardoId = rEduardo.body?.userId
  const felipeId  = rFelipe.body?.userId

  if (!tokenAlice || !aliceId || !bobId) {
    console.log(c('red', '\n  ⚠  Falha no registro/login. Verifique se o core-service está rodando na porta 8081.\n'))
    process.exit(1)
  }

  await sleep(300)

  // ────────────────────────────────────────────────────────────────────────────
  step('3. Pagamentos normais (score baixo → APPROVED)')
  // ────────────────────────────────────────────────────────────────────────────

  let r

  r = await pay(tokenAlice, aliceId, bobId, 50.00, 'Almoço entre amigos')
  if (r.body?.id) { paymentIds.push(r.body.id); if (r.traceId) traceIds.push({label:'Almoço R$50', id:r.traceId}) }
  await sleep(500)

  r = await pay(tokenAlice, aliceId, bobId, 150.00, 'Reembolso de conta de luz')
  if (r.body?.id) { paymentIds.push(r.body.id); if (r.traceId) traceIds.push({label:'Luz R$150', id:r.traceId}) }
  await sleep(500)

  r = await pay(tokenBob, bobId, aliceId, 200.00, 'Divisão de despesas')
  if (r.body?.id) { paymentIds.push(r.body.id); if (r.traceId) traceIds.push({label:'Despesas R$200', id:r.traceId}) }
  await sleep(500)

  r = await pay(tokenCarla, carlaId, bobId, 800.00, 'Pagamento de freelancer')
  if (r.body?.id) { paymentIds.push(r.body.id); if (r.traceId) traceIds.push({label:'Freelancer R$800', id:r.traceId}) }
  await sleep(500)

  r = await pay(tokenDana, danaId, aliceId, 1200.00, 'Investimento inicial')
  if (r.body?.id) { paymentIds.push(r.body.id); if (r.traceId) traceIds.push({label:'Investimento R$1200', id:r.traceId}) }
  await sleep(500)

  // ────────────────────────────────────────────────────────────────────────────
  step('4. Pagamentos de alto valor (score médio → MANUAL_ANALYSIS)')
  // ────────────────────────────────────────────────────────────────────────────

  r = await pay(tokenAlice, aliceId, carlaId, 5500.00, 'Compra de equipamentos')
  if (r.body?.id) { paymentIds.push(r.body.id); if (r.traceId) traceIds.push({label:'Equipamentos R$5500', id:r.traceId}) }
  await sleep(600)

  r = await pay(tokenDana, danaId, carlaId, 8000.00, 'Aquisição de software')
  if (r.body?.id) { paymentIds.push(r.body.id); if (r.traceId) traceIds.push({label:'Software R$8000', id:r.traceId}) }
  await sleep(600)

  r = await pay(tokenDana, danaId, bobId, 12000.00, 'Consultoria estratégica')
  if (r.body?.id) { paymentIds.push(r.body.id); if (r.traceId) traceIds.push({label:'Consultoria R$12000', id:r.traceId}) }
  await sleep(600)

  // ────────────────────────────────────────────────────────────────────────────
  step('5. Pagamentos rápidos e repetitivos (velocity → suspeito)')
  // ────────────────────────────────────────────────────────────────────────────

  // Simulando pagamentos repetidos do mesmo payer em curto espaço de tempo
  for (let i = 1; i <= 4; i++) {
    r = await pay(tokenAlice, aliceId, bobId, 99.90, `Assinatura mensal #${i}`)
    if (r.body?.id) { paymentIds.push(r.body.id); if (r.traceId) traceIds.push({label:`Assinatura #${i}`, id:r.traceId}) }
    await sleep(200)
  }

  // ────────────────────────────────────────────────────────────────────────────
  step('6. Pagamentos de PJ (CNPJ → empresa)')
  // ────────────────────────────────────────────────────────────────────────────

  r = await pay(tokenCarla, carlaId, danaId, 3500.00, 'Serviços de TI - Nota Fiscal 001')
  if (r.body?.id) { paymentIds.push(r.body.id); if (r.traceId) traceIds.push({label:'NF-001 R$3500', id:r.traceId}) }
  await sleep(500)

  r = await pay(tokenCarla, carlaId, aliceId, 2200.00, 'Consultoria jurídica')
  if (r.body?.id) { paymentIds.push(r.body.id); if (r.traceId) traceIds.push({label:'Jurídica R$2200', id:r.traceId}) }
  await sleep(500)

  // ────────────────────────────────────────────────────────────────────────────
  step('7. Pagamentos que devem FALHAR (saldo insuficiente)')
  // ────────────────────────────────────────────────────────────────────────────

  r = await pay(tokenEduardo, eduardoId, aliceId, 600.00, 'Tenta pagar mais do que tem')
  if (r.traceId) traceIds.push({label:'Sem saldo R$600', id:r.traceId})
  await sleep(500)

  r = await pay(tokenEduardo, eduardoId, bobId, 1000.00, 'Outra tentativa sem saldo')
  if (r.traceId) traceIds.push({label:'Sem saldo R$1000', id:r.traceId})
  await sleep(500)

  // ────────────────────────────────────────────────────────────────────────────
  step('8. Auto-transferência (deve rejeitar)')
  // ────────────────────────────────────────────────────────────────────────────

  r = await pay(tokenAlice, aliceId, aliceId, 500.00, 'Tentativa de auto-transferência')
  if (r.traceId) traceIds.push({label:'Auto-transferência', id:r.traceId})
  await sleep(300)

  // ────────────────────────────────────────────────────────────────────────────
  step('9. Consultando pagamentos')
  // ────────────────────────────────────────────────────────────────────────────

  if (paymentIds.length > 0) {
    await getPaymentById(tokenAlice, paymentIds[0])
    await sleep(200)
    if (paymentIds.length > 1) await getPaymentById(tokenAlice, paymentIds[1])
    await sleep(200)
  }

  await getPaymentsByUser(tokenAlice, aliceId)
  await sleep(200)
  await getPaymentsByStatus(tokenAlice, 'SUCCESS')
  await sleep(200)
  await getPaymentsByStatus(tokenAlice, 'FAILED')
  await sleep(200)
  await getPaymentsByStatus(tokenAlice, 'PENDING')

  // ────────────────────────────────────────────────────────────────────────────
  step('10. Resumo dos traces gerados')
  // ────────────────────────────────────────────────────────────────────────────

  if (traceIds.length > 0) {
    console.log(c('bold', `  ${traceIds.length} traces capturados:\n`))
    traceIds.forEach(({label, id}) => {
      console.log(`  ${c('yellow', '◉')} ${label.padEnd(30)} → ${c('cyan', `http://localhost:5173/traces/${id}`)}`)
    })
  } else {
    console.log(c('yellow', '  Nenhum trace_id capturado. Verifique se o SDK Java está instrumentado e o coletor rodando.'))
  }

  console.log(`\n${c('bold', '  Dashboard → Logs → filtre por serviço ou nível para ver a jornada completa.')}`)
  console.log(c('gray', `\n  Traces listados: http://localhost:5173/traces\n`))
}

main().catch(err => {
  console.error(c('red', `\n  Fatal: ${err.message}`))
  process.exit(1)
})
