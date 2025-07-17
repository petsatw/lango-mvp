package com.example.domain

class GenerateDialogueUseCase(private val learningRepository: LearningRepository) {
    fun generatePrompt(queues: Queues): String {
        // For now, a simple prompt. More complex logic will be added later.
        return "Generate a dialogue using new words: ${queues.newQueue.joinToString { it.token }}. Learned words: ${queues.learnedPool.joinToString { it.token }}."
    }
}