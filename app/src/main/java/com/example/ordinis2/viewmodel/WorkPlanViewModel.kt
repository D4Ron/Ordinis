package com.example.ordinis2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ordinis2.data.WorkPlan
import com.example.ordinis2.data.WorkSummary
import com.example.ordinis2.data.local.WorkPlanDao
import com.example.ordinis2.data.local.WorkPlanEntity
import com.example.ordinis2.data.local.WorkPlanTypeConverters
import com.example.ordinis2.domain.OpenAiWorkPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class WorkPlanViewModel @Inject constructor(
    private val repository: OpenAiWorkPlanRepository,
    private val workPlanDao: WorkPlanDao
) : ViewModel() {

    private val _workPlanState = MutableStateFlow<WorkPlan?>(null)
    val workPlanState: StateFlow<WorkPlan?> = _workPlanState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    // NEW: Flag to indicate if the current plan was loaded from DB
    private val _isPlanLoadedFromDb = MutableStateFlow(false)
    val isPlanLoadedFromDb: StateFlow<Boolean> = _isPlanLoadedFromDb.asStateFlow()

    val allWorkPlans: Flow<List<WorkPlanEntity>> = workPlanDao.getAllWorkPlans()


    fun generateWorkPlan(workSummary: WorkSummary) {
        _isLoading.value = true
        _errorMessage.value = null
        _workPlanState.value = null
        _isPlanLoadedFromDb.value = false

        viewModelScope.launch {
            val result = repository.generateWorkPlan(workSummary)

            _isLoading.value = false
            result.onSuccess { generatedPlan ->
                _workPlanState.value = generatedPlan

            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to generate plan"
            }
        }
    }
    fun loadWorkPlanFromEntity(workPlanEntity: WorkPlanEntity) {
        _isLoading.value = true
        _errorMessage.value = null
        _workPlanState.value = null


        viewModelScope.launch {
            try {
                val tasks = WorkPlanTypeConverters().jsonToTasksList(workPlanEntity.tasksJson)
                if (tasks != null) {
                    val workPlan = WorkPlan(
                        workPlanType = workPlanEntity.workPlanType,
                        tasks = tasks
                    )
                    _workPlanState.value = workPlan
                    _isPlanLoadedFromDb.value = true // Set flag: plan was loaded
                } else {
                    _errorMessage.value = "Failed to parse tasks from saved plan."
                    _isPlanLoadedFromDb.value = false // Failed to load
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading saved plan: ${e.message}"
                _isPlanLoadedFromDb.value = false // Failed to load
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun saveCurrentWorkPlan(workPlan: WorkPlan, workSummary: WorkSummary?) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val entity = mapWorkPlanToEntity(workPlan, workSummary)
                workPlanDao.insertWorkPlan(entity)
                _saveMessage.value = "Work plan saved successfully!"
                _isPlanLoadedFromDb.value = true // After saving, it's effectively "loaded from DB"
            } catch (e: Exception) {
                _saveMessage.value = "Failed to save work plan: ${e.message}"
                // _isPlanLoadedFromDb state doesn't change on save failure
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSaveMessage() {
        _saveMessage.value = null
    }
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    // Call this if you navigate away or want to reset the "loaded" status
    fun clearCurrentWorkPlanState() {
        _workPlanState.value = null
        _errorMessage.value = null
        _isPlanLoadedFromDb.value = false
    }


    private fun mapWorkPlanToEntity(workPlan: WorkPlan, workSummary: WorkSummary?): WorkPlanEntity {
        val tasksJson = WorkPlanTypeConverters().tasksListToJson(workPlan.tasks) ?: ""

        return WorkPlanEntity(
            // If workSummary is null (e.g., loading from entity without new summary),
            // you might want to fetch existing details from the entity if you had an ID.
            // For now, it assumes a new save or re-save with potentially new summary details.
            workPlanType = workPlan.workPlanType,
            tasksJson = tasksJson,
            projectTitle = workSummary?.projectTitle, // This might be an issue if saving an already loaded plan without a 'fresh' summary
            projectDescription = workSummary?.projectDescription,
            userRole = workSummary?.userRole,
            generatedDate = Date(), // This will always update the generatedDate on save
            deadline = workSummary?.deadline
        )
    }
}
