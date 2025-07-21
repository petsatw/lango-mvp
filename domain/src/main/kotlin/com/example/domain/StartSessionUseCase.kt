package com.example.domain

class StartSessionUseCase(private val learningRepository: LearningRepository) {
    suspend fun startSession(): Result<Queues> {
        val queuesResult = learningRepository.loadQueues()
                return queuesResult.map {
            it.newQueue.firstOrNull()?.let {
                it.usageCount = 0
                it.presentationCount = 0
            }
            it
        }
    }
}