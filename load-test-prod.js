/**
 * PayFlow — Load Testing Simulando Produção
 * ============================================
 * Cenários de carga pesada:
 *  - Criação massiva de usuários (100+ usuários)
 *  - Pagamentos concorrentes (500+ transações)
 *  - Consultas intensivas (GETs simultâneos)
 *  - Mistura de cenários: sucesso, falha, análise manual
 * 
 * Pré-requisitos: Node.js
 * 
 * Como rodar:
 *   node load-test-prod.js
 * 
 * Variáveis de ambiente:
 *   CORE_URL=http://localhost:8081 (padrão)
 *   CONCURRENT_USERS=50 (padrão)
 *   PAYMENTS_PER_USER=10 (padrão)
 */

const http = require('http')

const BASE_URL = process.env.CORE_URL || 'http://localhost:8081'
const CONCURRENT_USERS = parseInt(process.env.CONCURRENT_USERS) || 50
const PAYMENTS_PER_USER = parseInt(process.env.PAYMENTS_PER_USER) || 10
const GET_REQUESTS = parseInt(process.env.GET_REQUESTS) || 200

// ─── HTTP helper ─────────────────────────────────────────────────────────────

function req(method, url, body, token) {
  return new Promise((resolve, reject) => {
    const parsed = new URL(url)
    const payload = body ? JSON.stringify(body) : null

    const opts = {
      hostname: parsed.hostname,
      port: parsed.port || 80,
      path: parsed.pathname + parsed.search,
      method,
      headers: {
        'Content-Type': 'application/json',
        ...(payload ? { 'Content-Length': Buffer.byteLength(payload) } : {}),
        ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
      },
    }

    const r = http.request(opts, (res) => {
      let data = ''
      res.on('data', d => data += d)
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, body: JSON.parse(data) })
        } catch {
          resolve({ status: res.statusCode, body: data })
        }
      })
    })
    r.on('error', reject)
    if (payload) r.write(payload)
    r.end()
  })
}

// ─── Logger ───────────────────────────────────────────────────────────────────

const colors = { reset: '\x1b[0m', green: '\x1b[32m', red: '\x1b[31m', yellow: '\x1b[33m', cyan: '\x1b[36m', gray: '\x1b[90m', bold: '\x1b[1m', magenta: '\x1b[35m' }
const c = (color, txt) => `${colors[color]}${txt}${colors.reset}`

let stats = {
  total: 0,
  success: 0,
  failed: 0,
  startTime: Date.now()
}

function logStats() {
  const elapsed = ((Date.now() - stats.startTime) / 1000).toFixed(2)
  const rps = (stats.total / elapsed).toFixed(2)
  console.log(`\r${c('cyan', '│')} Requests: ${c('bold', stats.total)} ${c('green', '✓' + stats.success)} ${c('red', '✗' + stats.failed)} ${c('gray', `| RPS: ${rps} | Elapsed: ${elapsed}s`)}   `)
}

function updateStats(status) {
  stats.total++
  if (status < 400) stats.success++
  else stats.failed++
  logStats()
}

// ─── Data generators ─────────────────────────────────────────────────────────

const firstNames = ['Ana', 'Bruno', 'Carla', 'Diego', 'Eduarda', 'Felipe', 'Gabriela', 'Henrique', 'Isabela', 'João', 'Karla', 'Lucas', 'Marina', 'Nicolas', 'Olivia', 'Pedro', 'Quenia', 'Rafael', 'Sofia', 'Thiago', 'Umberto', 'Vanessa', 'William', 'Xuxa', 'Yuri', 'Zara']
const lastNames = ['Silva', 'Santos', 'Oliveira', 'Souza', 'Lima', 'Costa', 'Pereira', 'Rodrigues', 'Almeida', 'Nascimento', 'Araújo', 'Ferreira', 'Carvalho', 'Gomes', 'Ribeiro', 'Martins', 'Rocha', 'Barbosa', 'Cardoso', 'Nunes']
const companies = ['Tech Solutions', 'Digital Corp', 'Cloud Systems', 'Data Analytics', 'Smart Services', 'Future Tech', 'Innovation Hub', 'Global Solutions', 'Prime Systems', 'Elite Services']

function randomCPF() {
  const n = () => Math.floor(Math.random() * 10)
  return `${n()}${n()}.${n()}${n()}.${n()}${n()}-${n()}${n()}`
}

function randomCNPJ() {
  const n = () => Math.floor(Math.random() * 10)
  return `${n()}${n()}.${n()}${n()}.${n()}${n()}/0001-${n()}${n()}`
}

function randomEmail(name, i) {
  return `${name.toLowerCase().replace(' ', '.')}${i}@payflow.dev`
}

function randomBalance() {
  return (Math.random() * 50000 + 1000).toFixed(2)
}

function randomAmount() {
  return (Math.random() * 5000 + 10).toFixed(2)
}

const descriptions = [
  'Pagamento de serviços', 'Transferência', 'Compra de produtos', 'Aluguel', 'Salário',
  'Consultoria', 'Freelance', 'Investimento', 'Empréstimo', 'Divisão de despesas',
  'Assinatura mensal', 'Manutenção', 'Licença de software', 'Hospedagem', 'Marketing',
  'Treinamento', 'Eventos', 'Viagem', 'Refeição', 'Transporte', 'Material de escritório',
  'Equipamentos', 'Hardware', 'Software', 'Infraestrutura', 'Segurança', 'Backup',
  'Suporte técnico', 'Desenvolvimento', 'Design', 'Testes', 'QA', 'DevOps', 'Cloud'
]

function randomDescription() {
  return descriptions[Math.floor(Math.random() * descriptions.length)]
}

// ─── User operations ─────────────────────────────────────────────────────────

async function registerUser(i) {
  const isPJ = Math.random() > 0.7
  const firstName = firstNames[Math.floor(Math.random() * firstNames.length)]
  const lastName = lastNames[Math.floor(Math.random() * lastNames.length)]
  const name = isPJ ? `${companies[Math.floor(Math.random() * companies.length)]} LTDA` : `${firstName} ${lastName}`
  const doc = isPJ ? randomCNPJ() : randomCPF()
  const docType = isPJ ? 'CNPJ' : 'CPF'
  const email = randomEmail(name.replace(' ', ''), i)
  const balance = parseFloat(randomBalance())
  const password = 'senha123'

  const r = await req('POST', `${BASE_URL}/auth/register`, {
    name, email, password, confirmPassword: password,
    document: doc, documentType: docType, balance,
  })
  updateStats(r.status)
  return { token: r.body?.token, userId: r.body?.userId, name, email, balance }
}

async function loginUser(email) {
  const r = await req('POST', `${BASE_URL}/auth/login`, { email, password: 'senha123' })
  updateStats(r.status)
  return { token: r.body?.token, userId: r.body?.userId }
}

// ─── Payment operations ──────────────────────────────────────────────────────

async function createPayment(token, payerId, payeeId, amount, description) {
  const r = await req('POST', `${BASE_URL}/payments`, {
    payerId, payeeId, amount: parseFloat(amount),
    currency: 'BRL',
    description,
  }, token)
  updateStats(r.status)
  return r
}

// ─── GET operations ─────────────────────────────────────────────────────────

async function getPaymentById(token, paymentId) {
  const r = await req('GET', `${BASE_URL}/payments/${paymentId}`, null, token)
  updateStats(r.status)
  return r
}

async function getPaymentsByUser(token, userId) {
  const r = await req('GET', `${BASE_URL}/payments/users/${userId}`, null, token)
  updateStats(r.status)
  return r
}

async function getPaymentsByStatus(token, status) {
  const r = await req('GET', `${BASE_URL}/payments/status/${status}`, null, token)
  updateStats(r.status)
  return r
}

async function getAllPayments(token) {
  const r = await req('GET', `${BASE_URL}/payments`, null, token)
  updateStats(r.status)
  return r
}

async function getUserById(token, userId) {
  const r = await req('GET', `${BASE_URL}/users/${userId}`, null, token)
  updateStats(r.status)
  return r
}

// ─── Load test scenarios ────────────────────────────────────────────────────

async function scenario1_massiveUserCreation() {
  console.log(`\n${c('bold', c('magenta', '═══ SCENARIO 1: Massive User Creation ═══'))}`)
  console.log(`${c('gray', `Creating ${CONCURRENT_USERS} users concurrently...`)}`)
  
  const userPromises = []
  for (let i = 0; i < CONCURRENT_USERS; i++) {
    userPromises.push(registerUser(i))
  }
  
  const users = await Promise.all(userPromises)
  const successfulUsers = users.filter(u => u.token)
  console.log(`\n${c('green', `✓ Created ${successfulUsers.length}/${CONCURRENT_USERS} users`)}`)
  
  return successfulUsers
}

async function scenario2_concurrentPayments(users) {
  console.log(`\n${c('bold', c('magenta', '═══ SCENARIO 2: Concurrent Payments ═══'))}`)
  console.log(`${c('gray', `Executing ${PAYMENTS_PER_USER} payments per user...`)}`)
  
  const paymentPromises = []
  const paymentIds = []
  
  for (const user of users) {
    for (let i = 0; i < PAYMENTS_PER_USER; i++) {
      const randomPayee = users[Math.floor(Math.random() * users.length)]
      if (randomPayee.userId !== user.userId) {
        const amount = randomAmount()
        const description = randomDescription()
        paymentPromises.push(
          createPayment(user.token, user.userId, randomPayee.userId, amount, description)
            .then(r => {
              if (r.body?.id) paymentIds.push(r.body?.id)
              return r
            })
        )
      }
    }
  }
  
  await Promise.all(paymentPromises)
  console.log(`\n${c('green', `✓ Executed ${paymentPromises.length} payment requests`)}`)
  console.log(`${c('gray', `✓ Generated ${paymentIds.length} payment IDs`)}`)
  
  return paymentIds
}

async function scenario3_intensiveGets(users, paymentIds) {
  console.log(`\n${c('bold', c('magenta', '═══ SCENARIO 3: Intensive GET Requests ═══'))}`)
  console.log(`${c('gray', `Executing ${GET_REQUESTS} concurrent GET requests...`)}`)
  
  const getPromises = []
  
  // GET payments by ID
  for (let i = 0; i < Math.min(GET_REQUESTS / 4, paymentIds.length); i++) {
    const randomPayment = paymentIds[Math.floor(Math.random() * paymentIds.length)]
    const randomUser = users[Math.floor(Math.random() * users.length)]
    getPromises.push(getPaymentById(randomUser.token, randomPayment))
  }
  
  // GET payments by user
  for (let i = 0; i < GET_REQUESTS / 4; i++) {
    const randomUser = users[Math.floor(Math.random() * users.length)]
    getPromises.push(getPaymentsByUser(randomUser.token, randomUser.userId))
  }
  
  // GET payments by status
  const statuses = ['SUCCESS', 'FAILED', 'PENDING', 'MANUAL_ANALYSIS']
  for (let i = 0; i < GET_REQUESTS / 4; i++) {
    const randomUser = users[Math.floor(Math.random() * users.length)]
    const randomStatus = statuses[Math.floor(Math.random() * statuses.length)]
    getPromises.push(getPaymentsByStatus(randomUser.token, randomStatus))
  }
  
  // GET all payments
  for (let i = 0; i < GET_REQUESTS / 4; i++) {
    const randomUser = users[Math.floor(Math.random() * users.length)]
    getPromises.push(getAllPayments(randomUser.token))
  }
  
  // GET user by ID
  for (let i = 0; i < GET_REQUESTS / 10; i++) {
    const randomUser = users[Math.floor(Math.random() * users.length)]
    getPromises.push(getUserById(randomUser.token, randomUser.userId))
  }
  
  await Promise.all(getPromises)
  console.log(`\n${c('green', `✓ Executed ${getPromises.length} GET requests`)}`)
}

async function scenario4_mixedWorkload(users) {
  console.log(`\n${c('bold', c('magenta', '═══ SCENARIO 4: Mixed Workload (Read/Write) ═══'))}`)
  console.log(`${c('gray', `Simulating real production mix...`)}`)
  
  const mixedPromises = []
  
  for (let i = 0; i < 100; i++) {
    const randomUser = users[Math.floor(Math.random() * users.length)]
    const randomPayee = users[Math.floor(Math.random() * users.length)]
    
    // 70% reads, 30% writes
    if (Math.random() < 0.7) {
      // Read operations
      const operation = Math.floor(Math.random() * 4)
      switch (operation) {
        case 0:
          mixedPromises.push(getPaymentsByUser(randomUser.token, randomUser.userId))
          break
        case 1:
          mixedPromises.push(getAllPayments(randomUser.token))
          break
        case 2:
          mixedPromises.push(getUserById(randomUser.token, randomUser.userId))
          break
        case 3:
          const statuses = ['SUCCESS', 'FAILED', 'PENDING']
          mixedPromises.push(getPaymentsByStatus(randomUser.token, statuses[Math.floor(Math.random() * statuses.length)]))
          break
      }
    } else {
      // Write operations (payments)
      if (randomPayee.userId !== randomUser.userId) {
        mixedPromises.push(
          createPayment(
            randomUser.token,
            randomUser.userId,
            randomPayee.userId,
            randomAmount(),
            randomDescription()
          )
        )
      }
    }
  }
  
  await Promise.all(mixedPromises)
  console.log(`\n${c('green', `✓ Executed ${mixedPromises.length} mixed operations`)}`)
}

async function scenario5_highValuePayments(users) {
  console.log(`\n${c('bold', c('magenta', '═══ SCENARIO 5: High Value Payments ═══'))}`)
  console.log(`${c('gray', `Testing fraud detection with high values...`)}`)
  
  const highValuePromises = []
  const highValueUsers = users.filter(u => u.balance > 10000).slice(0, 10)
  
  for (const user of highValueUsers) {
    for (let i = 0; i < 5; i++) {
      const randomPayee = users[Math.floor(Math.random() * users.length)]
      if (randomPayee.userId !== user.userId) {
        const highAmount = (Math.random() * 15000 + 5000).toFixed(2) // 5k to 20k
        highValuePromises.push(
          createPayment(
            user.token,
            user.userId,
            randomPayee.userId,
            highAmount,
            `High value transaction ${i+1}`
          )
        )
      }
    }
  }
  
  await Promise.all(highValuePromises)
  console.log(`\n${c('green', `✓ Executed ${highValuePromises.length} high value payments`)}`)
}

// ─── Main ─────────────────────────────────────────────────────────────────────

async function main() {
  console.log(c('bold', '\n🚀 PayFlow Production Load Test\n'))
  console.log(c('gray', `  Core URL: ${BASE_URL}`))
  console.log(c('gray', `  Concurrent Users: ${CONCURRENT_USERS}`))
  console.log(c('gray', `  Payments per User: ${PAYMENTS_PER_USER}`))
  console.log(c('gray', `  GET Requests: ${GET_REQUESTS}\n`))
  
  try {
    // Scenario 1: Create users
    const users = await scenario1_massiveUserCreation()
    
    if (users.length === 0) {
      console.log(c('red', '\n  ⚠  No users created. Check if core-service is running.\n'))
      process.exit(1)
    }
    
    // Scenario 2: Concurrent payments
    const paymentIds = await scenario2_concurrentPayments(users)
    
    // Scenario 3: Intensive GETs
    await scenario3_intensiveGets(users, paymentIds)
    
    // Scenario 4: Mixed workload
    await scenario4_mixedWorkload(users)
    
    // Scenario 5: High value payments
    await scenario5_highValuePayments(users)
    
    // Final stats
    const elapsed = ((Date.now() - stats.startTime) / 1000).toFixed(2)
    const avgRps = (stats.total / elapsed).toFixed(2)
    const successRate = ((stats.success / stats.total) * 100).toFixed(2)
    
    console.log(`\n${c('bold', c('cyan', '═══ FINAL STATISTICS ═══'))}`)
    console.log(`  ${c('bold', 'Total Requests:')} ${stats.total}`)
    console.log(`  ${c('green', 'Success:')} ${stats.success} (${successRate}%)`)
    console.log(`  ${c('red', 'Failed:')} ${stats.failed}`)
    console.log(`  ${c('bold', 'Total Time:')} ${elapsed}s`)
    console.log(`  ${c('bold', 'Average RPS:')} ${avgRps}`)
    console.log(`  ${c('bold', 'Peak RPS:')} ${(stats.total / (elapsed * 0.8)).toFixed(2)} (estimated)`)
    console.log()
    
  } catch (err) {
    console.error(c('red', `\n  Fatal: ${err.message}`))
    process.exit(1)
  }
}

main()
