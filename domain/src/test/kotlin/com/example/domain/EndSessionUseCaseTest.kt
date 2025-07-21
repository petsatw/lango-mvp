package com.example.domain

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever

class EndSessionUseCaseTest {

    private lateinit var mockLearningRepository: LearningRepository
    private lateinit var endSessionUseCase: EndSessionUseCase

    @Before
    fun setup() {
        mockLearningRepository = mock(LearningRepository::class.java)
        endSessionUseCase = EndSessionUseCase(mockLearningRepository)
    }

    @Test
    fun `endSession saves queues to repository`() = runTest {
        // Arrange
        val queues = Queues(
            newQueue = mutableListOf(LearningItem("id1", "token1", "cat1", "sub1", 0, 0, false)),
            learnedPool = mutableListOf(LearningItem("id2", "token2", "cat2", "sub2", 0, 0, true))
        )
        whenever(mockLearningRepository.saveQueues(queues)).thenReturn(Result.success(Unit))

        // Act
        endSessionUseCase.endSession(queues)

        // Assert
        verify(mockLearningRepository).saveQueues(queues)
    }
}