package com.example.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever

class ProcessTurnUseCaseTest {

    private lateinit var mockLearningRepository: LearningRepository
    private lateinit var processTurnUseCase: ProcessTurnUseCase

    @Before
    fun setup() {
        mockLearningRepository = mock(LearningRepository::class.java)
        processTurnUseCase = ProcessTurnUseCase(mockLearningRepository)
    }

    @Test
    fun `processTurn increments usageCount if newTarget is in user response`() = runTest {
        // Arrange
        val newTarget = LearningItem("id1", "token1", 0, 0)
        val queues = Queues(
            newQueue = mutableListOf(newTarget),
            learnedPool = mutableListOf()
        )
        val userResponse = "User says token1"
        whenever(mockLearningRepository.saveQueues(org.mockito.kotlin.any())).thenReturn(Result.success(Unit))

        // Act
        val updatedQueuesResult = processTurnUseCase.processTurn(queues, userResponse)

        // Assert
        assertEquals(1, updatedQueuesResult.getOrThrow().newQueue[0].usageCount)
        verify(mockLearningRepository).saveQueues(queues)
    }

    @Test
    fun `processTurn moves newTarget to learnedPool and dequeues next when mastered`() = runTest {
        // Arrange
        val newTarget = LearningItem("id1", "token1", 0, 2) // usageCount = 2
        val nextNewTarget = LearningItem("id2", "token2", 0, 0)
        val queues = Queues(
            newQueue = mutableListOf(newTarget, nextNewTarget),
            learnedPool = mutableListOf()
        )
        val initialLearnedPoolSize = queues.learnedPool.size
        val userResponse = "User says token1"
        whenever(mockLearningRepository.saveQueues(org.mockito.kotlin.any())).thenReturn(Result.success(Unit))

        // Act
        val updatedQueuesResult = processTurnUseCase.processTurn(queues, userResponse)
        val updatedQueues = updatedQueuesResult.getOrThrow()

        // Assert
        // Assert
        assertEquals(1, updatedQueues.newQueue.size) // New queue should have one item
        assertEquals(initialLearnedPoolSize + 1, updatedQueues.learnedPool.size) // Learned pool should have one more item
        val movedItem = updatedQueues.learnedPool.first { it.id == newTarget.id } // Get the moved item from learnedPool
        assertTrue(movedItem.isLearned) // The moved item should now be learned
        assertEquals(3, movedItem.usageCount) // usageCount should be 3
    }

    @Test
    fun `processTurn does not increment usageCount if newTarget is not in user response`() = runTest {
        // Arrange
        val newTarget = LearningItem("id1", "token1", 0, 0)
        val queues = Queues(
            newQueue = mutableListOf(newTarget),
            learnedPool = mutableListOf()
        )
        val userResponse = "User says something else"
        whenever(mockLearningRepository.saveQueues(org.mockito.kotlin.any())).thenReturn(Result.success(Unit))

        // Act
        val updatedQueuesResult = processTurnUseCase.processTurn(queues, userResponse)

        // Assert
        assertEquals(0, updatedQueuesResult.getOrThrow().newQueue[0].usageCount)
        verify(mockLearningRepository).saveQueues(queues)
    }
}