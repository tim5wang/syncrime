import Fastify, { FastifyInstance } from 'fastify'
import jwt from '@fastify/jwt'
import cors from '@fastify/cors'
import rateLimit from '@fastify/rate-limit'
import { config } from './config/index.js'
import { initDatabase } from './database/connection.js'

// 路由
import authRoutes from './modules/auth/routes.js'
import syncRoutes from './modules/sync/routes.js'
import aiRoutes from './modules/ai/routes.js'
import recordsRoutes from './modules/records/routes.js'
import statisticsRoutes from './modules/statistics/routes.js'

export async function buildApp(): Promise<FastifyInstance> {
  const fastify = Fastify({
    logger: {
      level: config.env === 'development' ? 'debug' : 'info',
      transport: config.env === 'development' 
        ? { target: 'pino-pretty', options: { colorize: true } }
        : undefined
    }
  })

  // 注册插件
  await fastify.register(jwt, { 
    secret: config.jwt.secret 
  })
  
  await fastify.register(cors, { 
    origin: true,
    credentials: true
  })

  await fastify.register(rateLimit, {
    max: config.rateLimit.max,
    timeWindow: config.rateLimit.windowMs
  })

  // 健康检查
  fastify.get('/health', async () => ({ 
    status: 'ok', 
    timestamp: new Date().toISOString(),
    version: '1.0.0'
  }))

  // API 路由
  await fastify.register(authRoutes, { prefix: '/api/v1/auth' })
  await fastify.register(syncRoutes, { prefix: '/api/v1/sync' })
  await fastify.register(aiRoutes, { prefix: '/api/v1/ai' })
  await fastify.register(recordsRoutes, { prefix: '/api/v1/records' })
  await fastify.register(statisticsRoutes, { prefix: '/api/v1/statistics' })

  // 错误处理
  fastify.setErrorHandler((error, request, reply) => {
    fastify.log.error(error)
    reply.status(error.statusCode || 500).send({
      success: false,
      error: error.message || 'Internal Server Error'
    })
  })

  return fastify
}

export async function startServer() {
  // 初始化数据库
  initDatabase()

  const app = await buildApp()

  try {
    await app.listen({ 
      port: config.server.port, 
      host: config.server.host 
    })
    console.log(`🚀 Server running on http://${config.server.host}:${config.server.port}`)
  } catch (err) {
    app.log.error(err)
    process.exit(1)
  }
}

export default buildApp