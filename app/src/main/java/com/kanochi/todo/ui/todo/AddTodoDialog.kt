package com.kanochi.todo.ui.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kanochi.todo.data.model.Priority
import com.kanochi.todo.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoDialog(
    initialDate: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, priority: String, category: String, dueDate: Long?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf("medium") }
    var selectedCategory by remember { mutableStateOf("") }
    var hasDueDate by remember { mutableStateOf(initialDate != null) }
    var dueDate by remember { mutableStateOf(initialDate ?: 0L) }
    var showDatePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "新建任务",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("任务标题") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedLabelColor = GreenAccent,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = GreenAccent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述（可选）") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedLabelColor = GreenAccent,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = GreenAccent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Priority
                Text("优先级", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Priority.entries.forEach { priority ->
                        val isSelected = selectedPriority == priority.name.lowercase()
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPriority = priority.name.lowercase() },
                            label = {
                                Text(
                                    priority.display,
                                    fontSize = 13.sp,
                                    color = if (isSelected) {
                                        when (priority) {
                                            Priority.HIGH -> HighPriority
                                            Priority.MEDIUM -> MediumPriority
                                            Priority.LOW -> LowPriority
                                        }
                                    } else TextSecondary
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DarkSurfaceVariant,
                                selectedContainerColor = DarkSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = DarkBorder,
                                selectedBorderColor = when (priority) {
                                    Priority.HIGH -> HighPriority
                                    Priority.MEDIUM -> MediumPriority
                                    Priority.LOW -> LowPriority
                                },
                                enabled = true,
                                selected = isSelected
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Due date toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("设置截止日期", color = TextSecondary, fontSize = 13.sp)
                    Switch(
                        checked = hasDueDate,
                        onCheckedChange = { hasDueDate = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = GreenPrimary,
                            checkedThumbColor = GreenOnPrimary
                        )
                    )
                }

                if (hasDueDate) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val dateFormat = remember { SimpleDateFormat("yyyy年M月d日", Locale.CHINESE) }
                    OutlinedCard(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(containerColor = DarkSurfaceVariant),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
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
                            if (title.isNotBlank()) {
                                onConfirm(
                                    title.trim(),
                                    description.trim(),
                                    selectedPriority,
                                    selectedCategory,
                                    if (hasDueDate && dueDate > 0) dueDate else null
                                )
                            }
                        },
                        enabled = title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary,
                            contentColor = GreenOnPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("创建")
                    }
                }
            }
        }
    }

    // Simple date picker
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
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                DatePicker(state = state, colors = DatePickerDefaults.colors(
                    containerColor = DarkSurface,
                    titleContentColor = TextPrimary,
                    headlineContentColor = TextPrimary,
                    weekdayContentColor = TextSecondary,
                    subheadContentColor = TextSecondary,
                    yearContentColor = TextPrimary,
                    currentYearContentColor = GreenAccent,
                    selectedYearContentColor = DarkBackground,
                    dayContentColor = TextPrimary,
                    selectedDayContentColor = DarkBackground,
                    selectedDayContainerColor = GreenPrimary,
                    dayInSelectionRangeContentColor = TextPrimary,
                    dayInSelectionRangeContainerColor = CalendarSelectedBg,
                    todayContentColor = CalendarToday,
                    todayDateBorderColor = CalendarToday
                ))

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
                        Text("确定", color = GreenAccent)
                    }
                }
            }
        }
    }
}
