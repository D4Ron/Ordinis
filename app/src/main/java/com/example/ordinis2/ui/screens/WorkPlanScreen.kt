package com.example.ordinis2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ordinis2.data.WorkPlan
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun WorkPlanScreen(workPlan: WorkPlan) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Work Plan", style = MaterialTheme.typography.headlineSmall)
        Text(text = "Plan Type: ${workPlan.workPlanType}", style = MaterialTheme.typography.bodyLarge)

        workPlan.tasks.forEachIndexed { index, taskList ->
            Text(text = "Period ${index + 1}", style = MaterialTheme.typography.bodySmall)
            taskList.forEach { task ->
                Text(text = "Task: ${task.taskDescription}, Deadline: ${task.taskDeadline?.let {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                } ?: "Not Set"}, Estimated Hours: ${task.taskEstimatedHours ?: "Not Set"}")
            }
        }
    }
}