package com.kanochi.todo.data.remote

import com.kanochi.todo.data.model.TodoEntity

data class TodoDto(
    val id: String,
    val title: String,
    val description: String,
    val status: String,
    val priority: String,
    val category: String,
    val due_date: Long?,
    val created_at: Long,
    val updated_at: Long,
    val source: String,
    val tags: String,
    val sync_status: String
) {
    fun toEntity(): TodoEntity = TodoEntity(
        id = id,
        title = title,
        description = description,
        status = status,
        priority = priority,
        category = category,
        dueDate = due_date,
        createdAt = created_at,
        updatedAt = updated_at,
        source = source,
        tags = tags,
        syncStatus = sync_status
    )
}
