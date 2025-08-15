package com.example.ordinis2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkPlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkPlan(workPlan: WorkPlanEntity)

    @Query("SELECT * FROM work_plans ORDER BY generatedDate DESC")
    fun getAllWorkPlans(): Flow<List<WorkPlanEntity>>

    @Query("SELECT * FROM work_plans WHERE id = :id")
    suspend fun getWorkPlanById(id: Long): WorkPlanEntity?


}
