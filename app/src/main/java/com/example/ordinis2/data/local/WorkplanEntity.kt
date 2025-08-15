package com.example.ordinis2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.ordinis2.data.Task
import java.util.Date

@Entity(tableName = "work_plans")
@TypeConverters(WorkPlanTypeConverters::class)
data class WorkPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workPlanType: String,
    val tasksJson: String,
    val projectTitle: String?,
    val projectDescription: String?,
    val userRole: String?,
    val generatedDate: Date,
    val deadline: Date?

)

