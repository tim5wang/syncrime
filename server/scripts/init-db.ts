import { join, dirname } from 'path'
import { fileURLToPath } from 'url'
import { mkdirSync, existsSync } from 'fs'
import Database from 'better-sqlite3'

const dbPath = join(process.cwd(), 'data', 'syncrime.db')
const dbDir = dirname(dbPath)
if (!existsSync(dbDir)) mkdirSync(dbDir, { recursive: true })

const db = new Database(dbPath)
db.exec(`
  CREATE TABLE IF NOT EXISTS users (id TEXT PRIMARY KEY, email TEXT UNIQUE, password_hash TEXT, nickname TEXT, created_at DATETIME DEFAULT CURRENT_TIMESTAMP);
  CREATE TABLE IF NOT EXISTS devices (id TEXT PRIMARY KEY, user_id TEXT, device_name TEXT, last_active_at DATETIME);
  CREATE TABLE IF NOT EXISTS input_records (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id TEXT, content TEXT, app TEXT, category TEXT, tags TEXT, timestamp INTEGER, created_at DATETIME DEFAULT CURRENT_TIMESTAMP);
  CREATE TABLE IF NOT EXISTS knowledge_clips (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id TEXT, title TEXT, content TEXT, source_url TEXT, category TEXT, tags TEXT, created_at DATETIME DEFAULT CURRENT_TIMESTAMP);
  CREATE TABLE IF NOT EXISTS sync_records (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id TEXT, device_id TEXT, sync_type TEXT, record_count INTEGER, synced_at DATETIME DEFAULT CURRENT_TIMESTAMP);
  CREATE INDEX IF NOT EXISTS idx_input_records_user ON input_records(user_id);
`)
db.close()
console.log('✅ Database initialized:', dbPath)
