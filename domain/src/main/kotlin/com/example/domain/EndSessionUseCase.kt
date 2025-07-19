package com.example.domain

class EndSessionUseCase(private val learningRepository: LearningRepository) {
    fun endSession(queues: Queues) {
        learningRepository.saveQueues(queues)
    }
}