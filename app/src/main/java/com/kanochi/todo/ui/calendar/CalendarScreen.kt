package com.kanochi.todo.ui.calendar
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
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
    onOpenDrawer: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val today = remember { Calendar.getInstance() }
    val currentMonth = remember { mutableStateOf(Calendar.getInstance()) }
    val selectedDate = remember { mutableStateOf(Calendar.getInstance()) }
    var showYearMonthPicker by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<TodoEntity?>(null) }

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
    var isExpanded by remember { mutableStateOf(false) }
    var expandProgress by remember { mutableStateOf(0f) }

    // Drag-end decision: snap to nearest with animation
    val onDragEnd: () -> Unit = {
        val shouldExpand = when {
            isExpanded && expandProgress < 0.5f -> false
            isExpanded -> true
            !isExpanded && expandProgress > 0.5f -> true
            else -> false
        }
        val target = if (shouldExpand) 1f else 0f
        isExpanded = shouldExpand
        scope.launch {
            val start = expandProgress
            animate(start, target, animationSpec = tween(200)) { value, _ ->
                expandProgress = value
            }
        }
    }

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
                        IconButton(onClick = {
                            if (isExpanded) {
                                val cm = currentMonth.value.clone() as Calendar
                                cm.add(Calendar.MONTH, -1)
                                currentMonth.value = cm
                            } else {
                                val sd = selectedDate.value.clone() as Calendar
                                sd.add(Calendar.DAY_OF_MONTH, -7)
                                selectedDate.value = sd
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上一周/月", tint = AppSurface)
                        }
                        Text(
                            text = "${currentMonth.value.get(Calendar.YEAR)}年${currentMonth.value.get(Calendar.MONTH) + 1}月",
                            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppSurface,
                            modifier = Modifier.clickable { showYearMonthPicker = true }
                        )
                        IconButton(onClick = {
                            if (isExpanded) {
                                val cm = currentMonth.value.clone() as Calendar
                                cm.add(Calendar.MONTH, 1)
                                currentMonth.value = cm
                            } else {
                                val sd = selectedDate.value.clone() as Calendar
                                sd.add(Calendar.DAY_OF_MONTH, 7)
                                selectedDate.value = sd
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下一周/月", tint = AppSurface)
                        }
                    }
                },
                actions = {},
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = PrimaryBlue, titleContentColor = AppSurface
                )
            )
        },
        floatingActionButton = {}
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
                        if (isExpanded) {
                            val cm = currentMonth.value.clone() as Calendar
                            cm.add(Calendar.MONTH, 1)
                            currentMonth.value = cm
                        } else {
                            val sd = selectedDate.value.clone() as Calendar
                            sd.add(Calendar.DAY_OF_MONTH, 7)
                            selectedDate.value = sd
                        }
                    },
                    onSwipeRight = {
                        if (isExpanded) {
                            val cm = currentMonth.value.clone() as Calendar
                            cm.add(Calendar.MONTH, -1)
                            currentMonth.value = cm
                        } else {
                            val sd = selectedDate.value.clone() as Calendar
                            sd.add(Calendar.DAY_OF_MONTH, -7)
                            selectedDate.value = sd
                        }
                    },
                    onDragEnd = onDragEnd
                )
            }

            HorizontalDivider(color = AppBorder, thickness = 0.5.dp)

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    scope.launch {
                        try {
                            repository.fullSync()
                        } catch (_: Exception) { }
                        isRefreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize(),
                indicatorColors = PullToRefreshDefaults.colors(
                    containerColor = AppSurface
                )
            ) {
                LazyColumn(Modifier.fillMaxSize()) {
                    if (todosForDate.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().height(150.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                                Text("暂无待办事项", color = TextTertiary, fontSize = 14.sp)
                            }
                        }
                    } else {
                        items(todosForDate, key = { it.id }) { todo ->
                            TodoRow(todo,
                                onToggle = { scope.launch { repository.toggleStatus(todo.id) } },
                                onEdit = { editingTodo = todo },
                                onDelete = { scope.launch { repository.deleteTodo(todo.id) } })
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showYearMonthPicker) {
        YearMonthPickerDialog(
            currentYear = currentMonth.value.get(Calendar.YEAR),
            currentMonth = currentMonth.value.get(Calendar.MONTH),
            onDismiss = { showYearMonthPicker = false },
            onSelected = { y, m ->
                currentMonth.value.set(Calendar.YEAR, y)
                currentMonth.value.set(Calendar.MONTH, m)
                showYearMonthPicker = false
            }
        )
    }

    editingTodo?.let { todo ->
        TodoEditDialog(
            todo = todo,
            onDismiss = { editingTodo = null },
            onSave = { updated ->
                scope.launch {
                    repository.updateTodo(updated)
                }
                editingTodo = null
            }
        )
    }
}

@Composable
private fun WeekdayHeader() {
    val weekdays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp).height(18.dp),
        verticalAlignment = Alignment.CenterVertically) {
        weekdays.forEachIndexed { i, d ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(d, textAlign = TextAlign.Center,
                    color = AppSurface.copy(alpha = 0.45f),
                    fontSize = 11.sp, fontWeight = FontWeight.Normal)
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
    onSwipeRight: () -> Unit,
    onDragEnd: () -> Unit
) {
    val weeks = remember(month) { getWeeksForMonth(month) }
    val weekHeight = 44.dp
    val minHeight = 40.dp
    val maxHeight = weekHeight * weeks.size

    val calHeight = minHeight + (maxHeight - minHeight) * expandProgress

    // Index of the week containing the selected date (for collapsed view)
    val weekIndex = remember(weeks, selectedDate) {
        weeks.indexOfFirst { week ->
            week.any { day -> day != null && isSameDay(day, selectedDate) }
        }.coerceAtLeast(0)
    }

    // Collapsed: show only current week; Expanded: show all weeks
    val displayWeeks = if (expandProgress < 0.01f) listOf(weeks[weekIndex]) else weeks

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
            ) {
                displayWeeks.forEachIndexed { i, week ->
                    WeekRow(week, month, selectedDate, today, onDateSelected)
                    if (i < displayWeeks.size - 1) Spacer(Modifier.height(2.dp))
                }
            }
        }

        // Drag handle — 24dp touch area, visual bar centered
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var totalDeltaY = 0f
                        val startProgress = expandProgress
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            totalDeltaY += change.positionChange().y
                            if (abs(totalDeltaY) > 5f) {
                                val target = (startProgress + totalDeltaY / 200f).coerceIn(0f, 1f)
                                onExpandProgressChange(target)
                                change.consume()
                            }
                        } while (event.changes.any { it.pressed })
                        onDragEnd()
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
private fun TodoRow(todo: TodoEntity, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val done = todo.status == "completed"
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val actionWidth = 150f

    val priority = Priority.fromString(todo.priority)
    val pc = when (priority) { Priority.HIGH -> HighPriority; Priority.MEDIUM -> MediumPriority; Priority.LOW -> LowPriority }

    Box(
        Modifier
            .fillMaxWidth()
            .clipToBounds()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var dragX = 0f
                    var captured = false

                    do {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (change.pressed) {
                            val dx = change.positionChange().x
                            val dy = change.positionChange().y
                            dragX += dx

                            // Once drag is clearly horizontal, consume and move
                            if (!captured && abs(dragX) > abs(dy) + 8f && abs(dragX) > 8f) {
                                captured = true
                            }

                            if (captured) {
                                change.consume()
                                scope.launch {
                                    val target = (offset.value + dx).coerceIn(-actionWidth, 0f)
                                    offset.snapTo(target)
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    // Snap on release
                    scope.launch {
                        if (offset.value < -actionWidth / 3) {
                            offset.animateTo(-actionWidth)
                        } else {
                            offset.animateTo(0f)
                        }
                    }
                }
            }
    ) {
        // Background: action buttons on the right
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    scope.launch { offset.animateTo(0f) }
                    onEdit()
                }
            ) {
                Text("编辑", color = PrimaryBlue, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
            Spacer(Modifier.width(8.dp))
            TextButton(
                onClick = {
                    scope.launch { offset.animateTo(0f) }
                    onDelete()
                }
            ) {
                Text("删除", color = HighPriority, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
            Spacer(Modifier.width(8.dp))
        }

        // Foreground: swipeable task content
        Row(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .background(AppSurface),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left priority color bar
            Box(Modifier.width(5.dp).fillMaxHeight().background(pc))

            Spacer(Modifier.width(10.dp))

            Checkbox(checked = done, onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = CompletedGreen, uncheckedColor = TextTertiary, checkmarkColor = AppSurface))

            Spacer(Modifier.width(6.dp))

            Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                Text(todo.title, color = if (done) CompletedText else TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None)
                if (todo.category.isNotBlank()) {
                    Text(todo.category, color = TextTertiary, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }

    // Bottom separator
    HorizontalDivider(color = AppBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
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
private fun YearMonthPickerDialog(
    currentYear: Int, currentMonth: Int,
    onDismiss: () -> Unit, onSelected: (Int, Int) -> Unit
) {
    var selYear by remember { mutableStateOf(currentYear) }
    val years = (currentYear - 5..currentYear + 5).toList()
    val months = listOf("1月","2月","3月","4月","5月","6月","7月","8月","9月","10月","11月","12月")
    AlertDialog(onDismissRequest = onDismiss, title = { Text("选择年月", color = TextPrimary) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    years.forEach { y ->
                        TextButton(onClick = { selYear = y },
                            colors = ButtonDefaults.textButtonColors(contentColor = if (y == selYear) PrimaryBlue else TextPrimary))
                        { Text("${y}年", fontWeight = if (y == selYear) FontWeight.Bold else FontWeight.Normal) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                months.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { m ->
                            val i = months.indexOf(m)
                            TextButton(onClick = { onSelected(selYear, i) },
                                colors = ButtonDefaults.textButtonColors(contentColor = if (i == currentMonth && selYear == currentYear) PrimaryBlue else TextPrimary))
                            { Text(m, fontWeight = if (i == currentMonth && selYear == currentYear) FontWeight.Bold else FontWeight.Normal) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) } },
        containerColor = AppSurface, titleContentColor = TextPrimary)
}

@Composable
private fun TodoEditDialog(
    todo: TodoEntity,
    onDismiss: () -> Unit,
    onSave: (TodoEntity) -> Unit
) {
    val priorities = listOf("high", "medium", "low")
    val priorityLabels = listOf("高", "中", "低")
    val pc = { p: String -> when (p) { "high" -> HighPriority; "medium" -> MediumPriority; else -> LowPriority } }

    var title by remember { mutableStateOf(todo.title) }
    var description by remember { mutableStateOf(todo.description) }
    var category by remember { mutableStateOf(todo.category) }
    var priority by remember { mutableStateOf(todo.priority) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑任务", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue, focusedLabelColor = PrimaryBlue)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述") },
                    singleLine = false,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue, focusedLabelColor = PrimaryBlue)
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("分类") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue, focusedLabelColor = PrimaryBlue)
                )
                // Priority selector
                Text("优先级", fontSize = 13.sp, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    priorities.forEachIndexed { i, p ->
                        val isSelected = priority == p
                        FilterChip(
                            selected = isSelected,
                            onClick = { priority = p },
                            label = { Text(priorityLabels[i], fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = pc(p).copy(alpha = 0.12f),
                                selectedLabelColor = pc(p)
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(todo.copy(
                            title = title.trim(),
                            description = description.trim(),
                            category = category.trim(),
                            priority = priority,
                            updatedAt = System.currentTimeMillis()
                        ))
                    }
                },
                enabled = title.isNotBlank()
            ) { Text("保存", color = PrimaryBlue) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        },
        containerColor = AppSurface,
        titleContentColor = TextPrimary
    )
}
