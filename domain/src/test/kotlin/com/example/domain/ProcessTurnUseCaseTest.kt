package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class ProcessTurnUseCaseTest {

    private lateinit var mockLearningRepository: LearningRepository
    private lateinit var processTurnUseCase: ProcessTurnUseCase

    @Before
    fun setup() {
        mockLearningRepository = mock(LearningRepository::class.java)
        processTurnUseCase = ProcessTurnUseCase(mockLearningRepository)
    }

    @Test
    fun `processTurn increments usageCount if newTarget is in user response`() {
        // Arrange
        val newTarget = LearningItem("id1", "token1", "cat1", "sub1", 0, 0, false)
        val queues = Queues(
            newQueue = mutableListOf(newTarget),
            learnedPool = mutableListOf()
        )
        val userResponse = "User says token1"

        // Act
        val updatedQueues = processTurnUseCase.processTurn(queues, userResponse)

        // Assert
        assertEquals(1, updatedQueues.newQueue[0].usageCount)
        verify(mockLearningRepository).saveQueues(updatedQueues)
    }

    @Test
    fun `processTurn moves newTarget to learnedPool and dequeues next when mastered`() {
        // Arrange
        val newTarget = LearningItem("id1", "token1", "cat1", "sub1", 2, 0, false) // usageCount = 2
        val nextNewTarget = LearningItem("id2", "token2", "cat2", "sub2", 0, 0, false)
        val queues = Queues(
            newQueue = mutableListOf(newTarget, nextNewTarget),
            learnedPool = mutableListOf()
        )
        val userResponse = "User says token1"

        // Act
        val updatedQueues = processTurnUseCase.processTurn(queues, userResponse)

        // Assert
        assertTrue(updatedQueues.newQueue.first() == nextNewTarget) // Next new_target dequeued
        assertTrue(updatedQueues.learnedPool.contains(newTarget)) // Old new_target moved to learned pool
        assertEquals(3, newTarget.usageCount) // usageCount incremented to 3
        assertTrue(newTarget.isLearned) // isLearned set to true
        assertEquals(0, nextNewTarget.usageCount) // New new_target counts reset
        assertEquals(0, nextNewTarget.presentationCount) // New new_target counts reset
        verify(mockLearningRepository).saveQueues(updatedQueues)
    }

    @Test
    fun `processTurn does not increment usageCount if newTarget is not in user response`() {
        // Arrange
        val newTarget = LearningItem("id1", "token1", "cat1", "sub1", 0, 0, false)
        val queues = Queues(
            newQueue = mutableListOf(newTarget),
            learnedPool = mutableListOf()
        )
        val userResponse = "User says something else"

        // Act
        val updatedQueues = processTurnUseCase.processTurn(queues, userResponse)

        // Assert
        assertEquals(0, updatedQueues.newQueue[0].usageCount)
        verify(mockLearningRepository).saveQueues(updatedQueues)
    }
}