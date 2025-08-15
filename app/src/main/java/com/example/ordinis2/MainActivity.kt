package com.example.ordinis2

import android.app.Application
import android.os.Bundle
import com.example.ordinis2.data.WorkPlan
import com.example.ordinis2.data.WorkSummary
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ordinis2.data.local.AppDatabase
import com.example.ordinis2.ui.screens.LoginScreen
import com.example.ordinis2.ui.screens.RegisterScreen
import com.example.ordinis2.ui.screens.SavedWorkPlansScreen
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
                OrdinisApp(
                    loginViewModel = loginViewModel,
                    activityScope = activityScope
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdinisApp(
    activityScope: CoroutineScope,
    loginViewModel: LoginViewModel
) {
    val workPlanViewModel: WorkPlanViewModel = viewModel()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentScreen by rememberSaveable { mutableStateOf("welcome") }
    var generatedWorkPlan by rememberSaveable { mutableStateOf<WorkPlan?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoggedIn by rememberSaveable { mutableStateOf(false) }
    var showRegisterScreen by rememberSaveable { mutableStateOf(false) }
    var currentLoggedInUsername by rememberSaveable { mutableStateOf<String?>(null) }
    var currentWorkSummaryForSaving by remember { mutableStateOf<WorkSummary?>(null) }

    val workPlanState = workPlanViewModel.workPlanState.collectAsState()
    val errorState = workPlanViewModel.errorMessage.collectAsState()
    val isLoading by workPlanViewModel.isLoading.collectAsState()
    val loginResult by loginViewModel.loginResult.collectAsState()
    val saveMessage by workPlanViewModel.saveMessage.collectAsState()

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

    LaunchedEffect(saveMessage) {
        saveMessage?.let {
            snackbarHostState.showSnackbar(it)
            workPlanViewModel.clearSaveMessage()
        }
    }

    LaunchedEffect(errorState.value) {
        errorState.value?.let { msg ->
            if (currentScreen == "displayPlan" || currentScreen == "inputForm" || currentScreen == "savedPlans") {
                snackbarHostState.showSnackbar("Error: $msg")
            }
        }
    }

    if (!isLoggedIn) {
        if (showRegisterScreen) {
            RegisterScreen(
                onRegisterSuccess = { username, password ->
                    loginViewModel.register(username, password)
                },
                onLoginClick = { showRegisterScreen = false }
            )
        } else {
            LoginScreen(
                onLoginSuccess = { username, password ->
                    loginViewModel.login(username, password)
                    currentLoggedInUsername = username
                },
                onRegisterClick = { showRegisterScreen = true }
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ModernAppHeader(
            currentScreen = currentScreen,
            username = currentLoggedInUsername
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (currentScreen) {
                "welcome" -> {
                    WelcomeScreen(
                        username = currentLoggedInUsername,
                        onAddProjectButtonClicked = { currentScreen = "inputForm" }
                    )
                }

                "inputForm" -> {
                    ModernInputFormScreen(
                        workPlanViewModel = workPlanViewModel,
                        activityScope = activityScope,
                        isLoading = isLoading,
                        errorMessage = errorState.value,
                        snackbarHostState = snackbarHostState,
                        onWorkSummaryCreate = { workSummary ->
                            currentWorkSummaryForSaving = workSummary
                        }
                    )

                    LaunchedEffect(workPlanState.value, errorState.value, isLoading) {
                        if (!isLoading) {
                            if (workPlanState.value != null && currentScreen == "inputForm") {
                                currentScreen = "displayPlan"
                            }
                        }
                    }
                }

                "displayPlan" -> {
                    ModernDisplayPlanScreen(
                        workPlanState = workPlanState.value,
                        errorState = errorState.value,
                        isLoading = isLoading,
                        workPlanViewModel = workPlanViewModel,
                        currentWorkSummaryForSaving = currentWorkSummaryForSaving,
                        onNavigateToInputForm = { currentScreen = "inputForm" },
                        onNavigateToSavedPlans = { currentScreen = "savedPlans" }
                    )
                }

                "savedPlans" -> {
                    SavedWorkPlansScreen(
                        workPlanViewModel = workPlanViewModel,
                        onNavigateToDisplayPlan = { workPlanEntity ->
                            currentWorkSummaryForSaving = null
                            workPlanViewModel.loadWorkPlanFromEntity(workPlanEntity)
                            currentScreen = "displayPlan"
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    EmptyStateScreen(message = "Unknown screen: $currentScreen")
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        ModernBottomNavigation(
            currentScreen = currentScreen,
            workPlanState = workPlanState.value,
            onScreenChange = { screen ->
                when (screen) {
                    "displayPlan" -> {
                        if (workPlanViewModel.workPlanState.value != null || currentScreen == "savedPlans") {
                            currentScreen = "displayPlan"
                        } else {
                            currentScreen = "savedPlans"
                        }
                    }
                    else -> currentScreen = screen
                }
            }
        )
    }
}

@Composable
fun ModernAppHeader(
    currentScreen: String,
    username: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "O",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Ordinis",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = when (currentScreen) {
                            "welcome" -> "Plan Generator"
                            "inputForm" -> "Generate Work Plan"
                            "displayPlan" -> "Work Plan Details"
                            "savedPlans" -> "Saved Work Plans"
                            else -> "Workspace"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (username != null) {
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = username,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun ModernBottomNavigation(
    currentScreen: String,
    workPlanState: WorkPlan?,
    onScreenChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            NavigationBarItem(
                selected = currentScreen == "welcome" || currentScreen == "inputForm",
                onClick = { onScreenChange("welcome") },
                icon = {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Generate",
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        "Generate",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
            NavigationBarItem(
                selected = currentScreen == "displayPlan",
                onClick = { onScreenChange("displayPlan") },
                icon = {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = "Current Plan",
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        "Current Plan",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
            NavigationBarItem(
                selected = currentScreen == "savedPlans",
                onClick = { onScreenChange("savedPlans") },
                icon = {
                    Icon(
                        Icons.Filled.List,
                        contentDescription = "Saved",
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        "Saved",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
        }
    }
}

@Composable
fun ModernInputFormScreen(
    workPlanViewModel: WorkPlanViewModel,
    activityScope: CoroutineScope,
    isLoading: Boolean,
    errorMessage: String?,
    snackbarHostState: SnackbarHostState,
    onWorkSummaryCreate: (WorkSummary) -> Unit
) {
    var projectTitle by rememberSaveable { mutableStateOf("") }
    var projectDescription by rememberSaveable { mutableStateOf("") }
    var desiredPlanType by rememberSaveable { mutableStateOf("Weekly") }
    var userRole by rememberSaveable { mutableStateOf("") }
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
            val workSummary = WorkSummary(
                projectTitle = projectTitle,
                projectDescription = projectDescription,
                desiredPlanType = desiredPlanType,
                userRole = userRole,
                projectStartDate = projectStartDate,
                deadline = deadline
            )
            onWorkSummaryCreate(workSummary)
            activityScope.launch {
                workPlanViewModel.generateWorkPlan(workSummary)
            }
        },
        isLoading = isLoading,
        errorMessage = errorMessage,
        snackbarHostState = snackbarHostState
    )
}

@Composable
fun ModernDisplayPlanScreen(
    workPlanState: WorkPlan?,
    errorState: String?,
    isLoading: Boolean,
    workPlanViewModel: WorkPlanViewModel,
    currentWorkSummaryForSaving: WorkSummary?,
    onNavigateToInputForm: () -> Unit,
    onNavigateToSavedPlans: () -> Unit
) {
    when {
        isLoading && workPlanState == null -> {
            LoadingStateScreen()
        }
        errorState != null && workPlanState == null -> {
            ErrorStateScreen(
                error = errorState,
                onTryAgain = {
                    workPlanViewModel.clearErrorMessage()
                    onNavigateToInputForm()
                },
                onViewSavedPlans = {
                    workPlanViewModel.clearErrorMessage()
                    onNavigateToSavedPlans()
                }
            )
        }
        workPlanState != null -> {
            WorkPlanScreen(
                workPlan = workPlanState,
                workPlanViewModel = workPlanViewModel,
                workSummaryForSaving = currentWorkSummaryForSaving,
                modifier = Modifier.fillMaxSize()
            )
        }
        else -> {
            EmptyStateScreen(
                message = "No work plan to display.",
                primaryAction = { onNavigateToInputForm() },
                primaryActionText = "Generate New Plan",
                secondaryAction = { onNavigateToSavedPlans() },
                secondaryActionText = "View Saved Plans"
            )
        }
    }
}

@Composable
fun LoadingStateScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Generating your work plan...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This may take a few moments",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ErrorStateScreen(
    error: String,
    onTryAgain: () -> Unit,
    onViewSavedPlans: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF44336).copy(alpha = 0.1f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⚠️",
                    style = MaterialTheme.typography.displaySmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Error loading plan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF44336),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onTryAgain,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Try Again")
                    }
                    FilledTonalButton(
                        onClick = onViewSavedPlans,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("View Saved Plans")
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateScreen(
    message: String,
    primaryAction: (() -> Unit)? = null,
    primaryActionText: String = "Action",
    secondaryAction: (() -> Unit)? = null,
    secondaryActionText: String = "Secondary"
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📋",
                    style = MaterialTheme.typography.displaySmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (primaryAction != null || secondaryAction != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        primaryAction?.let { action ->
                            Button(
                                onClick = action,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(primaryActionText)
                            }
                        }
                        secondaryAction?.let { action ->
                            FilledTonalButton(
                                onClick = action,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(secondaryActionText)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlanTypeDropdown(
    selectedPlanType: String,
    onPlanTypeChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val planTypes = listOf("Weekly", "Monthly")

    Box(modifier = Modifier.fillMaxWidth()) {
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
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
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
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = true
            override fun isSelectableYear(year: Int): Boolean = true
        }
    )
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Column(modifier = modifier) {
        FilledTonalButton(
            onClick = { showDatePickerDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
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
    val context = LocalContext.current
    val loginViewModel: LoginViewModel = viewModel(
        factory = LoginViewModel.LoginViewModelFactory(AppDatabase.getDatabase(context).userDao())
    )
    Surface {
        OrdinisApp(loginViewModel = loginViewModel, activityScope = rememberCoroutineScope())
    }
}