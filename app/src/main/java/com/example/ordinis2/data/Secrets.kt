package com.example.ordinis2.data

import android.content.Context
import java.util.Properties
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject

class SecretsProvider @Inject constructor(
@ApplicationContext private val context: Context
) {
    fun getOpenAiKey(): String {
        val properties = Properties()
        context.assets.open("secrets.properties").use {
            properties.load(it)
        }
        return properties.getProperty("OPENAI_API_KEY") ?: ""
    }
}