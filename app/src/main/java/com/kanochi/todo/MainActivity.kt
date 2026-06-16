package com.kanochi.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.kanochi.todo.data.local.TodoDatabase
import com.kanochi.todo.data.repository.TodoRepository
import com.kanochi.todo.ui.calendar.CalendarScreen
import com.kanochi.todo.ui.theme.AppBackground
import com.kanochi.todo.ui.theme.TodoAppTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: TodoRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = TodoDatabase.getInstance(this)
        repository = TodoRepository(database.todoDao())

        setContent {
            TodoAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppBackground
                ) {
                    CalendarScreen(
                        repository = repository
                    )
                }
            }
        }
    }
}
