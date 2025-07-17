package com.example.domain

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
    fun `processTurn saves queues to repository`() {
        // Arrange
        val queues = Queues(
            newQueue = mutableListOf(LearningItem("id1", "token1", "cat1", "sub1", 0, 0, false)),
            learnedPool = mutableListOf(LearningItem("id2", "token2", "cat2", "sub2", 0, 0, true))
        )
        val presentedItem = LearningItem("id1", "token1", "cat1", "sub1", 0, 0, false)
        val isCorrect = true

        // Act
        processTurnUseCase.processTurn(queues, presentedItem, isCorrect)

        // Assert
        verify(mockLearningRepository).saveQueues(queues)
    }
}