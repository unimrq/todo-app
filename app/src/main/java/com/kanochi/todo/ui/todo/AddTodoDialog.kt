package com.kanochi.todo.ui.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kanochi.todo.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoDialog(
    initialDate: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, priority: String, category: String, dueDate: Long?) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf("default") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedDateOption by remember { mutableStateOf(if (initialDate != null) "custom" else "none") }
    var dueDate by remember { mutableStateOf(initialDate ?: 0L) }
    var showDatePicker by remember { mutableStateOf(false) }
    var serverOnline by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()

    // Server health check
    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) {
                val url = URL("http://10.0.2.2:8765/health")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                val code = conn.responseCode
                serverOnline = code == 200
                conn.disconnect()
            }
        } catch (_: Exception) { serverOnline = false }
    }

    val todayCal = remember { Calendar.getInstance() }
    val todayStart: Long = remember {
        val c = todayCal.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        c.timeInMillis
    }
    val tomorrowStart = todayStart + 86400000L

    // Compute due date based on selection
    val computedDueDate: Long? = when (selectedDateOption) {
        "today" -> todayStart
        "tomorrow" -> tomorrowStart
        "custom" -> if (dueDate > 0) dueDate else null
        else -> null
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppSurface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())
            ) {
                // Title with server indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "新建任务", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = TextPrimary, modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier.size(10.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(when (serverOnline) {
                                true -> CompletedGreen; false -> HighPriority; null -> TextTertiary
                            })
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Text input (3 lines)
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    placeholder = { Text("输入任务内容...", color = TextTertiary) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue, unfocusedBorderColor = AppBorder,
                        cursorColor = PrimaryBlue, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(Modifier.height(16.dp))

                // Priority chips
                Text("优先级", color = TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                val priorityOptions = listOf(
                    Triple("default", "默认", TextTertiary),
                    Triple("high", "高", HighPriority),
                    Triple("medium", "中", MediumPriority),
                    Triple("low", "低", PrimaryBlue)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    priorityOptions.forEach { (value, label, color) ->
                        val isSel = selectedPriority == value
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedPriority = value },
                            label = { Text(label, fontSize = 12.sp, color = if (isSel) color else TextSecondary) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = AppSurfaceVariant, selectedContainerColor = AppSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = AppBorder, selectedBorderColor = color, enabled = true, selected = isSel
                            )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Date chips
                Text("截止日期", color = TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val dateOptions = listOf("none" to "无", "today" to "今天", "tomorrow" to "明天", "custom" to "选择…")
                    dateOptions.forEach { (value, label) ->
                        val isSel = selectedDateOption == value
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                selectedDateOption = value
                                if (value == "custom") showDatePicker = true
                            },
                            label = {
                                Text(
                                    text = if (isSel && value == "custom") {
                                        SimpleDateFormat("M/d", Locale.CHINESE).format(Date(dueDate))
                                    } else label,
                                    fontSize = 12.sp, color = if (isSel) PrimaryBlue else TextSecondary
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = AppSurfaceVariant, selectedContainerColor = AppSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = AppBorder, selectedBorderColor = PrimaryBlue, enabled = true, selected = isSel
                            )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Category chips
                Text("分类", color = TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                val categoryOptions = listOf("", "工作", "生活", "学习", "其他")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categoryOptions.forEach { cat ->
                        val isSel = selectedCategory == cat
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedCategory = if (isSel && cat.isNotEmpty()) "" else cat },
                            label = { Text(if (cat.isEmpty()) "无" else cat, fontSize = 12.sp, color = if (isSel) PrimaryBlue else TextSecondary) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = AppSurfaceVariant, selectedContainerColor = AppSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = AppBorder, selectedBorderColor = PrimaryBlue, enabled = true, selected = isSel
                            )
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (text.isNotBlank()) {
                                onConfirm(
                                    text.trim(), "",
                                    if (selectedPriority == "default") "medium" else selectedPriority,
                                    selectedCategory,
                                    computedDueDate
                                )
                            }
                        },
                        enabled = text.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = AppSurface),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("创建") }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            initialDate = if (dueDate > 0) dueDate else System.currentTimeMillis(),
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                dueDate = date
                showDatePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(initialDate: Long, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = if (initialDate > 0) initialDate else null)
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = AppSurface)) {
            Column(Modifier.padding(16.dp)) {
                DatePicker(state = state, colors = DatePickerDefaults.colors(
                    containerColor = AppSurface, titleContentColor = TextPrimary, headlineContentColor = TextPrimary,
                    weekdayContentColor = TextSecondary, subheadContentColor = TextSecondary,
                    yearContentColor = TextPrimary, currentYearContentColor = PrimaryBlue,
                    selectedYearContentColor = AppSurface, dayContentColor = TextPrimary,
                    selectedDayContentColor = AppSurface, selectedDayContainerColor = PrimaryBlue,
                    dayInSelectionRangeContentColor = TextPrimary, dayInSelectionRangeContainerColor = PrimaryBlueVariant,
                    todayContentColor = BlueAccent, todayDateBorderColor = BlueAccent
                ))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
                    TextButton(onClick = { state.selectedDateMillis?.let { onConfirm(it) } }) { Text("确定", color = PrimaryBlue) }
                }
            }
        }
    }
}
