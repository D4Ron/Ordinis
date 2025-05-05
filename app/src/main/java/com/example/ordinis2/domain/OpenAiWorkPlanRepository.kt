package com.example.ordinis2.domain

import com.example.ordinis2.data.Task
import com.example.ordinis2.data.WorkPlan
import com.example.ordinis2.data.WorkSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class OpenAiWorkPlanRepository @Inject constructor(
    private val apiKey: String
) : IWorkPlanRepository {
    private val client = OkHttpClient()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override suspend fun generateWorkPlan(summary: WorkSummary): Result<WorkPlan> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildPrompt(summary)

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

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                    val content = JSONObject(body)
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")

                    val plan = extractWorkPlan(content, summary.desiredPlanType)
                    Result.success(plan)
                } else {
                    Result.failure(Exception("OpenAI Error: ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
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
                        val descAndDuration = parts[1].split("-")
                        val description = descAndDuration.getOrNull(0)?.trim() ?: ""
                        val daysText = descAndDuration.getOrNull(1)?.replace("days", "")?.replace("day", "")?.trim()
                        val days = daysText?.toIntOrNull() ?: 1
                        val deadline = Date(Date().time + days * 86_400_000L)
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

        return WorkPlan(planType, tasks)
    }
}
