package com.example.lango_coach_android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.EndSessionUseCase
import com.example.domain.GenerateDialogueUseCase
import com.example.domain.ProcessTurnUseCase
import com.example.domain.Queues
import com.example.domain.StartSessionUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class MainViewModel(
    private val startSessionUseCase: StartSessionUseCase,
    private val processTurnUseCase: ProcessTurnUseCase,
    private val generateDialogueUseCase: GenerateDialogueUseCase,
    private val endSessionUseCase: EndSessionUseCase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private var currentQueues: Queues? = null

    private val vmScope = CoroutineScope(SupervisorJob() + dispatcher)

    fun startSession() {
        vmScope.launch {
            _uiState.value = UiState.Loading

            startSessionUseCase.startSession()
                .onSuccess { queues ->
                    currentQueues = queues
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

            val queues = currentQueues
            if (queues == null) {
                _uiState.value = UiState.Error("Session not started")
                return@launch
            }

            processTurnUseCase.processTurn(queues, userResponseText)
                .onSuccess { updated ->
                    currentQueues = updated

                    if (updated.newQueue.isEmpty()) {
                        _uiState.value = UiState.Congrats
                        endSessionUseCase.endSession(updated)
                        currentQueues = null
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
        vmScope.launch {
            currentQueues?.let { queues ->
                endSessionUseCase.endSession(queues).onSuccess {
                    currentQueues = null
                    _uiState.value = UiState.Idle
                }.onFailure {
                    _uiState.value = UiState.Error(it.message ?: "Unknown error during session end")
                }
            }
        }
    }
}