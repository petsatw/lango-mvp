package com.example.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.util.UUID
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class StartSessionUseCaseTest {

    private lateinit var mockLearningRepository: LearningRepository
    private lateinit var startSessionUseCase: StartSessionUseCase

    @Before
    fun setup() {
        mockLearningRepository = mock(LearningRepository::class.java)
        startSessionUseCase = StartSessionUseCase(mockLearningRepository)
    }

    @Test
    fun `startSession returns queues from repository`() = runTest {
        // Arrange
        val initialNewItem = LearningItem("id1", "token1", 0, 0)
        val initialQueues = Queues(
            newQueue = mutableListOf(initialNewItem, LearningItem("id3", "token3", 0, 0)),
            learnedPool = mutableListOf(LearningItem("id2", "token2", 0, 0))
        )
        `when`(mockLearningRepository.loadQueues()).thenReturn(Result.success(
            Queues(
                newQueue = mutableListOf(initialNewItem.copy(), LearningItem("id3", "token3", 0, 0)),
                learnedPool = mutableListOf(LearningItem("id2", "token2", 0, 0))
            )
        ))

        // Act
        val resultSession = startSessionUseCase.startSession()

        // Assert
        assertTrue(resultSession.isSuccess)
        val session = resultSession.getOrThrow()
        assertEquals(initialNewItem.id, session.newTarget.id)
        assertEquals(initialQueues.learnedPool.size, session.queues.learnedPool.size)
        assertEquals(initialQueues.newQueue.size - 1, session.queues.newQueue.size) // newQueue size should be one less than initial
    }
}