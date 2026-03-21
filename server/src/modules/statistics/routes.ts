import { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify'
import db from '../../database/connection.js'

// 统计概览
async function overview(request: FastifyRequest, reply: FastifyReply) {
  const userId = (request.user as any).userId
  const total = db.prepare('SELECT COUNT(*) as count FROM input_records WHERE user_id = ?').get(userId) as any
  const today = db.prepare("SELECT COUNT(*) as count FROM input_records WHERE user_id = ? AND date(timestamp/1000, 'unixepoch') = date('now')").get(userId) as any
  const apps = db.prepare('SELECT app, COUNT(*) as count FROM input_records WHERE user_id = ? GROUP BY app ORDER BY count DESC LIMIT 10').all(userId)
  return { success: true, data: { totalRecords: total?.count || 0, todayRecords: today?.count || 0, topApps: apps } }
}

// 应用统计
async function byApp(request: FastifyRequest, reply: FastifyReply) {
  const userId = (request.user as any).userId
  const apps = db.prepare('SELECT app, COUNT(*) as count FROM input_records WHERE user_id = ? GROUP BY app ORDER BY count DESC').all(userId)
  return { success: true, data: apps }
}

// 趋势统计
async function trends(request: FastifyRequest, reply: FastifyReply) {
  const userId = (request.user as any).userId
  const { days = 7 } = request.query as any
  const trends = db.prepare(`
    SELECT date(timestamp/1000, 'unixepoch') as date, COUNT(*) as count
    FROM input_records WHERE user_id = ? AND timestamp >= strftime('%s', 'now', '-${days} days') * 1000
    GROUP BY date ORDER BY date
  `).all(userId)
  return { success: true, data: trends }
}

export default async function statisticsRoutes(fastify: FastifyInstance) {
  fastify.addHook('onRequest', async (request, reply) => {
    try { await request.jwtVerify() } catch (err) { reply.send(err) }
  })
  fastify.get('/overview', overview)
  fastify.get('/by-app', byApp)
  fastify.get('/trends', trends)
}
