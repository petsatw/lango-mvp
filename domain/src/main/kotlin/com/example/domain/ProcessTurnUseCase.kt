package com.example.domain

class ProcessTurnUseCase(private val learningRepository: LearningRepository) {
    fun processTurn(queues: Queues, userResponseText: String): Queues {
        val currentNewTarget = queues.newQueue.firstOrNull()

        currentNewTarget?.let { newTarget ->
            // Check if user used the new_target
            if (userResponseText.contains(newTarget.token, ignoreCase = true)) {
                newTarget.usageCount++
            }

            // Mastery check
            if (newTarget.usageCount >= 3) {
                queues.dequeueNewTarget()?.let { masteredItem ->
                    masteredItem.isLearned = true
                    queues.learnedPool.add(masteredItem)
                }
                // Dequeue next new_target and reset its counts
                queues.newQueue.firstOrNull()?.let { nextNewTarget ->
                    nextNewTarget.usageCount = 0
                    nextNewTarget.presentationCount = 0
                }
            }
        }

        // Save the updated queues
        learningRepository.saveQueues(queues)
        return queues
    }
}