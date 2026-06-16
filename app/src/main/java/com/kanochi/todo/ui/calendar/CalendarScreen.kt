package com.kanochi.todo.ui.calendar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kanochi.todo.data.model.*
import com.kanochi.todo.data.repository.TodoRepository
import com.kanochi.todo.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    repository: TodoRepository,
    onAddTodo: (Long?) -> Unit,
    onEditTodo: (TodoEntity) -> Unit,
    onOpenDrawer: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val calendar = remember { Calendar.getInstance() }
    val currentMonth = remember { mutableStateOf(Calendar.getInstance()) }
    val selectedDate = remember { mutableStateOf(Calendar.getInstance()) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }
    var isCalendarExpanded by remember { mutableStateOf(false) }

    // Today's date for highlighting
    val today = remember { Calendar.getInstance() }

    // Load todos for selected date
    val selectedDateStart = remember(selectedDate.value) {
        val c = selectedDate.value.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        c.timeInMillis
    }
    val selectedDateEnd = selectedDateStart + 86400000L

    val todosForDate by repository.getTodosForDate(selectedDateStart, selectedDateEnd)
        .collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "侧边栏",
                            tint = TextPrimary
                        )
                    }
                },
                title = {
                    Text(
                        text = "${currentMonth.value.get(Calendar.YEAR)}年${currentMonth.value.get(Calendar.MONTH) + 1}月",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                actions = {
                    IconButton(onClick = { /* 更多 - 待后续开发 */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "更多",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppSurface,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddTodo(selectedDateStart) },
                containerColor = PrimaryBlue,
                contentColor = AppSurface,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新建任务"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppBackground)
        ) {
            // Month navigation row
            MonthNavRow(
                calendar = currentMonth.value,
                onPreviousMonth = {
                    currentMonth.value = currentMonth.value.clone() as Calendar
                    currentMonth.value.add(Calendar.MONTH, -1)
                },
                onNextMonth = {
                    currentMonth.value = currentMonth.value.clone() as Calendar
                    currentMonth.value.add(Calendar.MONTH, 1)
                },
                onMonthClick = { showMonthPicker = true },
                onYearClick = { showYearPicker = true }
            )

            // Weekday headers
            WeekdayHeader()

            // Calendar grid (week or month)
            CalendarGrid(
                month = currentMonth.value,
                selectedDate = selectedDate.value,
                today = today,
                repository = repository,
                isExpanded = isCalendarExpanded,
                onDateSelected = { selectedDate.value = it },
                onSwipe = { direction ->
                    currentMonth.value = currentMonth.value.clone() as Calendar
                    currentMonth.value.add(Calendar.MONTH, if (direction > 0) -1 else 1)
                }
            )

            // Drag handle bar
            DragHandle(
                isExpanded = isCalendarExpanded,
                onToggle = { isCalendarExpanded = !isCalendarExpanded }
            )

            // Divider
            HorizontalDivider(color = AppBorder, thickness = 0.5.dp)

            // Todo list for selected date
            TodoListSection(
                date = selectedDate.value,
                todos = todosForDate,
                onToggleTodo = { todo ->
                    scope.launch {
                        repository.toggleStatus(todo.id)
                    }
                },
                onDeleteTodo = { todo ->
                    scope.launch {
                        repository.deleteTodo(todo.id)
                    }
                },
                onEditTodo = onEditTodo
            )
        }
    }

    // Month picker dialog
    if (showMonthPicker) {
        MonthPickerDialog(
            currentMonth = currentMonth.value,
            onDismiss = { showMonthPicker = false },
            onMonthSelected = { month ->
                currentMonth.value.set(Calendar.MONTH, month)
                showMonthPicker = false
            }
        )
    }

    // Year picker dialog
    if (showYearPicker) {
        YearPickerDialog(
            currentYear = currentMonth.value.get(Calendar.YEAR),
            onDismiss = { showYearPicker = false },
            onYearSelected = { year ->
                currentMonth.value.set(Calendar.YEAR, year)
                showYearPicker = false
            }
        )
    }
}

@Composable
private fun MonthNavRow(
    calendar: Calendar,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthClick: () -> Unit,
    onYearClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Text("<", color = TextSecondary, fontSize = 18.sp)
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${calendar.get(Calendar.YEAR)}年",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable(onClick = onYearClick)
                    .padding(horizontal = 4.dp)
            )
            Text(
                text = "${calendar.get(Calendar.MONTH) + 1}月",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable(onClick = onMonthClick)
                    .padding(horizontal = 4.dp)
            )
        }

        IconButton(onClick = onNextMonth) {
            Text(">", color = TextSecondary, fontSize = 18.sp)
        }
    }
}

@Composable
private fun WeekdayHeader() {
    val weekdays = listOf("一", "二", "三", "四", "五", "六", "日")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        weekdays.forEachIndexed { index, day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = if (index >= 5) CalendarWeekend else TextTertiary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    month: Calendar,
    selectedDate: Calendar,
    today: Calendar,
    repository: TodoRepository,
    isExpanded: Boolean,
    onDateSelected: (Calendar) -> Unit,
    onSwipe: (Float) -> Unit
) {
    val weeks = remember(month) { getWeeksForMonth(month) }
    // Current week (the week containing selectedDate)
    val currentWeekIndex = remember(selectedDate) {
        weeks.indexOfFirst { week ->
            week.any { day ->
                day != null && isSameDay(day, selectedDate)
            }
        }.coerceAtLeast(0)
    }

    // Animate height
    val gridHeight by animateDpAsState(
        targetValue = if (isExpanded) Dp.Unspecified else 52.dp,
        label = "gridHeight"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .horizontalScroll(rememberScrollState()) // Ensure horizontal swipe doesn't interfere
    ) {
        if (isExpanded) {
            // Show full month
            weeks.forEachIndexed { index, week ->
                WeekRow(
                    week = week,
                    month = month,
                    selectedDate = selectedDate,
                    today = today,
                    onDateSelected = onDateSelected
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
        } else {
            // Show only current week
            if (currentWeekIndex < weeks.size) {
                WeekRow(
                    week = weeks[currentWeekIndex],
                    month = month,
                    selectedDate = selectedDate,
                    today = today,
                    onDateSelected = onDateSelected
                )
            }
        }
    }
}

@Composable
private fun WeekRow(
    week: Array<Calendar?>,
    month: Calendar,
    selectedDate: Calendar,
    today: Calendar,
    onDateSelected: (Calendar) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        for (i in 0..6) {
            val day = week[i]
            if (day == null) {
                Spacer(modifier = Modifier.weight(1f))
            } else {
                DayCell(
                    day = day,
                    isToday = isSameDay(day, today),
                    isSelected = isSameDay(day, selectedDate),
                    isCurrentMonth = day.get(Calendar.MONTH) == month.get(Calendar.MONTH),
                    onClick = { onDateSelected(day) }
                )
            }
        }
    }
}

@Composable
private fun DragHandle(
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Small drag handle bar
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(AppBorder)
        )
    }
}

@Composable
private fun DayCell(
    day: Calendar,
    isToday: Boolean,
    isSelected: Boolean,
    isCurrentMonth: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = when {
            isSelected -> PrimaryBlueVariant
            isToday -> CalendarTodayBg
            else -> androidx.compose.ui.graphics.Color.Transparent
        },
        label = "dayBg"
    )

    val textColor = when {
        isSelected -> PrimaryBlue
        isToday -> CalendarToday
        !isCurrentMonth -> TextTertiary.copy(alpha = 0.4f)
        else -> TextPrimary
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${day.get(Calendar.DAY_OF_MONTH)}",
            color = textColor,
            fontSize = 14.sp,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun TodoListSection(
    date: Calendar,
    todos: List<TodoEntity>,
    onToggleTodo: (TodoEntity) -> Unit,
    onDeleteTodo: (TodoEntity) -> Unit,
    onEditTodo: (TodoEntity) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("M月d日 EEEE", Locale.CHINESE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Date header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateFormat.format(date.time),
                color = TextSecondary,
                fontSize = 13.sp
            )
            Text(
                text = "${todos.count { it.status == "completed" }}/${todos.size}",
                color = TextTertiary,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (todos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "今天没有待办事项",
                    color = TextTertiary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(todos, key = { it.id }) { todo ->
                    TodoRow(
                        todo = todo,
                        onToggle = { onToggleTodo(todo) },
                        onDelete = { onDeleteTodo(todo) },
                        onEdit = { onEditTodo(todo) }
                    )
                }
            }
        }

        // Spacer for FAB
        Spacer(modifier = Modifier.height(72.dp))
    }
}

@Composable
private fun TodoRow(
    todo: TodoEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val isCompleted = todo.status == "completed"
    val priority = Priority.fromString(todo.priority)
    val priorityColor = when (priority) {
        Priority.HIGH -> HighPriority
        Priority.MEDIUM -> MediumPriority
        Priority.LOW -> LowPriority
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, AppBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox / Toggle
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = CompletedGreen,
                    uncheckedColor = TextTertiary,
                    checkmarkColor = AppSurface
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    color = if (isCompleted) CompletedText else TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                if (todo.description.isNotBlank()) {
                    Text(
                        text = todo.description,
                        color = TextTertiary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Priority indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(priorityColor)
            )
        }
    }
}

// Helper functions
private fun getWeeksForMonth(calendar: Calendar): List<Array<Calendar?>> {
    val cal = calendar.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    // Convert from Sunday-first to Monday-first
    val startOffset = (firstDayOfWeek - Calendar.MONDAY + 7) % 7

    cal.add(Calendar.DAY_OF_MONTH, -startOffset)

    val weeks = mutableListOf<Array<Calendar?>>()
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val month = calendar.get(Calendar.MONTH)

    val totalDays = startOffset + daysInMonth
    val numWeeks = (totalDays + 6) / 7

    for (w in 0 until numWeeks) {
        val week = arrayOfNulls<Calendar>(7)
        for (d in 0..6) {
            val day = cal.clone() as Calendar
            week[d] = day
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        weeks.add(week)
    }

    cal.timeInMillis = calendar.timeInMillis
    return weeks
}

private fun isSameDay(c1: Calendar, c2: Calendar): Boolean {
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

@Composable
private fun MonthPickerDialog(
    currentMonth: Calendar,
    onDismiss: () -> Unit,
    onMonthSelected: (Int) -> Unit
) {
    val months = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")
    val currentMonthIndex = currentMonth.get(Calendar.MONTH)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择月份", color = TextPrimary) },
        text = {
            Column {
                months.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEachIndexed { _, month ->
                            val idx = months.indexOf(month)
                            TextButton(
                                onClick = { onMonthSelected(idx) },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (idx == currentMonthIndex) PrimaryBlue else TextPrimary
                                )
                            ) {
                                Text(
                                    month,
                                    fontWeight = if (idx == currentMonthIndex) FontWeight.Bold else FontWeight.Normal,
                                    color = if (idx == currentMonthIndex) PrimaryBlue else TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        },
        containerColor = AppSurface,
        titleContentColor = TextPrimary
    )
}

@Composable
private fun YearPickerDialog(
    currentYear: Int,
    onDismiss: () -> Unit,
    onYearSelected: (Int) -> Unit
) {
    val years = (currentYear - 5..currentYear + 5).toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择年份", color = TextPrimary) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                years.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { year ->
                            TextButton(
                                onClick = { onYearSelected(year) },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (year == currentYear) PrimaryBlue else TextPrimary
                                )
                            ) {
                                Text(
                                    "${year}年",
                                    fontWeight = if (year == currentYear) FontWeight.Bold else FontWeight.Normal,
                                    color = if (year == currentYear) PrimaryBlue else TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        },
        containerColor = AppSurface,
        titleContentColor = TextPrimary
    )
}
