"""Todo App Backend - Database Models"""
from datetime import datetime
from sqlalchemy import create_engine, Column, String, Integer, Boolean, DateTime, Text, Enum
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
import uuid

Base = declarative_base()

def generate_id():
    return str(uuid.uuid4())

class Todo(Base):
    __tablename__ = "todos"
    
    id = Column(String(36), primary_key=True, default=generate_id)
    title = Column(String(200), nullable=False)
    description = Column(Text, default="")
    status = Column(String(20), default="pending")  # pending, doing, done, cancelled
    priority = Column(String(10), default="medium")  # high, medium, low
    category = Column(String(50), default="")
    kanban_column = Column(String(50), default="待办")  # 待办, 进行中, 已完成
    due_date = Column(String(10), default="")  # YYYY-MM-DD
    created_at = Column(String(30), default=lambda: datetime.utcnow().isoformat())
    updated_at = Column(String(30), default=lambda: datetime.utcnow().isoformat())
    source = Column(String(10), default="user")  # user, ai
    tags = Column(Text, default="")  # comma separated
    
    def to_dict(self):
        return {
            "id": self.id,
            "title": self.title,
            "description": self.description,
            "status": self.status,
            "priority": self.priority,
            "category": self.category,
            "kanban_column": self.kanban_column,
            "due_date": self.due_date,
            "created_at": self.created_at,
            "updated_at": self.updated_at,
            "source": self.source,
            "tags": self.tags.split(",") if self.tags else []
        }

class Category(Base):
    __tablename__ = "categories"
    
    id = Column(String(36), primary_key=True, default=generate_id)
    name = Column(String(50), nullable=False, unique=True)
    color = Column(String(7), default="#4CAF50")  # hex color
    created_at = Column(String(30), default=lambda: datetime.utcnow().isoformat())
    
    def to_dict(self):
        return {
            "id": self.id,
            "name": self.name,
            "color": self.color,
            "created_at": self.created_at
        }

# Database setup
DATABASE_URL = "sqlite:///data/todo.db"
engine = create_engine(DATABASE_URL, connect_args={"check_same_thread": False})
SessionLocal = sessionmaker(bind=engine)

def init_db():
    import os
    os.makedirs("data", exist_ok=True)
    Base.metadata.create_all(engine)

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
