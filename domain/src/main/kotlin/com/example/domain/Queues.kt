package com.example.domain

import kotlinx.serialization.Serializable

@Serializable
data class Queues(
    val newQueue: MutableList<LearningItem>,
    val learnedPool: MutableList<LearningItem>
) {
    fun dequeueNewTarget(): LearningItem? {
        if (newQueue.isEmpty()) {
            return null
        }
        return newQueue.removeAt(0)
    }

    internal fun dequeueAndReset(): LearningItem? {
        if (newQueue.isEmpty()) {
            return null
        }
        val item = newQueue.removeAt(0)
        item.presentationCount = 0
        item.usageCount = 0
        return item
    }
}
