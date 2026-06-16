package com.example.todoapp.data

class TodoRepository {
    private val api get() = ApiClient.api

    suspend fun getTodosByDate(date: String): List<Todo> {
        return api.listTodos(dueDate = date)
    }

    suspend fun getMonthTodos(year: Int, month: Int): Map<String, List<Todo>> {
        return api.getMonthTodos(year, month)
    }

    suspend fun getKanban(): Map<String, List<Todo>> {
        return api.getKanban()
    }

    suspend fun create(todo: TodoCreate): Todo {
        return api.createTodo(todo)
    }

    suspend fun update(id: String, update: TodoUpdate): Todo {
        return api.updateTodo(id, update)
    }

    suspend fun delete(id: String) {
        api.deleteTodo(id)
    }

    suspend fun toggle(id: String): Todo {
        return api.toggleTodo(id)
    }
}
