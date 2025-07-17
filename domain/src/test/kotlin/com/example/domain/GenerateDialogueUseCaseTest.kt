package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class GenerateDialogueUseCaseTest {

    private lateinit var mockLearningRepository: LearningRepository
    private lateinit var generateDialogueUseCase: GenerateDialogueUseCase

    @Before
    fun setup() {
        mockLearningRepository = mock(LearningRepository::class.java)
        generateDialogueUseCase = GenerateDialogueUseCase(mockLearningRepository)
    }

    @Test
    fun `generatePrompt creates correct prompt string`() {
        // Arrange
        val newItems = mutableListOf(
            LearningItem("id1", "new1", "cat1", "sub1", 0, 0, false),
            LearningItem("id2", "new2", "cat2", "sub2", 0, 0, false)
        )
        val learnedItems = mutableListOf(
            LearningItem("id3", "learned1", "cat3", "sub3", 0, 0, true),
            LearningItem("id4", "learned2", "cat4", "sub4", 0, 0, true)
        )
        val queues = Queues(newItems, learnedItems)

        val expectedPrompt = "Generate a dialogue using new words: new1, new2. Learned words: learned1, learned2."

        // Act
        val resultPrompt = generateDialogueUseCase.generatePrompt(queues)

        // Assert
        assertEquals(expectedPrompt, resultPrompt)
    }
}