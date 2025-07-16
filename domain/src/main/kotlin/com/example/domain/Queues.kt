package com.example.domain

class Queues(
    val newQueue: MutableList<LearningItem>,
    val learnedPool: MutableList<LearningItem>
) {
    fun dequeueNewTarget(): LearningItem? {
        if (newQueue.isEmpty()) {
            return null
        }
        return newQueue.removeAt(0)
    }

    fun addLearnedItem(item: LearningItem) {
        item.isLearned = true
        learnedPool.add(item)
    }

    fun updateCounts(coachItems: List<LearningItem>, userItems: List<LearningItem>) {
        coachItems.forEach { coachItem ->
            newQueue.find { it.id == coachItem.id }?.presentationCount++
            learnedPool.find { it.id == coachItem.id }?.presentationCount++
        }

        userItems.forEach { userItem ->
            newQueue.find { it.id == userItem.id }?.usageCount++
            learnedPool.find { it.id == userItem.id }?.usageCount++
        }
    }
}
