package com.example.ordinis2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.ordinis2.DateSelector
import com.example.ordinis2.PlanTypeDropdown
import java.util.Date

@Composable
fun WorkPlanInputForm(
    projectTitle: String,
    onProjectTitleChange: (String) -> Unit,
    projectDescription: String,
    onProjectDescriptionChange: (String) -> Unit,
    desiredPlanType: String,
    onPlanTypeChange: (String) -> Unit,
    userRole: String,
    onUserRoleChange: (String) -> Unit,
    projectStartDate: Date,
    onProjectStartDateChange: (Date) -> Unit,
    deadline: Date?,
    onDeadlineChange: (Date?) -> Unit,
    onGenerateWorkPlan: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    snackbarHostState: SnackbarHostState
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        OutlinedTextField(
            value = projectTitle,
            onValueChange = onProjectTitleChange,
            label = { Text("Project Title") },
            placeholder = { Text("What are you working on?") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
        )

        PlanTypeDropdown(
            selectedPlanType = desiredPlanType,
            onPlanTypeChange = onPlanTypeChange
        )

        OutlinedTextField(
            value = userRole,
            onValueChange = onUserRoleChange,
            label = { Text("Your Role") },
            placeholder = { Text("Teacher, Student, etc...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
        )

        OutlinedTextField(
            value = projectDescription,
            onValueChange = onProjectDescriptionChange,
            label = { Text("Project Description") },
            placeholder = { Text("Describe your project...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            singleLine = false,
            maxLines = 10
        )

        Text(text = "Start Date & Deadline", style = MaterialTheme.typography.titleSmall)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DateSelector(
                label = "Start Date",
                selectedDate = projectStartDate,
                onDateChange = onProjectStartDateChange,
                modifier = Modifier.weight(1f)
            )
            DateSelector(
                label = "Deadline",
                selectedDate = deadline ?: Date(),
                onDateChange = onDeadlineChange,
                modifier = Modifier.weight(1f)
            )
        }

        Button(
            onClick = onGenerateWorkPlan,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(20.dp)
                        .padding(4.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Generate Work Plan")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    errorMessage?.let {
        LaunchedEffect(it) {
            snackbarHostState.showSnackbar(it)
        }
    }
}