package com.kanochi.todo.data.repository

import com.kanochi.todo.data.local.TodoDao
import com.kanochi.todo.data.model.*
import com.kanochi.todo.data.remote.TodoApi
import com.kanochi.todo.data.remote.TodoUpdateDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TodoRepository(private val dao: TodoDao) {

    fun getAllTodos(): Flow<List<TodoEntity>> = dao.getAllTodos()

    fun getTodosForDate(dayStart: Long, dayEnd: Long): Flow<List<TodoEntity>> =
        dao.getTodosForDate(dayStart, dayEnd)

    fun getOverdueTodos(now: Long): Flow<List<TodoEntity>> = dao.getOverdueTodos(now)

    fun getPendingTodos(todayStart: Long): Flow<List<TodoEntity>> = dao.getPendingTodos(todayStart)

    suspend fun getTodoById(id: String): TodoEntity? = dao.getTodoById(id)

    suspend fun createTodo(
        title: String,
        description: String = "",
        priority: String = "medium",
        category: String = "",
        dueDate: Long? = null
    ): TodoEntity {
        val todo = TodoEntity(
            id = java.util.UUID.randomUUID().toString(),
            title = title,
            description = description,
            priority = priority,
            category = category,
            dueDate = dueDate
        )
        dao.insertTodo(todo)
        return todo
    }

    suspend fun updateTodo(todo: TodoEntity) {
        try {
            TodoApi.service.updateTodo(
                todo.id,
                TodoUpdateDto(
                    title = todo.title,
                    description = todo.description,
                    status = todo.status,
                    priority = todo.priority,
                    category = todo.category,
                    due_date = todo.dueDate
                )
            )
        } catch (_: Exception) { }
        dao.updateTodo(todo)
    }

    suspend fun deleteTodo(id: String) {
        try {
            TodoApi.service.deleteTodo(id)
        } catch (_: Exception) { }
        dao.deleteTodoById(id)
    }

    suspend fun toggleStatus(id: String) {
        val todo = dao.getTodoById(id) ?: return
        val newStatus = if (todo.status == "completed") "pending" else "completed"
        dao.updateStatus(id, newStatus)
    }

    // Pull: fetch cloud tasks → merge into local
    suspend fun refreshFromServer(): Boolean = withContext(Dispatchers.IO) {
        try {
            val serverList = TodoApi.service.getTodos()
            val serverMap = serverList.associateBy { it.id }
            val localMap = dao.getTodosUpdatedSince(0).associateBy { it.id }

            val toUpsert = serverList.filter { s ->
                val local = localMap[s.id]
                local == null || local.updatedAt != s.updated_at
            }.map { it.toEntity() }

            val toDelete = localMap.keys - serverMap.keys

            if (toUpsert.isNotEmpty()) dao.insertTodos(toUpsert)
            toDelete.forEach { dao.deleteTodoById(it) }
            true
        } catch (_: Exception) {
            false
        }
    }

    // Push: upload local completed/updated tasks to cloud
    suspend fun syncToServer(): Boolean = withContext(Dispatchers.IO) {
        try {
            val localTodos = dao.getTodosUpdatedSince(0)
            for (todo in localTodos) {
                TodoApi.service.updateTodo(
                    todo.id,
                    TodoUpdateDto(
                        title = todo.title,
                        description = todo.description,
                        status = todo.status,
                        priority = todo.priority,
                        category = todo.category,
                        due_date = todo.dueDate
                    )
                )
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    // Bidirectional sync: push local changes first, then pull from cloud
    suspend fun fullSync(): Boolean = withContext(Dispatchers.IO) {
        try {
            val pushed = syncToServer()
            val pulled = refreshFromServer()
            pushed && pulled
        } catch (_: Exception) {
            false
        }
    }
}
