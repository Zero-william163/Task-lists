package com.taskflow.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.taskflow.data.model.Task
import com.taskflow.ui.viewmodel.TaskViewModel
import com.taskflow.worker.ReminderWorker
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    viewModel: TaskViewModel,
    taskId: Long? = null,
    onNavigateBack: () -> Unit
) {
    val selectedTask by viewModel.selectedTask.collectAsState()
    val context = LocalContext.current
    val isEditing = taskId != null

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableIntStateOf(Task.PRIORITY_MEDIUM) }
    var category by remember { mutableStateOf(Task.CATEGORY_NONE) }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var reminderTime by remember { mutableStateOf<Long?>(null) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTask) {
        selectedTask?.let { task ->
            title = task.title
            description = task.description
            priority = task.priority
            category = task.category
            dueDate = task.dueDate
            reminderTime = task.reminderTime
        }
    }

    if (isEditing && selectedTask == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "编辑任务" else "新建任务") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("任务标题 *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("任务描述") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
            )

            Text("优先级", style = MaterialTheme.typography.titleSmall)
            Row(modifier = Modifier.selectableGroup()) {
                PriorityOption("低", Task.PRIORITY_LOW, priority) { priority = it }
                PriorityOption("中", Task.PRIORITY_MEDIUM, priority) { priority = it }
                PriorityOption("高", Task.PRIORITY_HIGH, priority) { priority = it }
            }

            ExposedDropdownMenuBox(
                expanded = showCategoryDropdown,
                onExpandedChange = { showCategoryDropdown = it }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("分类") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) }
                )
                ExposedDropdownMenu(
                    expanded = showCategoryDropdown,
                    onDismissRequest = { showCategoryDropdown = false }
                ) {
                    Task.CATEGORIES.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                category = cat
                                showCategoryDropdown = false
                            }
                        )
                    }
                }
            }

            DateTimeSelector(
                label = "截止时间",
                timestamp = dueDate,
                onSelect = { dueDate = it }
            )

            DateTimeSelector(
                label = "提醒时间",
                timestamp = reminderTime,
                onSelect = { reminderTime = it }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    val task = Task(
                        id = selectedTask?.id ?: 0,
                        title = title,
                        description = description,
                        priority = priority,
                        category = category,
                        dueDate = dueDate,
                        reminderTime = reminderTime,
                        isCompleted = selectedTask?.isCompleted ?: false,
                        completedAt = selectedTask?.completedAt
                    )
                    if (isEditing) {
                        viewModel.updateTask(task)
                        reminderTime?.let {
                            if (!task.isCompleted && it > System.currentTimeMillis()) {
                                ReminderWorker.scheduleReminder(context, task)
                            }
                        }
                        onNavigateBack()
                    } else {
                        viewModel.addTask(task) { newId ->
                            val newTask = task.copy(id = newId)
                            reminderTime?.let {
                                if (it > System.currentTimeMillis()) {
                                    ReminderWorker.scheduleReminder(context, newTask)
                                }
                            }
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = title.isNotBlank()
            ) {
                Text(if (isEditing) "保存修改" else "创建任务")
            }
        }
    }
}

@Composable
fun PriorityOption(
    label: String,
    value: Int,
    selectedPriority: Int,
    onSelect: (Int) -> Unit
) {
    val isSelected = value == selectedPriority
    val color = when (value) {
        Task.PRIORITY_HIGH -> MaterialTheme.colorScheme.error
        Task.PRIORITY_MEDIUM -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .selectable(
                selected = isSelected,
                onClick = { onSelect(value) },
                role = Role.RadioButton
            )
            .padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
fun DateTimeSelector(
    label: String,
    timestamp: Long?,
    onSelect: (Long?) -> Unit
) {
    val context = LocalContext.current
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    var showClear by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(
                text = timestamp?.let { formatter.format(Date(it)) } ?: "未设置",
                style = MaterialTheme.typography.bodyMedium,
                color = if (timestamp != null) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (timestamp != null) {
                TextButton(onClick = { onSelect(null) }) {
                    Text("清除")
                }
            }
            Button(
                onClick = {
                    val calendar = Calendar.getInstance()
                    timestamp?.let { calendar.timeInMillis = it }

                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            calendar.set(year, month, day)
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                                    calendar.set(Calendar.MINUTE, minute)
                                    onSelect(calendar.timeInMillis)
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
            ) {
                Text("选择")
            }
        }
    }
}
