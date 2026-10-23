package com.chronos.tasktracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.chronos.tasktracker.data.AppDatabase
import com.chronos.tasktracker.data.model.TaskEntity
import com.chronos.tasktracker.scheduler.AlarmScheduler
import com.chronos.tasktracker.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // Notification permission granted or denied
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val database = AppDatabase.getInstance(this)
        val alarmScheduler = AlarmScheduler(this)

        setContent {
            ChronosTheme {
                MainAppScreen(database, alarmScheduler)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(database: AppDatabase, alarmScheduler: AlarmScheduler) {
    val coroutineScope = rememberCoroutineScope()
    val tasks by database.taskDao().getAllTasksFlow().collectAsState(initial = emptyList())
    var selectedTab by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Chronos",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = PrimaryIndigo.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "Native Compose",
                                color = PrimaryIndigo,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = DarkSurface) {
                val items = listOf("Home" to Icons.Default.Home, "Tasks" to Icons.Default.Checklist, "Reports" to Icons.Default.BarChart)
                items.forEachIndexed { index, pair ->
                    NavigationBarItem(
                        icon = { Icon(pair.second, contentDescription = pair.first) },
                        label = { Text(pair.first) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryIndigo,
                            indicatorColor = PrimaryIndigo.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryIndigo,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // Home Dashboard
                    val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    val todayTasks = tasks.filter { it.dueDate == todayStr }
                    val completedCount = todayTasks.count { it.isCompleted }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Today's Progress", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "$completedCount / ${todayTasks.size} Tasks Completed",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                            LinearProgressIndicator(
                                progress = { if (todayTasks.isNotEmpty()) completedCount.toFloat() / todayTasks.size else 1f },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                color = PrimaryIndigo,
                                trackColor = DarkElevated
                            )
                        }
                    }

                    Text("Active Tasks", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tasks, key = { it.id }) { task ->
                            TaskRow(
                                task = task,
                                onToggleComplete = {
                                    coroutineScope.launch {
                                        val updated = task.copy(isCompleted = !task.isCompleted)
                                        database.taskDao().updateTask(updated)
                                        if (updated.isCompleted) {
                                            alarmScheduler.cancelTaskReminder(task.id)
                                        } else {
                                            alarmScheduler.scheduleTaskReminder(updated)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                1 -> {
                    // All Tasks list
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tasks, key = { it.id }) { task ->
                            TaskRow(
                                task = task,
                                onToggleComplete = {
                                    coroutineScope.launch {
                                        val updated = task.copy(isCompleted = !task.isCompleted)
                                        database.taskDao().updateTask(updated)
                                    }
                                }
                            )
                        }
                    }
                }
                2 -> {
                    // Reports overview
                    val completedTotal = tasks.count { it.isCompleted }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Productivity Velocity", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Total Accomplished: $completedTotal", color = PriorityLow)
                            Text("Pending Queue: ${tasks.size - completedTotal}", color = PriorityMedium)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        QuickAddDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, priority, dueTime ->
                coroutineScope.launch {
                    val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    val newTask = TaskEntity(
                        id = "task_${System.currentTimeMillis()}",
                        title = title,
                        priority = priority,
                        dueDate = todayStr,
                        dueTime = dueTime
                    )
                    database.taskDao().insertTask(newTask)
                    alarmScheduler.scheduleTaskReminder(newTask)
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
fun TaskRow(task: TaskEntity, onToggleComplete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(
                    checkedColor = PriorityLow,
                    uncheckedColor = TextSecondary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    color = if (task.isCompleted) TextSecondary else TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Due ${task.dueDate} at ${task.dueTime} · ${task.priority.uppercase()}",
                    fontSize = 11.sp,
                    color = when (task.priority) {
                        "high" -> PriorityHigh
                        "medium" -> PriorityMedium
                        else -> PriorityLow
                    }
                )
            }
        }
    }
}

@Composable
fun QuickAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, priority: String, dueTime: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("medium") }
    var dueTime by remember { mutableStateOf("17:00") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = dueTime,
                    onValueChange = { dueTime = it },
                    label = { Text("Due Time (HH:mm)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onConfirm(title, priority, dueTime) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
