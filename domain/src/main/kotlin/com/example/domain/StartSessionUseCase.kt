package com.example.domain

class StartSessionUseCase(private val learningRepository: LearningRepository) {
    suspend fun startSession(): Result<Queues> {
        val queuesResult = learningRepository.loadQueues()
        return queuesResult.map { queues ->
            val target = queues.newQueue.removeFirst().apply {
                presentationCount = 0
                usageCount = 0
            }
            learningRepository.saveQueues(queues).getOrThrow() // persist reset counts
            queues        // return mutated reference
        }
    }
}