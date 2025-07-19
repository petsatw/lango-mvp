package com.example.domain

interface LlmService {
    suspend fun generateDialogue(prompt: String): String
}
