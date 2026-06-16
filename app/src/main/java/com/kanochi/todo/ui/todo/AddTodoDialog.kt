package com.kanochi.todo.ui.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.kanochi.todo.data.model.Priority
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
    var selectedPriority by remember { mutableStateOf("medium") }
    var selectedCategory by remember { mutableStateOf("") }
    var hasDueDate by remember { mutableStateOf(initialDate != null) }
    var dueDate by remember { mutableStateOf(initialDate ?: 0L) }
    var showDatePicker by remember { mutableStateOf(false) }
    // Server status
    var serverOnline by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()

    // Check server status
    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) {
                val url = URL("http://10.0.2.2:8765/health")  // Android emulator localhost
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                val code = conn.responseCode
                serverOnline = code == 200
                conn.disconnect()
            }
        } catch (_: Exception) {
            serverOnline = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppSurface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title row with server indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "新建任务",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    // Server status indicator (red/green light)
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (serverOnline) {
                                    true -> CompletedGreen
                                    false -> HighPriority
                                    null -> TextTertiary // loading
                                }
                            )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Text input (3 lines)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("输入任务内容...", color = TextTertiary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = AppBorder,
                        focusedLabelColor = PrimaryBlue,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = PrimaryBlue,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Priority
                Text("优先级", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Priority.entries.forEach { priority ->
                        val isSelected = selectedPriority == priority.name.lowercase()
                        val pColor = when (priority) {
                            Priority.HIGH -> HighPriority
                            Priority.MEDIUM -> MediumPriority
                            Priority.LOW -> PrimaryBlue
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPriority = priority.name.lowercase() },
                            label = {
                                Text(
                                    priority.display,
                                    fontSize = 13.sp,
                                    color = if (isSelected) pColor else TextSecondary
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = AppSurfaceVariant,
                                selectedContainerColor = AppSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = AppBorder,
                                selectedBorderColor = pColor,
                                enabled = true,
                                selected = isSelected
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Due date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("截止日期", color = TextSecondary, fontSize = 13.sp)
                    Switch(
                        checked = hasDueDate,
                        onCheckedChange = { hasDueDate = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = PrimaryBlue,
                            checkedThumbColor = AppSurface
                        )
                    )
                }

                if (hasDueDate) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val dateFormat = remember { SimpleDateFormat("yyyy年M月d日", Locale.CHINESE) }
                    OutlinedCard(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(containerColor = AppSurfaceVariant),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(AppBorder)
                        )
                    ) {
                        Text(
                            text = if (dueDate > 0) dateFormat.format(Date(dueDate)) else "选择日期",
                            modifier = Modifier.padding(12.dp),
                            color = if (dueDate > 0) TextPrimary else TextTertiary,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category
                Text("分类", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = { selectedCategory = it },
                    placeholder = { Text("分类名称（可选）", color = TextTertiary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = AppBorder,
                        focusedLabelColor = PrimaryBlue,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = PrimaryBlue,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (text.isNotBlank()) {
                                onConfirm(
                                    text.trim(),
                                    "",
                                    selectedPriority,
                                    selectedCategory,
                                    if (hasDueDate && dueDate > 0) dueDate else null
                                )
                            }
                        },
                        enabled = text.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue,
                            contentColor = AppSurface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("创建")
                    }
                }
            }
        }
    }

    // Date picker
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
private fun DatePickerDialog(
    initialDate: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = if (initialDate > 0) initialDate else null
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                DatePicker(
                    state = state,
                    colors = DatePickerDefaults.colors(
                        containerColor = AppSurface,
                        titleContentColor = TextPrimary,
                        headlineContentColor = TextPrimary,
                        weekdayContentColor = TextSecondary,
                        subheadContentColor = TextSecondary,
                        yearContentColor = TextPrimary,
                        currentYearContentColor = PrimaryBlue,
                        selectedYearContentColor = AppSurface,
                        dayContentColor = TextPrimary,
                        selectedDayContentColor = AppSurface,
                        selectedDayContainerColor = PrimaryBlue,
                        dayInSelectionRangeContentColor = TextPrimary,
                        dayInSelectionRangeContainerColor = PrimaryBlueVariant,
                        todayContentColor = BlueAccent,
                        todayDateBorderColor = BlueAccent
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = TextSecondary)
                    }
                    TextButton(onClick = {
                        state.selectedDateMillis?.let { onConfirm(it) }
                    }) {
                        Text("确定", color = PrimaryBlue)
                    }
                }
            }
        }
    }
}
