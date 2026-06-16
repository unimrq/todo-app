package com.kanochi.todo.data.remote

import com.kanochi.todo.data.model.TodoEntity
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

private const val BASE_URL = "http://8.138.122.116:8765/"

interface TodoApiService {
    @GET("todos")
    suspend fun getTodos(): List<TodoDto>

    @PUT("todos/{id}")
    suspend fun updateTodo(@Path("id") id: String, @Body todo: TodoUpdateDto): TodoDto
}

data class TodoUpdateDto(
    val title: String? = null,
    val description: String? = null,
    val status: String? = null,
    val priority: String? = null,
    val category: String? = null,
    val due_date: Long? = null
)

object TodoApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val service: TodoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TodoApiService::class.java)
    }
}
