package com.kanochi.todo.ui.calendar

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kanochi.todo.data.model.*
import com.kanochi.todo.data.repository.TodoRepository
import com.kanochi.todo.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

private const val SWIPE_THRESHOLD = 80f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    repository: TodoRepository,
    onAddTodo: (Long?) -> Unit,
    onEditTodo: (TodoEntity) -> Unit,
    onOpenDrawer: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val today = remember { Calendar.getInstance() }
    val currentMonth = remember { mutableStateOf(Calendar.getInstance()) }
    val selectedDate = remember { mutableStateOf(Calendar.getInstance()) }
    var isExpanded by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }

    // Selected date boundaries for DB query
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
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "侧边栏",
                            tint = AppSurface
                        )
                    }
                },
                title = {
                    Text(
                        text = "${currentMonth.value.get(Calendar.YEAR)}年${currentMonth.value.get(Calendar.MONTH) + 1}月",
                        fontWeight = FontWeight.Bold,
                        color = AppSurface,
                        modifier = Modifier.clickable { showMonthPicker = true }
                    )
                },
                actions = {
                    IconButton(onClick = { /* 更多 - 待后续开发 */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "更多",
                            tint = AppSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = PrimaryBlue,
                    titleContentColor = AppSurface
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
            // Calendar header area (deep blue background)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryBlue)
            ) {
                // Weekday headers
                WeekdayHeader()

                // Calendar grid with gestures
                CalendarGrid(
                    month = currentMonth.value,
                    selectedDate = selectedDate.value,
                    today = today,
                    isExpanded = isExpanded,
                    onDateSelected = { selectedDate.value = it },
                    onSwipeDown = { isExpanded = true },
                    onSwipeUp = { isExpanded = false },
                    onSwipeLeft = {
                        if (isExpanded) {
                            currentMonth.value = currentMonth.value.clone() as Calendar
                            currentMonth.value.add(Calendar.MONTH, 1)
                        } else {
                            selectedDate.value = selectedDate.value.clone() as Calendar
                            selectedDate.value.add(Calendar.DAY_OF_MONTH, 7)
                            // Keep within same month context
                            if (selectedDate.value.get(Calendar.MONTH) != currentMonth.value.get(Calendar.MONTH)) {
                                currentMonth.value = currentMonth.value.clone() as Calendar
                                currentMonth.value.timeInMillis = selectedDate.value.timeInMillis
                            }
                        }
                    },
                    onSwipeRight = {
                        if (isExpanded) {
                            currentMonth.value = currentMonth.value.clone() as Calendar
                            currentMonth.value.add(Calendar.MONTH, -1)
                        } else {
                            selectedDate.value = selectedDate.value.clone() as Calendar
                            selectedDate.value.add(Calendar.DAY_OF_MONTH, -7)
                            if (selectedDate.value.get(Calendar.MONTH) != currentMonth.value.get(Calendar.MONTH)) {
                                currentMonth.value = currentMonth.value.clone() as Calendar
                                currentMonth.value.timeInMillis = selectedDate.value.timeInMillis
                            }
                        }
                    }
                )

                // Drag handle bar (visual only, no click)
                DragHandle()
            }

            // Divider
            HorizontalDivider(color = AppBorder, thickness = 0.5.dp)

            // Todo list for selected date
            TodoListSection(
                date = selectedDate.value,
                todos = todosForDate,
                onToggleTodo = { todo ->
                    scope.launch { repository.toggleStatus(todo.id) }
                },
                onDeleteTodo = { todo ->
                    scope.launch { repository.deleteTodo(todo.id) }
                },
                onEditTodo = onEditTodo
            )
        }
    }

    // Month picker dialog (triggered by tapping TopBar title)
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
private fun WeekdayHeader() {
    val weekdays = listOf("一", "二", "三", "四", "五", "六", "日")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 2.dp)
    ) {
        weekdays.forEachIndexed { index, day ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    textAlign = TextAlign.Center,
                    color = AppSurface.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    month: Calendar,
    selectedDate: Calendar,
    today: Calendar,
    isExpanded: Boolean,
    onDateSelected: (Calendar) -> Unit,
    onSwipeDown: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
) {
    val weeks = remember(month) { getWeeksForMonth(month) }
    val currentWeekIndex = remember(selectedDate) {
        weeks.indexOfFirst { week ->
            week.any { day ->
                day != null && isSameDay(day, selectedDate)
            }
        }.coerceAtLeast(0)
    }

    // Drag gesture state
    var dragAccumX by remember { mutableStateOf(0f) }
    var dragAccumY by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .pointerInput(isExpanded) {
                dragAccumX = 0f
                dragAccumY = 0f
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumX += dragAmount.x
                        dragAccumY += dragAmount.y

                        val absX = abs(dragAccumX)
                        val absY = abs(dragAccumY)

                        if (absX > SWIPE_THRESHOLD || absY > SWIPE_THRESHOLD) {
                            if (absX > absY) {
                                // Horizontal swipe
                                if (dragAccumX > 0) {
                                    onSwipeRight()
                                } else {
                                    onSwipeLeft()
                                }
                            } else {
                                // Vertical swipe
                                if (dragAccumY > 0) {
                                    onSwipeDown()
                                } else {
                                    onSwipeUp()
                                }
                            }
                            dragAccumX = 0f
                            dragAccumY = 0f
                        }
                    },
                    onDragEnd = {
                        dragAccumX = 0f
                        dragAccumY = 0f
                    },
                    onDragCancel = {
                        dragAccumX = 0f
                        dragAccumY = 0f
                    }
                )
            }
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
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
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
private fun DragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(AppSurface.copy(alpha = 0.4f))
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
            isSelected -> AppSurface
            isToday -> AppSurface.copy(alpha = 0.2f)
            else -> Color.Transparent
        },
        label = "dayBg"
    )

    val textColor = when {
        isSelected -> PrimaryBlue
        isToday -> AppSurface
        !isCurrentMonth -> AppSurface.copy(alpha = 0.35f)
        else -> AppSurface.copy(alpha = 0.85f)
    }

    val isWeekend = day.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
            day.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY

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
            color = if (isSelected) textColor else {
                if (isWeekend && !isToday) AppSurface.copy(alpha = 0.6f) else textColor
            },
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
fun getWeeksForMonth(calendar: Calendar): List<Array<Calendar?>> {
    val cal = calendar.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val startOffset = (firstDayOfWeek - Calendar.MONDAY + 7) % 7
    cal.add(Calendar.DAY_OF_MONTH, -startOffset)

    val weeks = mutableListOf<Array<Calendar?>>()
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val totalDays = startOffset + daysInMonth
    val numWeeks = (totalDays + 6) / 7

    for (w in 0 until numWeeks) {
        val week = arrayOfNulls<Calendar>(7)
        for (d in 0..6) {
            week[d] = cal.clone() as Calendar
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        weeks.add(week)
    }
    cal.timeInMillis = calendar.timeInMillis
    return weeks
}

fun isSameDay(c1: Calendar, c2: Calendar): Boolean {
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

@Composable
private fun MonthPickerDialog(
    currentMonth: Calendar,
    onDismiss: () -> Unit,
    onMonthSelected: (Int) -> Unit
) {
    val months = listOf(
        "1月", "2月", "3月", "4月", "5月", "6月",
        "7月", "8月", "9月", "10月", "11月", "12月"
    )
    val currentMonthIndex = currentMonth.get(Calendar.MONTH)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择月份", color = TextPrimary) },
        text = {
            Column {
                months.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { month ->
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
                years.chunked(4).forEach { row ->
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
