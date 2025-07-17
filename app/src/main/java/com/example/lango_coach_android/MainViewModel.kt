package com.example.lango_coach_android

import androidx.lifecycle.ViewModel
import com.example.domain.EndSessionUseCase
import com.example.domain.GenerateDialogueUseCase
import com.example.domain.ProcessTurnUseCase
import com.example.domain.Queues
import com.example.domain.LearningItem
import com.example.domain.StartSessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(
    private val startSessionUseCase: StartSessionUseCase,
    private val processTurnUseCase: ProcessTurnUseCase,
    private val generateDialogueUseCase: GenerateDialogueUseCase,
    private val endSessionUseCase: EndSessionUseCase
) : ViewModel() {

    private val _queues = MutableStateFlow<Queues?>(null)
    val queues: StateFlow<Queues?> = _queues

    fun startSession() {
        val initialQueues = startSessionUseCase.startSession()
        _queues.value = initialQueues
    }

    fun processTurn(presentedItem: LearningItem, isCorrect: Boolean) {
        _queues.value?.let {
            currentQueues ->
            val updatedQueues = processTurnUseCase.processTurn(currentQueues, presentedItem, isCorrect)
            _queues.value = updatedQueues
        }
    }

    fun generateDialogue(): String {
        return _queues.value?.let {
            currentQueues ->
            generateDialogueUseCase.generatePrompt(currentQueues)
        } ?: ""
    }

    fun endSession() {
        _queues.value?.let {
            currentQueues ->
            endSessionUseCase.endSession(currentQueues)
            _queues.value = null // Clear queues after session ends
        }
    }
}