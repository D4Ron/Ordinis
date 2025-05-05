package com.example.ordinis2.domain

import com.example.ordinis2.data.WorkPlan
import com.example.ordinis2.data.WorkSummary

interface IWorkPlanRepository {
    suspend fun generateWorkPlan(summary: WorkSummary): Result<WorkPlan>
}