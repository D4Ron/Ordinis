package com.example.ordinis2.data.local

import androidx.compose.ui.input.key.type
import androidx.room.TypeConverter // Removed androidx.compose.ui.input.key.type as it's unused
import com.example.ordinis2.data.Task
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class WorkPlanTypeConverters {
    private val gson = Gson()

    // This is the @TypeConverter for Room to read from the database
    @TypeConverter
    fun fromTasksJson(json: String?): List<List<Task>>? {
        if (json == null) {
            return null
        }
        val type = object : TypeToken<List<List<Task>>>() {}.type
        return gson.fromJson(json, type)
    }

    // This is the @TypeConverter for Room to write to the database
    @TypeConverter
    fun tasksListToJson(tasks: List<List<Task>>?): String? { // Renamed for clarity to match its pair
        if (tasks == null) {
            return null
        }
        return gson.toJson(tasks)
    }


    fun jsonToTasksList(json: String?): List<List<Task>>? {
        if (json == null) {
            return null
        }
        // The logic is identical to fromTasksJson
        val type = object : TypeToken<List<List<Task>>>() {}.type
        return gson.fromJson(json, type)
    }


    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
