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
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
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
    var deletingTodo by remember { mutableStateOf<TodoEntity?>(null) }

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
                            val cm = currentMonth.value.clone() as Calendar
                            cm.add(Calendar.MONTH, -1)
                            currentMonth.value = cm
                        }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上一月", tint = AppSurface)
                        }
                        Text(
                            text = "${currentMonth.value.get(Calendar.YEAR)}年${currentMonth.value.get(Calendar.MONTH) + 1}月",
                            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppSurface,
                            modifier = Modifier.clickable { showYearMonthPicker = true }
                        )
                        IconButton(onClick = {
                            val cm = currentMonth.value.clone() as Calendar
                            cm.add(Calendar.MONTH, 1)
                            currentMonth.value = cm
                        }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下一月", tint = AppSurface)
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppBackground)
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
                modifier = Modifier.fillMaxSize()
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
                                onDelete = { deletingTodo = todo })
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

    deletingTodo?.let { todo ->
        AlertDialog(
            onDismissRequest = { deletingTodo = null },
            title = { Text("删除任务", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("确定删除「${todo.title}」？", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.deleteTodo(todo.id)
                        }
                        deletingTodo = null
                    }
                ) { Text("删除", color = HighPriority) }
            },
            dismissButton = {
                TextButton(onClick = { deletingTodo = null }) { Text("取消", color = TextSecondary) }
            },
            containerColor = AppSurface,
            titleContentColor = TextPrimary
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
                    color = AppSurface,
                    fontSize = 11.sp, fontWeight = FontWeight.Normal)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
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
    val weekHeight = 40.dp
    val weekSpacer = 2.dp
    val density = LocalDensity.current

    // Compute which week of the month contains the selected date.
    // If selectedDate is outside the current month, use its day-of-month
    // clamped to the current month's max days (so we always show a valid week).
    val weekIndex = remember(selectedDate, month, weeks) {
        val dayOfMonth = selectedDate.get(Calendar.DAY_OF_MONTH)
            .coerceAtMost(month.getActualMaximum(Calendar.DAY_OF_MONTH))
        val firstOfMonth = month.clone() as Calendar
        firstOfMonth.set(Calendar.DAY_OF_MONTH, 1)
        val offFromMon = (firstOfMonth.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        ((offFromMon + dayOfMonth - 1) / 7).coerceIn(0, weeks.size - 1)
    }

    val weekStep = weekHeight + weekSpacer
    val collapsedHeight = weekStep + 4.dp
    val expandedHeight = weekHeight * weeks.size + weekSpacer * (weeks.size - 1)

    // Two-phase animation:
    // Phase 1 (0→0.5): current week slides from collapsed to expanded position, box grows
    // Phase 2 (0.5→1): other weeks slide/fade in
    val p1 = (expandProgress / 0.5f).coerceAtMost(1f)
    val p2 = ((expandProgress - 0.5f) / 0.5f).coerceAtLeast(0f)

    val weekStepPx = with(density) { weekStep.toPx() }
    val collapsedOffsetPx = -(weekIndex * weekStepPx)

    // Offset: at p1=0, current week at top (offset = -weekIndex*weekStep)
    //        at p1=1, current week at its natural position (offset = 0)
    val offsetPx = if (expandProgress < 0.5f) {
        // 收缩：让当前周显示在顶部，再多往下偏移一些
        val topOffset = -(weekIndex * weekStepPx)
        // 限制偏移量，确保不超出可见区域
        val maxOffset = with(density) { (collapsedHeight - weekHeight).toPx() }
        // 这里加一个偏移量，让日期往下移，比如 8.dp
        val extraOffset = with(density) { 8.dp.toPx() }
        (topOffset.coerceAtLeast(-maxOffset) + extraOffset).toInt()
    } else {
        lerp(0f, 0f, p2).toInt()
    }

    // Box height grows as current week moves down
    val currentWeekMovePx = lerp(0f, -(collapsedOffsetPx), p1)
    val growingHeight = with(density) { collapsedHeight.toPx() } + currentWeekMovePx
    // Phase 2: grow to full expanded height
    val expandedHeightPx = with(density) { expandedHeight.toPx() }
    val calHeightPx = if (expandProgress < 0.5f) growingHeight
                      else lerp(growingHeight, expandedHeightPx, p2)

    val calHeight = (calHeightPx / density.density).dp

    val dragRange = 400f

    Column(Modifier.fillMaxWidth()) {
        // Calendar grid — always render all weeks, offset continuously
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(calHeight)
                .clipToBounds()
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        val target = (expandProgress + delta / dragRange).coerceIn(0f, 1f)
                        onExpandProgressChange(target)
                    },
                    onDragStopped = { onDragEnd() }
                )
        ) {
            Column(
                modifier = Modifier
                    .offset { IntOffset(0, offsetPx) }
                    .fillMaxWidth()
                    .wrapContentHeight(unbounded = true)
                    .padding(horizontal = 8.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = { },
                            onHorizontalDrag = { _, dragAmount ->
                                if (abs(dragAmount) > 80f) {
                                    if (dragAmount > 0) onSwipeRight() else onSwipeLeft()
                                }
                            }
                        )
                    }
            ) {
                weeks.forEachIndexed { i, week ->
                    val weekAlpha = if (i == weekIndex) 1f else p2
                    WeekRow(
                        week, month, selectedDate, today, onDateSelected,
                        modifier = if (weekAlpha < 1f) Modifier.alpha(weekAlpha) else Modifier
                    )
                    if (i < weeks.size - 1) {
                        if (i != weekIndex) Spacer(Modifier.height(weekSpacer).alpha(p2))
                        else Spacer(Modifier.height(weekSpacer))
                    }
                }
            }
        }

        // Drag handle bar — same draggable logic as the grid area above
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        val target = (expandProgress + delta / dragRange).coerceIn(0f, 1f)
                        onExpandProgressChange(target)
                    },
                    onDragStopped = { onDragEnd() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.width(64.dp).height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(AppSurface.copy(alpha = 0.5f))
            )
        }
    }
}

@Composable
private fun WeekRow(
    week: Array<Calendar?>, month: Calendar, selectedDate: Calendar, today: Calendar,
    onDateSelected: (Calendar) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(Modifier.fillMaxWidth().height(40.dp).then(modifier), verticalAlignment = Alignment.CenterVertically) {
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
        val density = LocalDensity.current
        val actionWidth = with(density) { 160.dp.toPx() }

        val priority = Priority.fromString(todo.priority)
        val pc = when (priority) { Priority.HIGH -> HighPriority; Priority.MEDIUM -> MediumPriority; Priority.LOW -> LowPriority }

        Box(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
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
                TextButton(onClick = onEdit,
                    modifier = Modifier.width(80.dp).height(48.dp)
                ) { Text("编辑", color = AppSurface, fontSize = 12.sp) }
                TextButton(onClick = onDelete,
                    modifier = Modifier.width(80.dp).height(48.dp)
                ) { Text("删除", color = AppSurface, fontSize = 12.sp) }
            }

            // Foreground: swipeable content
            Row(
                Modifier
                    .offset { IntOffset(offset.value.roundToInt(), 0) }
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .background(AppSurface)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (offset.value < -actionWidth / 2) {
                                scope.launch { offset.animateTo(0f) }
                            } else {
                                onToggle()
                            }
                        }
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority vertical bar
                Box(
                    Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(pc)
                )
                Spacer(Modifier.width(12.dp))
                // Checkbox
                Checkbox(
                    checked = done,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = pc,
                        uncheckedColor = TextSecondary,
                        checkmarkColor = AppSurface
                    ),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                // Title + description + category
                Column(Modifier.weight(1f)) {
                    Text(
                        todo.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (done) TextTertiary else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None
                    )
                    if (!todo.description.isNullOrBlank()) {
                        Text(
                            todo.description,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                // Category label on the right
                val cat = Category.fromName(todo.category)
                if (cat != Category.NONE) {
                    val catColor = when (cat) {
                        Category.SCHEDULE -> CategorySchedule
                        Category.WORK -> CategoryWork
                        Category.STUDY -> CategoryStudy
                        Category.EXERCISE -> CategoryExercise
                        Category.LIFE -> CategoryLife
                        Category.PERSONAL -> CategoryPersonal
                        Category.NONE -> TextTertiary
                    }
                    Text(
                        "#${cat.displayName}",
                        fontSize = 11.sp,
                        color = catColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
