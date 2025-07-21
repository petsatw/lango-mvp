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
            LearningItem("id1", "token1", 0, 0),
            LearningItem("id2", "token2", 0, 0)
        )
        learnedPool = mutableListOf(
            LearningItem("id3", "token3", 5, 5)
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

    
}
