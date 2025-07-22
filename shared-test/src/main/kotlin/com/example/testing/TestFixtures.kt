package com.example.testing

import com.example.domain.LearningItem
import com.example.domain.Queues

object TestFixtures {
    fun dummyItem(
        id: String = "ID_001",
        token: String = "token",
        presentation: Int = 0,
        usage: Int = 0,
        learned: Boolean = false
    ) = LearningItem(id, token, presentation, usage, learned)

    fun queuesFixture(
        new: List<LearningItem> = listOf(dummyItem()),
        learned: List<LearningItem> = emptyList()
    ) = Queues(new.toMutableList(), learned.toMutableList())
}
