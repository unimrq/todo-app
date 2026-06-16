package com.kanochi.todo.ui.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kanochi.todo.ui.theme.*
import kotlinx.coroutines.Dispatchers
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
    var selectedDateOption by remember { mutableStateOf("default") }
    var dueDate by remember { mutableStateOf(initialDate ?: 0L) }
    var showDatePicker by remember { mutableStateOf(false) }
    var serverOnline by remember { mutableStateOf<Boolean?>(null) }

    // Server health check
    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) {
                val url = URL("http://8.138.122.116:8765/health")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 3000; conn.readTimeout = 3000
                serverOnline = conn.responseCode == 200
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

    val computedDueDate: Long? = when (selectedDateOption) {
        "today" -> todayStart
        "tomorrow" -> tomorrowStart
        "custom" -> if (dueDate > 0) dueDate else null
        else -> null
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AppSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())
            ) {
                // Title with server dot
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("新建任务", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        color = TextPrimary, modifier = Modifier.weight(1f))
                    Box(Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background(when (serverOnline) { true -> CompletedGreen; false -> HighPriority; null -> TextTertiary }))
                }

                Spacer(Modifier.height(12.dp))

                // Text input (4 lines min)
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    placeholder = { Text("输入任务内容...", fontSize = 14.sp, color = TextTertiary) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    maxLines = 6,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue, unfocusedBorderColor = AppBorder,
                        cursorColor = PrimaryBlue, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(Modifier.height(12.dp))

                // Priority row - label and chips on same line
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("优先级", fontSize = 12.sp, color = TextSecondary,
                        modifier = Modifier.width(40.dp))
                    Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(Triple("default","默认",TextTertiary), Triple("high","高",HighPriority),
                            Triple("medium","中",MediumPriority), Triple("low","低",PrimaryBlue)
                        ).forEach { (v, label, c) ->
                            val sel = selectedPriority == v
                            FilterChip(selected = sel, onClick = { selectedPriority = v },
                                label = { Text(label, fontSize = 11.sp, color = if (sel) c else TextSecondary) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = AppSurfaceVariant, selectedContainerColor = AppSurfaceVariant),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = AppBorder, selectedBorderColor = c, enabled = true, selected = sel),
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Date row
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("日期", fontSize = 12.sp, color = TextSecondary,
                        modifier = Modifier.width(40.dp))
                    Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("default" to "默认", "today" to "今天", "tomorrow" to "明天", "custom" to "选择").forEach { (v, label) ->
                            val sel = selectedDateOption == v
                            val displayLabel = if (sel && v == "custom") {
                                try { SimpleDateFormat("M/d", Locale.CHINESE).format(Date(dueDate)) } catch (_: Exception) { label }
                            } else label
                            FilterChip(selected = sel, onClick = {
                                selectedDateOption = v
                                if (v == "custom") showDatePicker = true
                            }, label = { Text(displayLabel, fontSize = 11.sp, color = if (sel) PrimaryBlue else TextSecondary) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = AppSurfaceVariant, selectedContainerColor = AppSurfaceVariant),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = AppBorder, selectedBorderColor = PrimaryBlue, enabled = true, selected = sel),
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Category row
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("分类", fontSize = 12.sp, color = TextSecondary,
                        modifier = Modifier.width(40.dp))
                    Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("", "工作", "生活", "学习", "个人", "其他").forEach { cat ->
                            val sel = selectedCategory == cat
                            FilterChip(selected = sel, onClick = { selectedCategory = if (sel && cat.isNotEmpty()) "" else cat },
                                label = { Text(if (cat.isEmpty()) "默认" else cat, fontSize = 11.sp,
                                    color = if (sel) PrimaryBlue else TextSecondary) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = AppSurfaceVariant, selectedContainerColor = AppSurfaceVariant),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = AppBorder, selectedBorderColor = PrimaryBlue, enabled = true, selected = sel),
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Buttons - "确认" as text button, same as "取消"
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", fontSize = 13.sp, color = TextSecondary)
                    }
                    Spacer(Modifier.width(4.dp))
                    TextButton(
                        onClick = {
                            if (text.isNotBlank()) onConfirm(text.trim(), "",
                                if (selectedPriority == "default") "medium" else selectedPriority,
                                selectedCategory, computedDueDate)
                        },
                        enabled = text.isNotBlank()
                    ) {
                        Text("确认", fontSize = 13.sp,
                            color = if (text.isNotBlank()) PrimaryBlue else TextTertiary)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            initialDate = if (dueDate > 0) dueDate else System.currentTimeMillis(),
            onDismiss = { showDatePicker = false },
            onConfirm = { date -> dueDate = date; showDatePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(initialDate: Long, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = if (initialDate > 0) initialDate else null)
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = AppSurface)) {
            Column(Modifier.padding(12.dp)) {
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
