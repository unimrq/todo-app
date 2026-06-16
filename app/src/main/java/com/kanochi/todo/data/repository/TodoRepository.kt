package com.kanochi.todo.data.repository

import com.kanochi.todo.data.local.TodoDao
import com.kanochi.todo.data.model.*
import com.kanochi.todo.data.remote.TodoApi
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
        dao.updateTodo(todo)
    }

    suspend fun deleteTodo(id: String) {
        dao.deleteTodoById(id)
    }

    suspend fun toggleStatus(id: String) {
        val todo = dao.getTodoById(id) ?: return
        val newStatus = if (todo.status == "completed") "pending" else "completed"
        dao.updateStatus(id, newStatus)
    }

    // Server sync — replace local data with server data
    suspend fun refreshFromServer(): Boolean = withContext(Dispatchers.IO) {
        try {
            val serverTodos = TodoApi.service.getTodos()
            val entities = serverTodos.map { it.toEntity() }
            dao.deleteAll()
            dao.insertTodos(entities)
            true
        } catch (_: Exception) {
            false
        }
    }
}
