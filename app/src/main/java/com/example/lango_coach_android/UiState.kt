package com.example.lango_coach_android

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    object Listening : UiState()
    object Waiting : UiState()
    data class Error(val message: String) : UiState()
    data class CoachSpeaking(val text: String) : UiState()
    object Congrats : UiState()
}