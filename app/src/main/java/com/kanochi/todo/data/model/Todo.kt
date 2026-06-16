package com.kanochi.todo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String = "",
    val status: String = "pending",   // pending | completed | cancelled
    val priority: String = "medium",   // high | medium | low
    val category: String = "",
    val dueDate: Long? = null,         // timestamp in millis
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val source: String = "user",       // user | ai
    val tags: String = "",             // comma-separated
    val syncStatus: String = "pending" // pending | synced | conflict
)

data class Todo(
    val id: String,
    val title: String,
    val description: String,
    val status: Status,
    val priority: Priority,
    val category: String,
    val dueDate: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val source: String,
    val tags: List<String>,
    val syncStatus: String
)

enum class Status(val display: String) {
    PENDING("待办"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    companion object {
        fun fromString(s: String) = entries.find { it.name == s } ?: PENDING
    }
}

enum class Priority(val display: String, val score: Int) {
    HIGH("高", 3),
    MEDIUM("中", 2),
    LOW("低", 1);

    companion object {
        fun fromString(s: String) = entries.find { it.name == s } ?: MEDIUM
    }
}

fun TodoEntity.toTodo(): Todo = Todo(
    id = id,
    title = title,
    description = description,
    status = Status.fromString(status),
    priority = Priority.fromString(priority),
    category = category,
    dueDate = dueDate,
    createdAt = createdAt,
    updatedAt = updatedAt,
    source = source,
    tags = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() },
    syncStatus = syncStatus
)

fun Todo.toEntity(): TodoEntity = TodoEntity(
    id = id,
    title = title,
    description = description,
    status = status.name,
    priority = priority.name,
    category = category,
    dueDate = dueDate,
    createdAt = createdAt,
    updatedAt = updatedAt,
    source = source,
    tags = tags.joinToString(","),
    syncStatus = syncStatus
)
