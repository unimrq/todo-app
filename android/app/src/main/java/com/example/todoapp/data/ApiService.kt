package com.example.todoapp.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

// 改成你的服务器地址
// 局域网：http://192.168.10.x:8765
// 外网：http://8.138.122.116:8765
object ApiConfig {
    var baseUrl: String = "http://8.138.122.116:8765/"
}

interface TodoApi {
    @GET("todos")
    suspend fun listTodos(
        @Query("status") status: String? = null,
        @Query("due_date") dueDate: String? = null,
        @Query("kanban_column") kanbanColumn: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): List<Todo>

    @GET("todos/{id}")
    suspend fun getTodo(@Path("id") id: String): Todo

    @POST("todos")
    suspend fun createTodo(@Body todo: TodoCreate): Todo

    @PUT("todos/{id}")
    suspend fun updateTodo(@Path("id") id: String, @Body update: TodoUpdate): Todo

    @DELETE("todos/{id}")
    suspend fun deleteTodo(@Path("id") id: String)

    @PATCH("todos/{id}/toggle")
    suspend fun toggleTodo(@Path("id") id: String): Todo

    @GET("calendar/{year}/{month}")
    suspend fun getMonthTodos(
        @Path("year") year: Int,
        @Path("month") month: Int
    ): Map<String, List<Todo>>

    @GET("kanban")
    suspend fun getKanban(): Map<String, List<Todo>>

    @GET("sync/changes")
    suspend fun getChanges(@Query("since") since: String? = null): List<Todo>

    @GET("health")
    suspend fun health(): Map<String, String>
}

object ApiClient {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(ApiConfig.baseUrl)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: TodoApi = retrofit.create(TodoApi::class.java)
}
