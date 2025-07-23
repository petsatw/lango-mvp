package com.example.domain

import java.util.UUID

class StartSessionUseCase(private val learningRepository: LearningRepository) {
    suspend fun startSession(): Result<Session> {
        val queuesResult = learningRepository.loadQueues()
        return queuesResult.map { queues ->
            val newTarget = queues.newQueue.removeFirst().apply {
                presentationCount = 0
                usageCount = 0
            }
            learningRepository.saveQueues(queues).getOrThrow() // persist reset counts
            Session(
                sessionId = UUID.randomUUID().toString(),
                startTime = System.currentTimeMillis(),
                queues = queues,
                newTarget = newTarget
            )
        }
    }
}