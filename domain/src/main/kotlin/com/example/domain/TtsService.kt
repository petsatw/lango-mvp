package com.example.domain

interface TtsService {
    suspend fun speak(text: String)
}