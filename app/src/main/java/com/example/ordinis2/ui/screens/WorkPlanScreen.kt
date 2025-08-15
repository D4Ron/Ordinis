package com.example.ordinis2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ordinis2.data.Task
import com.example.ordinis2.data.WorkPlan
import com.example.ordinis2.data.WorkSummary
import com.example.ordinis2.viewmodel.WorkPlanViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt
import kotlin.text.isNotBlank

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkPlanScreen(
    workPlan: WorkPlan,
    workPlanViewModel: WorkPlanViewModel, // Added ViewModel parameter
    workSummaryForSaving: WorkSummary?,    // Added WorkSummary for context during save
    modifier: Modifier = Modifier
) {
    var currentPeriodIndex by remember { mutableIntStateOf(0) }
    val periodsCount = workPlan.tasks.size
    val isLoadingSave by workPlanViewModel.isLoading.collectAsState() // For save button enabled state
    val isPlanAlreadySavedOrLoaded by workPlanViewModel.isPlanLoadedFromDb.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Save Button
        if(!isPlanAlreadySavedOrLoaded){
            Button(
                onClick = {
                    workSummaryForSaving?.let { summary ->
                        workPlanViewModel.saveCurrentWorkPlan(workPlan, summary)
                    } ?: run {
                        workPlanViewModel.saveCurrentWorkPlan(workPlan, null)
                    }
                } ,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                enabled = !isLoadingSave
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Save Work Plan")
                Spacer(Modifier.width(8.dp))
                Text("Save Work Plan")
            }
        } else {
            Text(
                "This work plan is saved.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }


        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                CompactWorkPlanHeader(workPlan = workPlan)
            }

            if (periodsCount > 0 && currentPeriodIndex < periodsCount && workPlan.tasks[currentPeriodIndex].isNotEmpty()) {
                item {
                    CompactProgressCard(
                        periodIndex = currentPeriodIndex,
                        tasks = workPlan.tasks[currentPeriodIndex],
                        totalPeriods = periodsCount
                    )
                }
            }

            if (periodsCount > 0 && currentPeriodIndex < periodsCount) {
                val tasksForPeriod = workPlan.tasks[currentPeriodIndex]
                if (tasksForPeriod.isNotEmpty()) {
                    items(tasksForPeriod.size) { index ->
                        val task = tasksForPeriod[index]
                        CompactTaskCard(
                            task = task,
                            taskIndex = index + 1, // 1-based index for display
                            totalTasks = tasksForPeriod.size
                        )
                    }
                } else {
                    item {
                        EmptyPeriodCard()
                    }
                }
            } else if (periodsCount == 0 && workPlan.tasks.isEmpty()) { // Ensure it's truly empty
                item {
                    EmptyWorkPlanCard()
                }
            }
        }

        if (periodsCount > 1) {
            BottomPeriodNavigation(
                currentPeriod = currentPeriodIndex,
                totalPeriods = periodsCount,
                onPreviousPeriod = {
                    if (currentPeriodIndex > 0) {
                        currentPeriodIndex--
                    }
                },
                onNextPeriod = {
                    if (currentPeriodIndex < periodsCount - 1) {
                        currentPeriodIndex++
                    }
                }
            )
        }
    }
}

@Composable
fun CompactWorkPlanHeader(workPlan: WorkPlan) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = "Work Plan Icon",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Work Plan Overview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = workPlan.workPlanType,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun CompactProgressCard(
    periodIndex: Int,
    tasks: List<Task>,
    totalPeriods: Int
) {
    val completedTasks = tasks.count { it.taskIsCompleted }
    val totalTasks = tasks.size
    val progress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks.toFloat() else 0f
    val totalHours = tasks.sumOf { it.taskEstimatedHours?.toDouble() ?: 0.0 }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Period ${periodIndex + 1} of $totalPeriods",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            CircleShape
                        )
                ) {
                    Text(
                        text = "${(progress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$completedTasks/$totalTasks tasks completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (totalHours > 0) {
                    Text(
                        // Format to one decimal place if needed, or round
                        text = "${String.format(Locale.US, "%.1f", totalHours)}h estimated total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CompactTaskCard(
    task: Task,
    taskIndex: Int,
    totalTasks: Int
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (task.taskIsCompleted) 1.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (task.taskIsCompleted)
                MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp) // Subtle change for completed
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top // Align to top for multi-line description
            ) {
                Icon(
                    imageVector = if (task.taskIsCompleted) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = if (task.taskIsCompleted) "Task Completed" else "Task Not Completed",
                    tint = if (task.taskIsCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(top = 2.dp) // Slight padding for alignment
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Task $taskIndex",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (task.taskIsCompleted)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            else
                                MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse Task" else "Expand Task",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.taskDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (task.taskIsCompleted)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!isExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            task.taskEstimatedHours?.let { hours ->
                                CompactChip(text = "${hours}h")
                            }
                            task.taskDeadline?.let { deadline ->
                                val dateFormatter = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
                                CompactChip(text = "Due: ${dateFormatter.format(deadline)}")
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeOut()
            ) {
                CompactTaskDetails(task = task, taskIndex = taskIndex, totalTasks = totalTasks)
            }
        }
    }
}

@Composable
fun CompactTaskDetails(
    task: Task,
    taskIndex: Int,
    totalTasks: Int
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()) } // More detailed date
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$taskIndex of $totalTasks",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                task.taskEstimatedHours?.let { hours ->
                    CompactDetailCard(title = "Est. Time", content = "$hours hours", modifier = Modifier.weight(1f))
                }
                task.taskDeadline?.let { deadline ->
                    CompactDetailCard(title = "Due Date", content = dateFormatter.format(deadline), modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (task.taskDescription.isNotBlank()) {
                CompactDetailCard(title = "Notes", content = task.taskDescription, modifier = Modifier.fillMaxWidth())
            }
            Spacer(modifier = Modifier.height(8.dp))
            CompactStatusBanner(isCompleted = task.taskIsCompleted)
        }
    }
}

@Composable
fun CompactChip(text: String) {
    Surface(
        shape = CircleShape, // Fully rounded
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CompactDetailCard(title: String, content: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = title.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CompactStatusBanner(isCompleted: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (isCompleted) Color(0xFF4CAF50).copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
        contentColor = if (isCompleted) Color(0xFF006400) else MaterialTheme.colorScheme.onErrorContainer // Dark Green for completed, error color for not
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Filled.AccountBox else Icons.Filled.Info,
                contentDescription = "Status Icon",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isCompleted) "Task is Marked Complete" else "Task Pending",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun BottomPeriodNavigation(
    currentPeriod: Int,
    totalPeriods: Int,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 4.dp, // Add shadow for better separation
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp) // Elevate background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousPeriod, enabled = currentPeriod > 0) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Period")
            }
            Text(
                text = "Period ${currentPeriod + 1} / $totalPeriods",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            IconButton(onClick = onNextPeriod, enabled = currentPeriod < totalPeriods - 1) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Period")
            }
        }
    }
}

@Composable
fun EmptyPeriodCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp), // More padding for visual emphasis
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent) // No background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MailOutline,
                contentDescription = "No tasks",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No tasks for this period.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = "You can navigate to other periods if available.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EmptyWorkPlanCard() {
    Card(
        modifier = Modifier
            .fillMaxSize() // Take up more space if it's the only thing on screen
            .padding(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.List,
                contentDescription = "No work plan",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Work Plan is Empty",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "There are no tasks or periods defined in this work plan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

