import { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify'
import db from '../../database/connection.js'

// 获取记录列表
async function list(request: FastifyRequest, reply: FastifyReply) {
  const userId = (request.user as any).userId
  const { limit = 50, offset = 0, app, startDate, endDate } = request.query as any
  let sql = 'SELECT * FROM input_records WHERE user_id = ?'
  const params: any[] = [userId]
  if (app) { sql += ' AND app = ?'; params.push(app) }
  if (startDate) { sql += ' AND timestamp >= ?'; params.push(new Date(startDate).getTime()) }
  if (endDate) { sql += ' AND timestamp <= ?'; params.push(new Date(endDate).getTime()) }
  sql += ' ORDER BY timestamp DESC LIMIT ? OFFSET ?'
  params.push(limit, offset)
  const records = db.prepare(sql).all(...params)
  return { success: true, data: { records } }
}

// 获取单条记录
async function get(request: FastifyRequest, reply: FastifyReply) {
  const userId = (request.user as any).userId
  const { id } = request.params as any
  const record = db.prepare('SELECT * FROM input_records WHERE id = ? AND user_id = ?').get(id, userId)
  if (!record) return reply.status(404).send({ success: false, error: 'Not found' })
  return { success: true, data: record }
}

// 删除记录
async function del(request: FastifyRequest, reply: FastifyReply) {
  const userId = (request.user as any).userId
  const { id } = request.params as any
  db.prepare('DELETE FROM input_records WHERE id = ? AND user_id = ?').run(id, userId)
  return { success: true }
}

export default async function recordsRoutes(fastify: FastifyInstance) {
  fastify.addHook('onRequest', async (request, reply) => {
    try { await request.jwtVerify() } catch (err) { reply.send(err) }
  })
  fastify.get('/', list)
  fastify.get('/:id', get)
  fastify.delete('/:id', del)
}
