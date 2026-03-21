import { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify'
import axios from 'axios'
import { config } from '../../config/index.js'

const API = 'https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation'

async function callAI(messages: any[]) {
  const res = await axios.post(API, { model: 'qwen-turbo', input: { messages } }, {
    headers: { 'Authorization': `Bearer ${config.ai.dashscopeApiKey}`, 'Content-Type': 'application/json' }
  })
  return res.data.output?.text || res.data.output?.choices?.[0]?.message?.content || ''
}

// 智能续写
async function completion(request: FastifyRequest, reply: FastifyReply) {
  const { context, maxLength = 100 } = request.body as any
  const text = await callAI([
    { role: 'system', content: '你是智能写作助手，直接输出续写内容。' },
    { role: 'user', content: `续写（${maxLength}字内）：${context}` }
  ])
  return { success: true, data: { text } }
}

// 智能摘要
async function summarize(request: FastifyRequest, reply: FastifyReply) {
  const { content, maxLength = 200 } = request.body as any
  const summary = await callAI([
    { role: 'system', content: '你是摘要专家，提取关键信息。' },
    { role: 'user', content: `摘要（${maxLength}字内）：${content}` }
  ])
  return { success: true, data: { summary } }
}

// AI 对话
async function chat(request: FastifyRequest, reply: FastifyReply) {
  const { messages } = request.body as any
  const message = await callAI(messages)
  return { success: true, data: { message } }
}

export default async function aiRoutes(fastify: FastifyInstance) {
  fastify.addHook('onRequest', async (request, reply) => {
    try { await request.jwtVerify() } catch (err) { reply.send(err) }
  })
  fastify.post('/completion', completion)
  fastify.post('/summarize', summarize)
  fastify.post('/chat', chat)
}
