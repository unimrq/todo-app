package com.kanochi.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.kanochi.todo.data.local.TodoDatabase
import com.kanochi.todo.data.model.*
import com.kanochi.todo.data.repository.TodoRepository
import com.kanochi.todo.ui.calendar.CalendarScreen
import com.kanochi.todo.ui.theme.AppBackground
import com.kanochi.todo.ui.theme.TodoAppTheme
import com.kanochi.todo.ui.todo.AddTodoDialog
import kotlinx.coroutines.launch

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
                    var showAddDialog by remember { mutableStateOf(false) }
                    var addDialogDate by remember { mutableStateOf<Long?>(null) }
                    val scope = rememberCoroutineScope()

                    CalendarScreen(
                        repository = repository,
                        onAddTodo = { date ->
                            addDialogDate = date
                            showAddDialog = true
                        },
                        onEditTodo = { todo ->
                        }
                    )

                    if (showAddDialog) {
                        AddTodoDialog(
                            initialDate = addDialogDate,
                            onDismiss = {
                                showAddDialog = false
                                addDialogDate = null
                            },
                            onConfirm = { title, description, priority, category, dueDate ->
                                scope.launch {
                                    repository.createTodo(
                                        title = title,
                                        description = description,
                                        priority = priority,
                                        category = category,
                                        dueDate = dueDate
                                    )
                                }
                                showAddDialog = false
                                addDialogDate = null
                            }
                        )
                    }
                }
            }
        }
    }
}
