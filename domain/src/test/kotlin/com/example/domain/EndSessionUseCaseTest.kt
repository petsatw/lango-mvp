package com.example.domain

import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class EndSessionUseCaseTest {

    private lateinit var mockLearningRepository: LearningRepository
    private lateinit var endSessionUseCase: EndSessionUseCase

    @Before
    fun setup() {
        mockLearningRepository = mock(LearningRepository::class.java)
        endSessionUseCase = EndSessionUseCase(mockLearningRepository)
    }

    @Test
    fun `endSession saves queues to repository`() {
        // Arrange
        val queues = Queues(
            newQueue = mutableListOf(LearningItem("id1", "token1", "cat1", "sub1", 0, 0, false)),
            learnedPool = mutableListOf(LearningItem("id2", "token2", "cat2", "sub2", 0, 0, true))
        )

        // Act
        endSessionUseCase.endSession(queues)

        // Assert
        verify(mockLearningRepository).saveQueues(queues)
    }
}