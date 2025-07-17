package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QueuesTest {

    private lateinit var newQueue: MutableList<LearningItem>
    private lateinit var learnedPool: MutableList<LearningItem>
    private lateinit var queues: Queues

    @Before
    fun setup() {
        newQueue = mutableListOf(
            LearningItem("id1", "text1", "cat1", "sub1", 0, 0, false),
            LearningItem("id2", "text2", "cat2", "sub2", 0, 0, false)
        )
        learnedPool = mutableListOf(
            LearningItem("id3", "text3", "cat3", "sub3", 5, 5, true)
        )
        queues = Queues(newQueue, learnedPool)
    }

    @Test
    fun `dequeueNewTarget should return first item and remove it from newQueue`() {
        val initialSize = newQueue.size
        val dequeuedItem = queues.dequeueNewTarget()

        assertEquals("id1", dequeuedItem?.id)
        assertEquals(initialSize - 1, newQueue.size)
        assertFalse(newQueue.contains(dequeuedItem))
    }

    @Test
    fun `dequeueNewTarget should return null if newQueue is empty`() {
        newQueue.clear()
        val dequeuedItem = queues.dequeueNewTarget()
        assertEquals(null, dequeuedItem)
    }

    @Test
    fun `addLearnedItem should add item to learnedPool`() {
        val itemToAdd = LearningItem("id4", "text4", "cat4", "sub4", 0, 0, false)
        val initialSize = learnedPool.size
        queues.addLearnedItem(itemToAdd)

        assertEquals(initialSize + 1, learnedPool.size)
        assertTrue(learnedPool.contains(itemToAdd))
        assertTrue(itemToAdd.isLearned)
    }

    @Test
    fun `updateCounts should increment presentationCount for coach items`() {
        val coachItems = listOf(
            LearningItem("id1", "text1", "cat1", "sub1", 0, 0, false),
            LearningItem("id3", "text3", "cat3", "sub3", 5, 5, true)
        )
        val initialPresentationCount1 = newQueue[0].presentationCount
        val initialPresentationCount3 = learnedPool[0].presentationCount

        queues.updateCounts(coachItems, emptyList())

        assertEquals(initialPresentationCount1 + 1, newQueue[0].presentationCount)
        assertEquals(initialPresentationCount3 + 1, learnedPool[0].presentationCount)
    }

    @Test
    fun `updateCounts should increment usageCount for user items`() {
        val userItems = listOf(
            LearningItem("id1", "text1", "cat1", "sub1", 0, 0, false),
            LearningItem("id3", "text3", "cat3", "sub3", 5, 5, true)
        )
        val initialUsageCount1 = newQueue[0].usageCount
        val initialUsageCount3 = learnedPool[0].usageCount

        queues.updateCounts(emptyList(), userItems)

        assertEquals(initialUsageCount1 + 1, newQueue[0].usageCount)
        assertEquals(initialUsageCount3 + 1, learnedPool[0].usageCount)
    }

    @Test
    fun `updateCounts should handle mixed coach and user items`() {
        val coachItems = listOf(newQueue[0])
        val userItems = listOf(learnedPool[0])

        val initialPresentationCount1 = newQueue[0].presentationCount
        val initialUsageCount3 = learnedPool[0].usageCount

        queues.updateCounts(coachItems, userItems)

        assertEquals(initialPresentationCount1 + 1, newQueue[0].presentationCount)
        assertEquals(initialUsageCount3 + 1, learnedPool[0].usageCount)
    }

    @Test
    fun `copy should create independent copies of mutable lists`() {
        val copiedQueues = queues.copy(
            newQueue = queues.newQueue.toMutableList(),
            learnedPool = queues.learnedPool.toMutableList()
        )

        // Modify the copied queues
        copiedQueues.newQueue.add(LearningItem("id4", "text4", "cat4", "sub4", 0, 0, false))
        copiedQueues.learnedPool.removeAt(0)

        // Assert that the original queues remain unchanged
        assertEquals(2, queues.newQueue.size)
        assertEquals(1, queues.learnedPool.size)
        assertEquals("id1", queues.newQueue[0].id)
        assertEquals("id3", queues.learnedPool[0].id)

        // Assert that the copied queues reflect the changes
        assertEquals(3, copiedQueues.newQueue.size)
        assertEquals(0, copiedQueues.learnedPool.size)
        assertEquals("id4", copiedQueues.newQueue[2].id)
    }
}
