package com.example.lango_coach_android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.EndSessionUseCase
import com.example.domain.GenerateDialogueUseCase
import com.example.domain.ProcessTurnUseCase
import com.example.domain.Queues
import com.example.domain.StartSessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val startSessionUseCase: StartSessionUseCase,
    private val processTurnUseCase: ProcessTurnUseCase,
    private val generateDialogueUseCase: GenerateDialogueUseCase,
    private val endSessionUseCase: EndSessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private var currentQueues: Queues? = null

    fun startSession() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val initialQueues = startSessionUseCase.startSession()
                currentQueues = initialQueues
                generateCoachDialogue()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error during session start")
            }
        }
    }

    fun processTurn(userResponseText: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Waiting
            currentQueues?.let { queues ->
                try {
                    val updatedQueues = processTurnUseCase.processTurn(queues, userResponseText)
                    currentQueues = updatedQueues
                    if (updatedQueues.newQueue.isEmpty()) {
                        _uiState.value = UiState.Congrats
                        endSessionUseCase.endSession(updatedQueues)
                        currentQueues = null
                    } else {
                        generateCoachDialogue()
                    }
                } catch (e: Exception) {
                    _uiState.value = UiState.Error(e.message ?: "Unknown error during turn processing")
                }
            } ?: run {
                _uiState.value = UiState.Error("Session not started.")
            }
        }
    }

    private suspend fun generateCoachDialogue() {
        currentQueues?.let { queues ->
            try {
                val coachText = generateDialogueUseCase.generatePrompt(queues)
                _uiState.value = UiState.CoachSpeaking(coachText)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error during dialogue generation")
            }
        }
    }

    fun endSession() {
        viewModelScope.launch {
            currentQueues?.let { queues ->
                endSessionUseCase.endSession(queues)
                currentQueues = null
                _uiState.value = UiState.Idle
            }
        }
    }
}