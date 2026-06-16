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
import java.text.SimpleDateFormat
import java.util.*

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
                            onDismiss = {
                                showAddDialog = false
                                addDialogDate = null
                            },
                            onConfirm = { text ->
                                scope.launch {
                                    // Parse user's text input
                                    val parsed = parseTaskText(text, addDialogDate)
                                    repository.createTodo(
                                        title = parsed.title,
                                        description = parsed.description,
                                        priority = parsed.priority,
                                        category = parsed.category,
                                        dueDate = parsed.dueDate
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

    /**
     * Parse plain text task input into structured fields.
     * User just types something like:
     * - "买猫粮 高优先级 明天" → title=买猫粮, priority=high, dueDate=tomorrow
     * - "写报告" → title=写报告, priority=medium, dueDate=selected date
     */
    private fun parseTaskText(
        text: String,
        selectedDate: Long?
    ): ParsedTask {
        var title = text
        var priority = "medium"
        val lower = text.lowercase(Locale.getDefault())

        // Detect priority keywords
        if (lower.contains("高优先级") || lower.contains("重要") || lower.contains("紧急")) {
            priority = "high"
            title = title.replace(Regex("[（(]?高优先级[）)]?"), "").trim()
            title = title.replace(Regex("[（(]?重要[）)]?"), "").trim()
            title = title.replace(Regex("[（(]?紧急[）)]?"), "").trim()
        }
        if (lower.contains("低优先级") || lower.contains("不急")) {
            priority = "low"
            title = title.replace(Regex("[（(]?低优先级[）)]?"), "").trim()
            title = title.replace(Regex("[（(]?不急[）)]?"), "").trim()
        }

        // Default due date = selected calendar date
        val dueDate = selectedDate

        return ParsedTask(
            title = title.ifBlank { text },
            description = "",
            priority = priority,
            category = "",
            dueDate = dueDate
        )
    }
}

data class ParsedTask(
    val title: String,
    val description: String,
    val priority: String,
    val category: String,
    val dueDate: Long?
)
