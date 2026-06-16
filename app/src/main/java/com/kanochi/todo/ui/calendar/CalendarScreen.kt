package com.kanochi.todo.ui.calendar
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
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
import kotlinx.coroutines.coroutineScope
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
    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }

    val selectedDateStart = remember(selectedDate.value) {
        val c = selectedDate.value.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        c.timeInMillis
    }
    val selectedDateEnd = selectedDateStart + 86400000L

    val todosForDate by repository.getTodosForDate(selectedDateStart, selectedDateEnd)
        .collectAsState(initial = emptyList())

    // Calendar expand/collapse state
    var expandProgress by remember { mutableStateOf(0f) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "侧边栏", tint = AppSurface)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${currentMonth.value.get(Calendar.YEAR)}年",
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppSurface,
                            modifier = Modifier.clickable { showYearPicker = true }
                        )
                        Text(
                            text = "${currentMonth.value.get(Calendar.MONTH) + 1}月",
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppSurface,
                            modifier = Modifier.clickable { showMonthPicker = true }
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = AppSurface)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = PrimaryBlue, titleContentColor = AppSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddTodo(selectedDateStart) },
                containerColor = PrimaryBlue, contentColor = AppSurface, shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
            ) { Icon(Icons.Default.Add, contentDescription = "新建任务") }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(AppBackground)
        ) {
            Column(Modifier.fillMaxWidth().background(PrimaryBlue)) {
                WeekdayHeader()

                CalendarArea(
                    month = currentMonth.value,
                    selectedDate = selectedDate.value,
                    today = today,
                    expandProgress = expandProgress,
                    onExpandProgressChange = { expandProgress = it },
                    onDateSelected = { selectedDate.value = it },
                    onSwipeLeft = {
                        if (expandProgress > 0.5f) {
                            currentMonth.value = currentMonth.value.clone() as Calendar
                            currentMonth.value.add(Calendar.MONTH, 1)
                        } else {
                            selectedDate.value = selectedDate.value.clone() as Calendar
                            selectedDate.value.add(Calendar.DAY_OF_MONTH, 7)
                        }
                    },
                    onSwipeRight = {
                        if (expandProgress > 0.5f) {
                            currentMonth.value = currentMonth.value.clone() as Calendar
                            currentMonth.value.add(Calendar.MONTH, -1)
                        } else {
                            selectedDate.value = selectedDate.value.clone() as Calendar
                            selectedDate.value.add(Calendar.DAY_OF_MONTH, -7)
                        }
                    }
                )
            }

            HorizontalDivider(color = AppBorder, thickness = 0.5.dp)

            if (todosForDate.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无待办事项", color = TextTertiary, fontSize = 14.sp)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(todosForDate, key = { it.id }) { todo ->
                        TodoRow(todo, onToggle = { scope.launch { repository.toggleStatus(todo.id) } },
                            onDelete = { scope.launch { repository.deleteTodo(todo.id) } },
                            onEdit = { })
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showMonthPicker) {
        MonthPickerDialog(currentMonth.value, onDismiss = { showMonthPicker = false },
            onMonthSelected = { m -> currentMonth.value.set(Calendar.MONTH, m); showMonthPicker = false })
    }
    if (showYearPicker) {
        YearPickerDialog(
            currentYear = currentMonth.value.get(Calendar.YEAR),
            onDismiss = { showYearPicker = false },
            onYearSelected = { y -> currentMonth.value.set(Calendar.YEAR, y); showYearPicker = false }
        )
    }
}

@Composable
private fun WeekdayHeader() {
    val weekdays = listOf("一", "二", "三", "四", "五", "六", "日")
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 0.dp).height(20.dp),
        verticalAlignment = Alignment.CenterVertically) {
        weekdays.forEachIndexed { i, d ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(d, textAlign = TextAlign.Center, color = AppSurface.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun CalendarArea(
    month: Calendar,
    selectedDate: Calendar,
    today: Calendar,
    expandProgress: Float,
    onExpandProgressChange: (Float) -> Unit,
    onDateSelected: (Calendar) -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
) {
    val weeks = remember(month) { getWeeksForMonth(month) }
    val weekHeight = 44.dp
    val minHeight = weekHeight
    val maxHeight = weekHeight * weeks.size

    val calHeight = minHeight + (maxHeight - minHeight) * expandProgress

    // Index of the week containing the selected date (for collapsed offset)
    val weekIndex = remember(weeks, selectedDate) {
        weeks.indexOfFirst { week ->
            week.any { day -> day != null && isSameDay(day, selectedDate) }
        }.coerceAtLeast(0)
    }

    Column(Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(calHeight)
                .clipToBounds()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var totalX = 0f
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            totalX += change.positionChange().x
                            if (abs(totalX) > SWIPE_THRESHOLD) {
                                change.consume()
                                while (event.changes.any { it.pressed }) {
                                    awaitPointerEvent().changes.firstOrNull { it.id == down.id }?.consume()
                                }
                                if (totalX > 0) onSwipeRight() else onSwipeLeft()
                                break
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .graphicsLayer {
                        // Offset so current week is at top when collapsed
                        val weekPx = 42f * density
                        translationY = -(weekIndex * weekPx) * (1f - expandProgress)
                    }
            ) {
                weeks.forEachIndexed { i, week ->
                    WeekRow(week, month, selectedDate, today, onDateSelected)
                    if (i < weeks.size - 1) Spacer(Modifier.height(2.dp))
                }
            }
        }

        // Drag handle — 40dp touch area, visual bar centered
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            val delta = change.positionChange().y
                            if (abs(delta) > 0.5f) {
                                val target = (expandProgress + delta / 400f).coerceIn(0f, 1f)
                                onExpandProgressChange(target)
                            }
                        } while (event.changes.any { it.pressed })
                        onExpandProgressChange(if (expandProgress > 0.3f) 1f else 0f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.width(32.dp).height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(AppSurface.copy(alpha = 0.5f))
            )
        }
    }
}

@Composable
private fun WeekRow(
    week: Array<Calendar?>, month: Calendar, selectedDate: Calendar, today: Calendar,
    onDateSelected: (Calendar) -> Unit
) {
    Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0..6) {
            val day = week[i]
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (day != null) {
                    DayCell(day, isSameDay(day, today), isSameDay(day, selectedDate),
                        day.get(Calendar.MONTH) == month.get(Calendar.MONTH), onClick = { onDateSelected(day) })
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: Calendar, isToday: Boolean, isSelected: Boolean, isCurrentMonth: Boolean, onClick: () -> Unit) {
    val bg = animateColorAsState(when { isSelected -> AppSurface; isToday -> AppSurface.copy(alpha = 0.2f); else -> Color.Transparent }, label = "bg")
    val tc = when { isSelected -> PrimaryBlue; isToday -> AppSurface; !isCurrentMonth -> AppSurface.copy(alpha = 0.35f); else -> AppSurface.copy(alpha = 0.85f) }
    val isWk = day.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || day.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
    Box(Modifier.size(36.dp).clip(CircleShape).background(bg.value).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text("${day.get(Calendar.DAY_OF_MONTH)}", color = if (isWk && !isToday && !isSelected) AppSurface.copy(alpha = 0.6f) else tc,
            fontSize = 14.sp, fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun TodoRow(todo: TodoEntity, onToggle: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit) {
    val done = todo.status == "completed"
    val pc = when (Priority.fromString(todo.priority)) { Priority.HIGH -> HighPriority; Priority.MEDIUM -> MediumPriority; Priority.LOW -> LowPriority }
    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(containerColor = AppSurface), shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, AppBorder)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = done, onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = CompletedGreen, uncheckedColor = TextTertiary, checkmarkColor = AppSurface))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(todo.title, color = if (done) CompletedText else TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None)
                if (todo.description.isNotBlank()) Text(todo.description, color = TextTertiary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(8.dp).clip(CircleShape).background(pc))
        }
    }
}

// Helpers
private fun getWeeksForMonth(c: Calendar): List<Array<Calendar?>> {
    val cal = c.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val off = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
    cal.add(Calendar.DAY_OF_MONTH, -off)
    val weeks = mutableListOf<Array<Calendar?>>()
    val total = off + c.getActualMaximum(Calendar.DAY_OF_MONTH)
    for (w in 0 until (total + 6) / 7) {
        val week = arrayOfNulls<Calendar>(7)
        for (d in 0..6) { week[d] = cal.clone() as Calendar; cal.add(Calendar.DAY_OF_MONTH, 1) }
        weeks.add(week)
    }
    cal.timeInMillis = c.timeInMillis; return weeks
}

private fun isSameDay(a: Calendar, b: Calendar) = a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

@Composable
private fun MonthPickerDialog(cm: Calendar, onDismiss: () -> Unit, onMonthSelected: (Int) -> Unit) {
    val ms = listOf("1月","2月","3月","4月","5月","6月","7月","8月","9月","10月","11月","12月")
    val cur = cm.get(Calendar.MONTH)
    AlertDialog(onDismissRequest = onDismiss, title = { Text("选择月份", color = TextPrimary) },
        text = { Column { ms.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { row.forEach { m ->
                val i = ms.indexOf(m)
                TextButton(onClick = { onMonthSelected(i) }, colors = ButtonDefaults.textButtonColors(contentColor = if (i==cur) PrimaryBlue else TextPrimary))
                { Text(m, fontWeight = if (i==cur) FontWeight.Bold else FontWeight.Normal) }
            }}
        }}},
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) } },
        containerColor = AppSurface, titleContentColor = TextPrimary)
}

@Composable
private fun YearPickerDialog(currentYear: Int, onDismiss: () -> Unit, onYearSelected: (Int) -> Unit) {
    val years = (currentYear - 5..currentYear + 5).toList()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("选择年份", color = TextPrimary) },
        text = {
            Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                years.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { row.forEach { y ->
                        TextButton(onClick = { onYearSelected(y) },
                            colors = ButtonDefaults.textButtonColors(contentColor = if (y == currentYear) PrimaryBlue else TextPrimary))
                        { Text("${y}年", fontWeight = if (y == currentYear) FontWeight.Bold else FontWeight.Normal) }
                    }}
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) } },
        containerColor = AppSurface, titleContentColor = TextPrimary)
}
