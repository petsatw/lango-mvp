package com.example.domain

class StartSessionUseCase(private val learningRepository: LearningRepository) {
    fun startSession(): Queues {
        val queues = learningRepository.loadQueues(null as Pair<String, String>?)
        queues.dequeueNewTarget()?.let {
            it.usageCount = 0
            it.presentationCount = 0
        }
        return queues
    }
}