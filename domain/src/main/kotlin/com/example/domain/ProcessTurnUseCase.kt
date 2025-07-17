package com.example.domain

class ProcessTurnUseCase(private val learningRepository: LearningRepository) {
    fun processTurn(queues: Queues, presentedItem: LearningItem, isCorrect: Boolean): Queues {
        // For now, just save the queues. More complex logic will be added later.
        learningRepository.saveQueues(queues)
        return queues
    }
}