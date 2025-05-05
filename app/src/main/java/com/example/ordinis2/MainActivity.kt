package com.example.ordinis2


import android.app.Application
import android.os.Bundle
import com.example.ordinis2.data.WorkPlan
import com.example.ordinis2.data.WorkSummary
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ordinis2.data.local.AppDatabase
import com.example.ordinis2.domain.OpenAiWorkPlanRepository
import com.example.ordinis2.ui.screens.LoginScreen
import com.example.ordinis2.ui.screens.RegisterScreen
import com.example.ordinis2.ui.screens.WelcomeScreen
import com.example.ordinis2.ui.screens.WorkPlanInputForm
import com.example.ordinis2.ui.screens.WorkPlanScreen
import com.example.ordinis2.viewmodel.LoginViewModel
import com.example.ordinis2.viewmodel.WorkPlanViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val activityScope = CoroutineScope(Dispatchers.Main)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val application = LocalContext.current.applicationContext as Application
            val userDao = AppDatabase.getDatabase(application).userDao()
            val viewModelFactory = LoginViewModel.LoginViewModelFactory(userDao)
            val loginViewModel: LoginViewModel = viewModel(factory = viewModelFactory)
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                OrdinisApp(workPlanViewModel = WorkPlanViewModel(OpenAiWorkPlanRepository(apiKey = "nothing to see here")),loginViewModel=loginViewModel, activityScope = activityScope)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }
}



// Main App Composable and UI handler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdinisApp(
    workPlanViewModel: WorkPlanViewModel, //TODO: look into hiltviewModel to initialize this directly with it
    activityScope: CoroutineScope,
    loginViewModel: LoginViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var currentScreen by rememberSaveable { mutableStateOf("welcome") } // Start with welcome screen
    var generatedWorkPlan by rememberSaveable { mutableStateOf<WorkPlan?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoggedIn by rememberSaveable { mutableStateOf(false) }
    var showRegisterScreen by rememberSaveable { mutableStateOf(false) }

    val workPlanState = workPlanViewModel.workPlanState.collectAsState()
    val errorState = workPlanViewModel.errorMessage.collectAsState()
    val isLoading by workPlanViewModel.isLoading.collectAsState()
    val loginResult by loginViewModel.loginResult.collectAsState()

    // Handle login result and navigate accordingly
    LaunchedEffect(loginResult) {
        when {
            loginResult == "Login successful" -> {
                isLoggedIn = true
                currentScreen = "welcome"
                loginViewModel.clearLoginResult()
            }

            loginResult?.startsWith("Registration successful") == true -> {
                showRegisterScreen = false
                currentScreen = "login"
                loginViewModel.clearLoginResult()
            }
        }
    }

    // Handle state updates when new work plan or error is available
    LaunchedEffect(workPlanState.value) {
        generatedWorkPlan = workPlanState.value
    }

    LaunchedEffect(errorState.value) {
        errorMessage = errorState.value
    }

    // Navigation Logic for Screens
    if (!isLoggedIn) {
        if (showRegisterScreen) {
            RegisterScreen(
                onRegisterSuccess = { username, password ->
                    loginViewModel.register(
                        username,
                        password
                    )
                },
                onLoginClick = { showRegisterScreen = false }
            )
        } else {
            LoginScreen(
                onLoginSuccess = { username, password -> loginViewModel.login(username, password) },
                onRegisterClick = { showRegisterScreen = true }
            )
        }
        return
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
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = "View WorkPlan"
                        )
                    },
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

                                    // Wait until either work plan or error message is available
                                    while (
                                        workPlanViewModel.workPlanState.value == null &&
                                        workPlanViewModel.errorMessage.value == null
                                    ) {
                                        delay(100)
                                    }

                                    currentScreen = "displayPlan"
                                }
                            },
                            isLoading = isLoading,
                            errorMessage = errorMessage,
                            snackbarHostState = snackbarHostState
                        )
                    }
                }

                "displayPlan" -> {
                    val plan = workPlanState.value
                    val error = errorState.value

                    when {
                        isLoading -> {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        }

                        error != null -> {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        plan != null -> {
                            WorkPlanScreen(workPlan = plan)
                        }

                        else -> {
                            Text("No work plan generated yet. Please fill out the form.")
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
        errorMessage?.let {}
    }
}

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

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    val apiKey = "nothing to see here"
    val workPlanViewModel = WorkPlanViewModel(OpenAiWorkPlanRepository(apiKey))
    val loginViewModel: LoginViewModel = viewModel()
    Surface {
        OrdinisApp(workPlanViewModel = workPlanViewModel,loginViewModel=loginViewModel,activityScope = rememberCoroutineScope())
    }
}
