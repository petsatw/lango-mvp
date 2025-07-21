package com.example.domain

class ProcessTurnUseCase(private val learningRepository: LearningRepository) {
    suspend fun processTurn(queues: Queues, userResponseText: String): Result<Queues> {
        val currentNewTarget = queues.newQueue.firstOrNull()

        currentNewTarget?.let { newTarget ->
            // Check if user used the new_target
            if (userResponseText.contains(newTarget.token, ignoreCase = true)) {
                newTarget.usageCount++
            }

            // Mastery check
            if (newTarget.usageCount >= 3) {
                queues.newQueue.removeAt(0)
                newTarget.isLearned = true
                queues.learnedPool.add(newTarget)
            }
        }

        // Save the updated queues
        learningRepository.saveQueues(queues)
        return Result.success(queues)
    }
}