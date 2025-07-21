package com.example.domain

class EndSessionUseCase(private val learningRepository: LearningRepository) {
    suspend fun endSession(queues: Queues): Result<Unit> {
        return learningRepository.saveQueues(queues)
    }
}