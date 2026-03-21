import { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify'
import db from '../../database/connection.js'

// 推送数据
async function push(request: FastifyRequest, reply: FastifyReply) {
  const userId = (request.user as any).userId
  const { records, deviceId } = request.body as any
  if (!records?.length) return { success: true, data: { syncedCount: 0 } }
  
  const insertStmt = db.prepare('INSERT INTO input_records (user_id, content, app, category, tags, timestamp) VALUES (?, ?, ?, ?, ?, ?)')
  const insertMany = db.transaction((records: any[]) => {
    for (const r of records) insertStmt.run(userId, r.content, r.app, r.category, r.tags ? JSON.stringify(r.tags) : null, r.timestamp)
  })
  insertMany(records)
  db.prepare('INSERT INTO sync_records (user_id, device_id, sync_type, record_count) VALUES (?, ?, ?, ?)').run(userId, deviceId, 'PUSH', records.length)
  return { success: true, data: { syncedCount: records.length } }
}

// 拉取数据
async function pull(request: FastifyRequest, reply: FastifyReply) {
  const userId = (request.user as any).userId
  const { lastSyncTime = 0, limit = 1000 } = request.query as any
  const records = db.prepare('SELECT * FROM input_records WHERE user_id = ? AND updated_at > ? ORDER BY timestamp ASC LIMIT ?')
    .all(userId, new Date(lastSyncTime).toISOString(), limit)
  return { success: true, data: { records } }
}

// 同步状态
async function status(request: FastifyRequest, reply: FastifyReply) {
  const userId = (request.user as any).userId
  const lastSync = db.prepare('SELECT * FROM sync_records WHERE user_id = ? ORDER BY synced_at DESC LIMIT 1').get(userId)
  const total = db.prepare('SELECT COUNT(*) as count FROM input_records WHERE user_id = ?').get(userId) as any
  return { success: true, data: { lastSync, totalRecords: total?.count || 0 } }
}

export default async function syncRoutes(fastify: FastifyInstance) {
  fastify.addHook('onRequest', async (request, reply) => {
    try { await request.jwtVerify() } catch (err) { reply.send(err) }
  })
  fastify.post('/push', push)
  fastify.get('/pull', pull)
  fastify.get('/status', status)
}
