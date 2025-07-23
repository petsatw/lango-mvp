package com.example.testing

import com.example.domain.LearningItem
import com.example.domain.Queues

object TestFixtures {
    private const val ZERO_UUID = "00000000-0000-0000-0000-000000000000"
    fun String.normaliseUuid() = replace(
        // raw string literal: no need to escape quotes or backslashes
        Regex("""("sessionId"\s*:\s*")[^"]+(")"""),
        // use the two captured groups to preserve the quotes and any whitespace
        "$1$ZERO_UUID$2"
    ).trim()
    fun dummyItem(
        id: String = "ID_001",
        token: String = "token",
        presentation: Int = 0,
        usage: Int = 0,
        learned: Boolean = false
    ) = LearningItem(id, token, presentation, usage, learned)

    fun queuesFixture(
        newCount: Int = 1,
        learnedCount: Int = 0,
        newItems: List<LearningItem> = (1..newCount).map { dummyItem(id = "new_$it") },
        learnedItems: List<LearningItem> = (1..learnedCount).map { dummyItem(id = "learned_$it", learned = true) }
    ) = Queues(newItems.toMutableList(), learnedItems.toMutableList())
}
