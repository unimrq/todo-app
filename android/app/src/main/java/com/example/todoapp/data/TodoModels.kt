package com.example.todoapp.data

import com.google.gson.annotations.SerializedName

data class Todo(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val status: String = "pending",
    val priority: String = "medium",
    val category: String = "",
    @SerializedName("kanban_column")
    val kanbanColumn: String = "待办",
    @SerializedName("due_date")
    val dueDate: String = "",
    @SerializedName("created_at")
    val createdAt: String = "",
    @SerializedName("updated_at")
    val updatedAt: String = "",
    val source: String = "user",
    val tags: List<String> = emptyList()
)

data class TodoCreate(
    val title: String,
    val description: String = "",
    val priority: String = "medium",
    val category: String = "",
    @SerializedName("kanban_column")
    val kanbanColumn: String = "待办",
    @SerializedName("due_date")
    val dueDate: String = "",
    val tags: List<String> = emptyList(),
    val source: String = "user"
)

data class TodoUpdate(
    val title: String? = null,
    val description: String? = null,
    val status: String? = null,
    val priority: String? = null,
    val category: String? = null,
    @SerializedName("kanban_column")
    val kanbanColumn: String? = null,
    @SerializedName("due_date")
    val dueDate: String? = null,
    val tags: List<String>? = null
)
