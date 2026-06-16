package com.kanochi.todo.data.local

import androidx.room.*
import com.kanochi.todo.data.model.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY CASE WHEN status = 'pending' THEN 0 ELSE 1 END, CASE priority WHEN 'high' THEN 0 WHEN 'medium' THEN 1 ELSE 2 END, dueDate ASC")
    fun getAllTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE dueDate >= :dayStart AND dueDate < :dayEnd ORDER BY priority ASC")
    fun getTodosForDate(dayStart: Long, dayEnd: Long): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getTodoById(id: String): TodoEntity?

    @Query("SELECT * FROM todos WHERE status = 'pending' AND dueDate IS NOT NULL AND dueDate < :now ORDER BY dueDate ASC")
    fun getOverdueTodos(now: Long): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE status = 'pending' AND (dueDate IS NULL OR dueDate >= :todayStart)")
    fun getPendingTodos(todayStart: Long): Flow<List<TodoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodos(todos: List<TodoEntity>)

    @Update
    suspend fun updateTodo(todo: TodoEntity)

    @Query("UPDATE todos SET status = :status, updatedAt = :now WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE todos SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("SELECT * FROM todos WHERE syncStatus = 'pending'")
    suspend fun getUnsyncedTodos(): List<TodoEntity>

    @Delete
    suspend fun deleteTodo(todo: TodoEntity)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteTodoById(id: String)

    @Query("DELETE FROM todos")
    suspend fun deleteAll()

    @Query("SELECT * FROM todos WHERE updatedAt > :since")
    suspend fun getTodosUpdatedSince(since: Long): List<TodoEntity>

    @Query("SELECT MAX(updatedAt) FROM todos")
    suspend fun getLastUpdateTime(): Long?
}
