package com.example.domain

class StartSessionUseCase(private val learningRepository: LearningRepository) {
    fun startSession(): Queues {
        // For now, just load queues from the repository. More complex logic can be added later.
        return learningRepository.loadQueues(null as Pair<String, String>?)
    }
}