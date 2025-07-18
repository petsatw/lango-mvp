package com.example.domain

class GenerateDialogueUseCase(private val learningRepository: LearningRepository, private val llmService: LlmService) {
    suspend fun generatePrompt(queues: Queues): String {
        val newTarget = queues.newQueue.firstOrNull()
        val learnedPool = queues.learnedPool

        val promptBuilder = StringBuilder()

        if (newTarget != null && newTarget.usageCount == 0 && newTarget.presentationCount == 0) {
            // Introduce brand new target
            promptBuilder.append("Say '${newTarget.token}' by itself. ")
            promptBuilder.append("Explain what '${newTarget.token}' means in a very short sentence that a 5-year-old would understand. ")
            promptBuilder.append("Give one simple example with '${newTarget.token}' that a 5-year-old would understand, preferably with items from the learned pool: ${learnedPool.joinToString { it.token }}. ")
            newTarget.presentationCount++ // Increment presentation count for new target introduction
        } else if (newTarget != null) {
            // Dialogue stage loop
            promptBuilder.append("Generate natural German dialogue using only '${newTarget.token}' and items from the learned pool: ${learnedPool.joinToString { it.token }}. ")
            promptBuilder.append("Bias towards less used or less familiar items (consider their usage and presentation counts). ")
            promptBuilder.append("Avoid repeating the same learned pool item twice in a row. ")
            promptBuilder.append("Frequently use strategic questions to elicit the learner’s use of '${newTarget.token}'.")
        } else {
            // New queue is empty, session completed
            return "Congratulations! You've completed your learning objectives."
        }

        val finalPrompt = promptBuilder.toString()
        return llmService.generateDialogue(finalPrompt)
    }
}