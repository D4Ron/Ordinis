package com.example.ordinis2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ordinis2.data.WorkPlan
import com.example.ordinis2.data.WorkSummary
import com.example.ordinis2.domain.IWorkPlanRepository
import com.example.ordinis2.domain.OpenAiWorkPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkPlanViewModel @jakarta.inject.Inject constructor(
    private val repository: OpenAiWorkPlanRepository
) : ViewModel() {

    private val _workPlanState = MutableStateFlow<WorkPlan?>(null)
    val workPlanState: StateFlow<WorkPlan?> = _workPlanState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    suspend fun generateWorkPlan(workSummary: WorkSummary) {
        _isLoading.value = true
        _errorMessage.value = null
        _workPlanState.value = null

        val result = repository.generateWorkPlan(workSummary)

        _isLoading.value = false
        result.onSuccess {
            _workPlanState.value = it
        }.onFailure {
            _errorMessage.value = it.message ?: "Failed to generate plan"
        }
    }
}
