package com.example.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
        val expectedQueues = Queues(
            newQueue = mutableListOf(LearningItem("id1", "token1", 0, 0)),
            learnedPool = mutableListOf(LearningItem("id2", "token2", 0, 0))
        )
        `when`(mockLearningRepository.loadQueues()).thenReturn(Result.success(expectedQueues))

        // Act
        val resultQueues = startSessionUseCase.startSession()

        // Assert
        assertEquals(Result.success(expectedQueues), resultQueues)
    }
}