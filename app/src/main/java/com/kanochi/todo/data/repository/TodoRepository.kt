package com.kanochi.todo.data.repository

import com.kanochi.todo.data.local.TodoDao
import com.kanochi.todo.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

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
        dueDate: Long? = null,
        tags: List<String> = emptyList(),
        source: String = "user"
    ): TodoEntity {
        val now = System.currentTimeMillis()
        val todo = TodoEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            priority = priority,
            category = category,
            dueDate = dueDate,
            createdAt = now,
            updatedAt = now,
            source = source,
            tags = tags.joinToString(","),
            syncStatus = "pending"
        )
        dao.insertTodo(todo)
        return todo
    }

    suspend fun updateTodo(todo: TodoEntity) {
        dao.updateTodo(todo.copy(updatedAt = System.currentTimeMillis(), syncStatus = "pending"))
    }

    suspend fun toggleStatus(id: String) {
        val todo = dao.getTodoById(id) ?: return
        val newStatus = if (todo.status == "pending") "completed" else "pending"
        dao.updateStatus(id, newStatus)
    }

    suspend fun deleteTodo(id: String) = dao.deleteTodoById(id)

    suspend fun getUnsyncedTodos(): List<TodoEntity> = dao.getUnsyncedTodos()

    suspend fun markSynced(id: String) = dao.updateSyncStatus(id, "synced")
}
