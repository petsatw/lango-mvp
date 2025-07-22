package com.example.domain

interface PromptTemplateLoader {
    suspend fun loadPromptTemplate(assetPath: String): Map<String, String>
}