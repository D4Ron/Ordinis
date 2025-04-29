package com.example.ordinis2


import android.os.Bundle
import com.example.ordinis2.data.Task
import com.example.ordinis2.data.WorkPlan
import com.example.ordinis2.data.WorkSummary
import android.provider.Settings.Global.getString
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import okhttp3.*
import java.util.*
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.util.*



class MainActivity : ComponentActivity() {

    private val activityScope = CoroutineScope(Dispatchers.Main)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                OrdinisApp(activityScope = activityScope)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel() // Cancel the CoroutineScope when the Activity is destroyed
    }
}



// Main App Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdinisApp(
    workPlanViewModel: WorkPlanViewModel = WorkPlanViewModel(apiKey = stringResource(R.string.openai_api_key)),
    activityScope: CoroutineScope
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var currentScreen by rememberSaveable { mutableStateOf("welcome") }
    var generatedWorkPlan by rememberSaveable { mutableStateOf<WorkPlan?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val workPlanState = workPlanViewModel.workPlanState.collectAsState()
    val errorState = workPlanViewModel.errorMessage.collectAsState()
    val isLoading by workPlanViewModel.isLoading.collectAsState()

    // Update generatedWorkPlan when new work plan is available
    LaunchedEffect(workPlanState.value) {
        generatedWorkPlan = workPlanState.value
    }

    // Update errorMessage when a new error appears
    LaunchedEffect(errorState.value) {
        errorMessage = errorState.value
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when (currentScreen) {
                        "welcome" -> Text("Ordinis - Work Plan Generator")
                        "inputForm" -> Text("Generate Work Plan")
                        "displayPlan" -> Text("Work Plan")
                        else -> Text("Ordinis")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == "welcome",
                    onClick = { currentScreen = "welcome" },
                    icon = { Icon(Icons.Filled.Add, contentDescription = "Add Project") },
                    label = { Text("Add Project") }
                )
                NavigationBarItem(
                    selected = currentScreen == "displayPlan",
                    onClick = { currentScreen = "displayPlan" },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "View WorkPlan") },
                    label = { Text("View Plan") }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (currentScreen) {
                "welcome" -> {
                    WelcomeScreen(onAddProjectButtonClicked = { currentScreen = "inputForm" })
                }
                "inputForm" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        var projectTitle by rememberSaveable { mutableStateOf("") }
                        var projectDescription by rememberSaveable { mutableStateOf("") }
                        var desiredPlanType by rememberSaveable { mutableStateOf("Weekly") }
                        var userRole by rememberSaveable { mutableStateOf("Project Manager") }
                        var projectStartDate by rememberSaveable { mutableStateOf(Date()) }
                        var deadline by rememberSaveable { mutableStateOf<Date?>(null) }

                        WorkPlanInputForm(
                            projectTitle = projectTitle,
                            onProjectTitleChange = { projectTitle = it },
                            projectDescription = projectDescription,
                            onProjectDescriptionChange = { projectDescription = it },
                            desiredPlanType = desiredPlanType,
                            onPlanTypeChange = { desiredPlanType = it },
                            userRole = userRole,
                            onUserRoleChange = { userRole = it },
                            projectStartDate = projectStartDate,
                            onProjectStartDateChange = { projectStartDate = it },
                            deadline = deadline,
                            onDeadlineChange = { deadline = it },
                            onGenerateWorkPlan = {
                                activityScope.launch {
                                    val workSummary = WorkSummary(
                                        projectTitle = projectTitle,
                                        projectDescription = projectDescription,
                                        desiredPlanType = desiredPlanType,
                                        userRole = userRole,
                                        deadline = deadline
                                    )
                                    workPlanViewModel.generateWorkPlan(workSummary)
                                    currentScreen = "displayPlan"
                                }
                            },
                            generatedWorkPlan = generatedWorkPlan,
                            isLoading = isLoading,
                            errorMessage = errorMessage,
                            snackbarHostState = snackbarHostState
                        )
                    }
                }
                "displayPlan" -> {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp)) // Show a loading spinner
                    } else {
                        generatedWorkPlan?.let { plan ->
                            WorkPlanScreen(workPlan = plan)
                        } ?: run {
                            Text("No work plan generated yet.")
                        }
                    }
                }
                else -> {
                    Text("Unknown screen")
                }
            }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
            errorMessage = null
        }
    }
}






// Welcome screen
@Composable
fun WelcomeScreen(onAddProjectButtonClicked: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to Ordinis",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Click the button below to add a new project to monitor.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        Button(onClick = onAddProjectButtonClicked) {
            Text("Add a New Project to Monitor")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    generatedWorkPlan: WorkPlan?,
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



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanTypeDropdown(
    selectedPlanType: String,
    onPlanTypeChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val planTypes = listOf("Weekly", "Monthly")

    Box {
        OutlinedTextField(
            value = selectedPlanType,
            onValueChange = {},
            label = { Text("Plan Type") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown")
                }
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            planTypes.forEach { planType ->
                DropdownMenuItem(
                    text = { Text(text = planType) },
                    onClick = {
                        onPlanTypeChange(planType)
                        expanded = false
                    }
                )
            }
        }
    }
}

// Displays a date selector for the form
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelector(
    label: String,
    selectedDate: Date?,
    onDateChange: (Date) -> Unit,
    modifier: Modifier = Modifier
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate?.time,
        selectableDates = object : SelectableDates {
            fun isDateSelectable(dateMillis: Long): Boolean {
                return true
            }
        }
    )
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())


    Column(modifier = modifier) {
        Button(onClick = { showDatePickerDialog = true }) {
            Text(text = "$label: ${selectedDate?.let { dateFormatter.format(it) } ?: "Not Set"}")
        }

        if (showDatePickerDialog) {
            DatePickerDialog(
                onDismissRequest = { showDatePickerDialog = false },
                confirmButton = {
                    Button(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            onDateChange(Date(it))
                        }
                        showDatePickerDialog = false
                    }) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDatePickerDialog = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

// ViewModel to manage UI state and interact with the OpenAI API
class WorkPlanViewModel(private val apiKey: String) : ViewModel() {

    private val _workPlanState = MutableStateFlow<WorkPlan?>(null)
    val workPlanState: StateFlow<WorkPlan?> = _workPlanState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val client = OkHttpClient()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    suspend fun generateWorkPlan(workSummary: WorkSummary) {
        _isLoading.value = true
        _errorMessage.value = null
        _workPlanState.value = null

        val prompt = buildPrompt(workSummary)

        val requestBody = JSONObject().apply {
            put("model", "gpt-3.5-turbo")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), requestBody.toString()))
            .build()

        try {
            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

            _isLoading.value = false
            if (response.isSuccessful) {
                response.body?.string()?.let { responseBody ->
                    val json = JSONObject(responseBody)
                    val content = json
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                    val extractedPlan = extractWorkPlan(content, workSummary.desiredPlanType)
                    _workPlanState.value = extractedPlan
                } ?: run {
                    _errorMessage.value = "Empty response from OpenAI"
                }
            } else {
                _errorMessage.value = "OpenAI Error: ${response.code}"
            }
        } catch (e: Exception) {
            _isLoading.value = false
            _errorMessage.value = e.message ?: "Network error"
        }
    }


    private fun buildPrompt(summary: WorkSummary): String {
        return """
            You are an expert project planner. Based on the following project details, generate a detailed step-by-step work plan. Each task should include a description and estimated duration in days.

            Project Title: ${summary.projectTitle}
            Description: ${summary.projectDescription}
            Role: ${summary.userRole}
            Start Date: ${dateFormat.format(summary.projectStartDate)}
            Deadline: ${summary.deadline?.let { dateFormat.format(it) } ?: "Not provided"}
            Plan Type: ${summary.desiredPlanType}

            Format:
            Task 1: [Task Description] - [Duration in days]
            Task 2: ...
        """.trimIndent()
    }

    private fun extractWorkPlan(workPlanString: String, planType: String): WorkPlan {
        val tasks = mutableListOf<List<Task>>()
        val lines = workPlanString.lines()

        var i = 0
        while (i < lines.size) {
            if (lines[i].startsWith("Task")) {
                val taskList = mutableListOf<Task>()
                while (i < lines.size && lines[i].startsWith("Task")) {
                    val parts = lines[i].split(":")
                    if (parts.size > 1) {
                        val descriptionDuration = parts[1].split("-")
                        val description = descriptionDuration.getOrNull(0)?.trim() ?: ""
                        val daysText = descriptionDuration.getOrNull(1)?.replace("days", "")?.replace("day", "")?.trim()
                        val days = daysText?.toIntOrNull() ?: 1
                        val deadline = Date(Date().time + days * 86400000L)
                        val estimatedHours = days * 8
                        taskList.add(Task(description, deadline, estimatedHours))
                    }
                    i++
                }
                tasks.add(taskList)
            } else {
                i++
            }
        }
        return WorkPlan(workPlanType = planType, tasks = tasks)
    }
}


// Displays the work plan
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

// Preview Composable
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    val apiKey = stringResource(R.string.openai_api_key)
    val workPlanViewModel = WorkPlanViewModel(apiKey)
    Surface {
        OrdinisApp(workPlanViewModel = workPlanViewModel, activityScope = rememberCoroutineScope())
    }
}
