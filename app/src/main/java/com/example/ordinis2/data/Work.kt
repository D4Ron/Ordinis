package com.example.ordinis2.data

import java.util.Date

// Data class to represent a single task
data class Task(
    val taskDescription: String,
    val taskDeadline: Date?,
    val taskEstimatedHours: Int?,
    val taskIsCompleted: Boolean = false
)

// Data class to represent the work plan
data class WorkPlan(
    val workPlanType: String,
    val tasks: List<List<Task>>
)

//Data class to represent Use input for the work plan
data class WorkSummary(
    val projectTitle: String,
    val projectDescription: String,
    val desiredPlanType: String,
    val userRole: String = "Project Manager",
    val projectStartDate: Date = Date(),
    val deadline: Date?
)