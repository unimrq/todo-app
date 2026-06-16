package com.kanochi.todo.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

private const val BASE_URL = "http://8.138.122.116:8765/"

interface TodoApiService {
    @GET("todos")
    suspend fun getTodos(): List<TodoDto>
}

object TodoApi {
    val service: TodoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TodoApiService::class.java)
    }
}
