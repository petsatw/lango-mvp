package com.example.lango_coach_android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.CoachOrchestrator
import com.example.domain.GenerateDialogueUseCase
import com.example.domain.Session

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class MainViewModel(
    private val coachOrchestrator: CoachOrchestrator,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private var currentSession: Session? = null

    private val vmScope = CoroutineScope(SupervisorJob() + dispatcher)

    fun startSession() {
        vmScope.launch {
            _uiState.value = UiState.Loading

            coachOrchestrator.startSession()
                .onSuccess { session ->
                    currentSession = session
                    generateCoachDialogue()
                }
                .onFailure { e ->
                    _uiState.value = UiState.Error(
                        e.message ?: "Unknown error during session start"
                    )
                }
        }
    }

    fun processTurn(userResponseText: String) {
        vmScope.launch {
            _uiState.value = UiState.Waiting

            val session = currentSession
            if (session == null) {
                _uiState.value = UiState.Error("Session not started")
                return@launch
            }

            coachOrchestrator.processTurn(userResponseText)
                .onSuccess { session ->
                    currentSession = session
                    if (session.queues.newQueue.isEmpty()) {
                        _uiState.value = UiState.Congrats
                        currentSession = null
                    } else {
                        generateCoachDialogue()
                    }
                }
                .onFailure { e ->
                    _uiState.value = UiState.Error(
                        e.message ?: "Unknown error during turn processing"
                    )
                }
        }
    }

    private suspend fun generateCoachDialogue() {
        currentSession?.let { session ->
            try {
                val coachText = generateDialogueUseCase.generatePrompt(session.queues)
                _uiState.value = UiState.CoachSpeaking(coachText)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error during dialogue generation")
            }
        }
    }

    fun endSession() {
        vmScope.launch {
            currentSession?.let { session ->
                coachOrchestrator.endSession(session.queues).onSuccess {
                    currentSession = null
                    _uiState.value = UiState.Idle
                }.onFailure {
                    _uiState.value = UiState.Error(it.message ?: "Unknown error during session end")
                }
            }
        }
    }
}