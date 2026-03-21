import { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify'
import bcrypt from 'bcryptjs'
import { v4 as uuidv4 } from 'uuid'
import db from '../../database/connection.js'
import { config } from '../../config/index.js'

// 注册
async function register(request: FastifyRequest, reply: FastifyReply) {
  const { email, password, nickname } = request.body as any
  const existingUser = db.prepare('SELECT id FROM users WHERE email = ?').get(email)
  if (existingUser) {
    return reply.status(400).send({ success: false, error: 'Email already registered' })
  }
  const userId = uuidv4()
  const passwordHash = await bcrypt.hash(password, 10)
  db.prepare('INSERT INTO users (id, email, password_hash, nickname) VALUES (?, ?, ?, ?)')
    .run(userId, email, passwordHash, nickname || email.split('@')[0])
  const token = request.server.jwt.sign({ userId, email })
  return reply.status(201).send({ success: true, data: { userId, email, token } })
}

// 登录
async function login(request: FastifyRequest, reply: FastifyReply) {
  const { email, password } = request.body as any
  const user = db.prepare('SELECT * FROM users WHERE email = ?').get(email) as any
  if (!user || !(await bcrypt.compare(password, user.password_hash))) {
    return reply.status(401).send({ success: false, error: 'Invalid credentials' })
  }
  const token = request.server.jwt.sign({ userId: user.id, email: user.email })
  return { success: true, data: { userId: user.id, email: user.email, token } }
}

// 获取当前用户
async function getCurrentUser(request: FastifyRequest, reply: FastifyReply) {
  const userId = (request.user as any).userId
  const user = db.prepare('SELECT id, email, nickname, created_at FROM users WHERE id = ?').get(userId)
  return { success: true, data: user }
}

export default async function authRoutes(fastify: FastifyInstance) {
  fastify.post('/register', register)
  fastify.post('/login', login)
  fastify.register(async function (fastify) {
    fastify.addHook('onRequest', async (request, reply) => {
      try { await request.jwtVerify() } catch (err) { reply.send(err) }
    })
    fastify.get('/me', getCurrentUser)
  })
}
