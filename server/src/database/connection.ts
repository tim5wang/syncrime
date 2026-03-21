import Database from 'better-sqlite3'
import { config } from '../config/index.js'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'
import { mkdirSync, existsSync } from 'fs'

const __dirname = dirname(fileURLToPath(import.meta.url))

// 确保数据目录存在
const dbPath = config.database.path
const dbDir = join(process.cwd(), dirname(dbPath))
if (!existsSync(dbDir)) {
  mkdirSync(dbDir, { recursive: true })
}

export const db = new Database(dbPath)

// 启用外键约束
db.pragma('journal_mode = WAL')
db.pragma('foreign_keys = ON')

// 初始化数据库表
export function initDatabase() {
  db.exec(`
    -- 用户表
    CREATE TABLE IF NOT EXISTS users (
      id TEXT PRIMARY KEY,
      email TEXT UNIQUE NOT NULL,
      password_hash TEXT NOT NULL,
      nickname TEXT,
      avatar TEXT,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

    -- 设备表
    CREATE TABLE IF NOT EXISTS devices (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL,
      device_name TEXT,
      device_type TEXT,
      last_active_at DATETIME,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

    -- 输入记录表
    CREATE TABLE IF NOT EXISTS input_records (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id TEXT NOT NULL,
      session_id TEXT,
      content TEXT NOT NULL,
      app TEXT,
      category TEXT,
      tags TEXT,
      is_sensitive INTEGER DEFAULT 0,
      timestamp INTEGER NOT NULL,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

    -- 知识剪藏表
    CREATE TABLE IF NOT EXISTS knowledge_clips (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id TEXT NOT NULL,
      title TEXT NOT NULL,
      content TEXT NOT NULL,
      source_url TEXT,
      source_type TEXT,
      category TEXT,
      tags TEXT,
      summary TEXT,
      view_count INTEGER DEFAULT 0,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

    -- 同步记录表
    CREATE TABLE IF NOT EXISTS sync_records (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id TEXT NOT NULL,
      device_id TEXT NOT NULL,
      sync_type TEXT NOT NULL,
      record_count INTEGER DEFAULT 0,
      status TEXT DEFAULT 'completed',
      synced_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

    -- 创建索引
    CREATE INDEX IF NOT EXISTS idx_input_records_user ON input_records(user_id);
    CREATE INDEX IF NOT EXISTS idx_input_records_timestamp ON input_records(timestamp);
    CREATE INDEX IF NOT EXISTS idx_knowledge_clips_user ON knowledge_clips(user_id);
    CREATE INDEX IF NOT EXISTS idx_sync_records_user ON sync_records(user_id);
  `)

  console.log('✅ Database initialized successfully')
}

export default db