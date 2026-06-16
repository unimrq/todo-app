from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, List
import sqlite3
import uuid
import json
from datetime import datetime
import os

app = FastAPI(title="Todo API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

DB_PATH = os.path.join(os.path.dirname(__file__), "todos.db")

def get_db():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    conn.execute("""
        CREATE TABLE IF NOT EXISTS todos (
            id TEXT PRIMARY KEY,
            title TEXT NOT NULL,
            description TEXT DEFAULT '',
            status TEXT DEFAULT 'pending',
            priority TEXT DEFAULT 'medium',
            category TEXT DEFAULT '',
            due_date INTEGER,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL,
            source TEXT DEFAULT 'user',
            tags TEXT DEFAULT '',
            sync_status TEXT DEFAULT 'synced'
        )
    """)
    return conn

class TodoCreate(BaseModel):
    title: str
    description: str = ""
    priority: str = "medium"
    category: str = ""
    due_date: Optional[int] = None
    source: str = "user"
    tags: str = ""

class TodoUpdate(BaseModel):
    title: Optional[str] = None
    description: Optional[str] = None
    status: Optional[str] = None
    priority: Optional[str] = None
    category: Optional[str] = None
    due_date: Optional[int] = None

class TodoResponse(BaseModel):
    id: str
    title: str
    description: str
    status: str
    priority: str
    category: str
    due_date: Optional[int] = None
    created_at: int
    updated_at: int
    source: str
    tags: str
    sync_status: str

def row_to_dict(row):
    return {
        "id": row["id"],
        "title": row["title"],
        "description": row["description"],
        "status": row["status"],
        "priority": row["priority"],
        "category": row["category"],
        "due_date": row["due_date"],
        "created_at": row["created_at"],
        "updated_at": row["updated_at"],
        "source": row["source"],
        "tags": row["tags"],
        "sync_status": row["sync_status"]
    }

@app.get("/todos", response_model=List[TodoResponse])
def list_todos(status: str = None, priority: str = None, category: str = None):
    conn = get_db()
    query = "SELECT * FROM todos WHERE 1=1"
    params = []
    if status:
        query += " AND status = ?"
        params.append(status)
    if priority:
        query += " AND priority = ?"
        params.append(priority)
    if category:
        query += " AND category = ?"
        params.append(category)
    query += " ORDER BY CASE WHEN status='pending' THEN 0 ELSE 1 END, CASE priority WHEN 'high' THEN 0 WHEN 'medium' THEN 1 ELSE 2 END, due_date ASC"
    rows = conn.execute(query, params).fetchall()
    return [row_to_dict(r) for r in rows]

@app.get("/todos/{todo_id}", response_model=TodoResponse)
def get_todo(todo_id: str):
    conn = get_db()
    row = conn.execute("SELECT * FROM todos WHERE id = ?", (todo_id,)).fetchone()
    if not row:
        raise HTTPException(404, "Todo not found")
    return row_to_dict(row)

@app.post("/todos", response_model=TodoResponse, status_code=201)
def create_todo(todo: TodoCreate):
    conn = get_db()
    now = int(datetime.now().timestamp() * 1000)
    todo_id = str(uuid.uuid4())
    conn.execute("""
        INSERT INTO todos (id, title, description, status, priority, category, due_date, created_at, updated_at, source, tags, sync_status)
        VALUES (?, ?, ?, 'pending', ?, ?, ?, ?, ?, ?, ?, 'synced')
    """, (todo_id, todo.title, todo.description, todo.priority, todo.category, todo.due_date, now, now, todo.source, todo.tags))
    conn.commit()
    row = conn.execute("SELECT * FROM todos WHERE id = ?", (todo_id,)).fetchone()
    return row_to_dict(row)

@app.put("/todos/{todo_id}", response_model=TodoResponse)
def update_todo(todo_id: str, todo: TodoUpdate):
    conn = get_db()
    existing = conn.execute("SELECT * FROM todos WHERE id = ?", (todo_id,)).fetchone()
    if not existing:
        raise HTTPException(404, "Todo not found")
    
    now = int(datetime.now().timestamp() * 1000)
    updates = {}
    for field in ["title", "description", "status", "priority", "category", "due_date"]:
        val = getattr(todo, field, None)
        if val is not None:
            updates[field] = val
    
    if not updates:
        return row_to_dict(existing)
    
    updates["updated_at"] = now
    set_clause = ", ".join(f"{k} = ?" for k in updates)
    values = list(updates.values()) + [todo_id]
    conn.execute(f"UPDATE todos SET {set_clause} WHERE id = ?", values)
    conn.commit()
    row = conn.execute("SELECT * FROM todos WHERE id = ?", (todo_id,)).fetchone()
    return row_to_dict(row)

@app.delete("/todos/{todo_id}", status_code=204)
def delete_todo(todo_id: str):
    conn = get_db()
    conn.execute("DELETE FROM todos WHERE id = ?", (todo_id,))
    conn.commit()

@app.patch("/todos/{todo_id}/toggle", response_model=TodoResponse)
def toggle_todo(todo_id: str):
    conn = get_db()
    row = conn.execute("SELECT * FROM todos WHERE id = ?", (todo_id,)).fetchone()
    if not row:
        raise HTTPException(404, "Todo not found")
    new_status = "completed" if row["status"] == "pending" else "pending"
    now = int(datetime.now().timestamp() * 1000)
    conn.execute("UPDATE todos SET status = ?, updated_at = ? WHERE id = ?", (new_status, now, todo_id))
    conn.commit()
    row = conn.execute("SELECT * FROM todos WHERE id = ?", (todo_id,)).fetchone()
    return row_to_dict(row)

@app.get("/sync/changes")
def get_changes(since: int = 0):
    conn = get_db()
    rows = conn.execute("SELECT * FROM todos WHERE updated_at > ? ORDER BY updated_at ASC", (since,)).fetchall()
    return [row_to_dict(r) for r in rows]

@app.post("/sync/push")
def push_changes(todos: List[TodoCreate]):
    conn = get_db()
    now = int(datetime.now().timestamp() * 1000)
    for todo in todos:
        todo_id = str(uuid.uuid4())
        conn.execute("""
            INSERT OR REPLACE INTO todos (id, title, description, status, priority, category, due_date, created_at, updated_at, source, tags, sync_status)
            VALUES (?, ?, ?, 'pending', ?, ?, ?, ?, ?, ?, ?, 'synced')
        """, (todo_id, todo.title, todo.description, todo.priority, todo.category, todo.due_date, now, now, todo.source, todo.tags))
    conn.commit()
    return {"status": "ok", "count": len(todos)}

@app.get("/sync/status")
def sync_status():
    conn = get_db()
    last = conn.execute("SELECT MAX(updated_at) as last FROM todos").fetchone()
    count = conn.execute("SELECT COUNT(*) as c FROM todos").fetchone()
    return {
        "last_sync": last["last"] or 0,
        "total_todos": count["c"]
    }

@app.get("/health")
def health():
    return {"status": "ok"}

if __name__ == "__main__":
    import uvicorn
    print("Starting Todo API server on port 8765...")
    uvicorn.run(app, host="0.0.0.0", port=8765)
