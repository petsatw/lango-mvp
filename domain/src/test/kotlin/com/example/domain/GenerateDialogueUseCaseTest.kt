package com.example.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.every
import io.mockk.verify

class GenerateDialogueUseCaseTest {

    private lateinit var mockLearningRepository: LearningRepository
    private lateinit var mockLlmService: LlmService
    private lateinit var generateDialogueUseCase: GenerateDialogueUseCase

    @Before
    fun setup() {
        mockLearningRepository = mockk()
        mockLlmService = mockk()
        generateDialogueUseCase = GenerateDialogueUseCase(mockLearningRepository, mockLlmService)
    }

    @Test
    fun `generatePrompt creates correct prompt string for new target`() = runTest {
        // Arrange
        val newTarget = LearningItem("id1", "new1", "cat1", "sub1", 0, 0, false)
        val learnedItems = mutableListOf(
            LearningItem("id3", "learned1", "cat3", "sub3", 0, 0, true)
        )
        val queues = Queues(mutableListOf(newTarget), learnedItems)

        val expectedLlmPrompt = "Say 'new1' by itself. Explain what 'new1' means in a very short sentence that a 5-year-old would understand. Give one simple example with 'new1' that a 5-year-old would understand, preferably with items from the learned pool: learned1. "
        val expectedLlmResponse = "LLM generated response for new target"

        coEvery { mockLlmService.generateDialogue(expectedLlmPrompt) } returns expectedLlmResponse

        // Act
        val resultPrompt = generateDialogueUseCase.generatePrompt(queues)

        // Assert
        assertEquals(expectedLlmResponse, resultPrompt)
        assertEquals(1, newTarget.presentationCount)
    }

    @Test
    fun `generatePrompt creates correct prompt string for existing target`() = runTest {
        // Arrange
        val newTarget = LearningItem("id1", "new1", "cat1", "sub1", 1, 1, false)
        val learnedItems = mutableListOf(
            LearningItem("id3", "learned1", "cat3", "sub3", 0, 0, true)
        )
        val queues = Queues(mutableListOf(newTarget), learnedItems)

        val expectedLlmPrompt = "Generate natural German dialogue using only 'new1' and items from the learned pool: learned1. Bias towards less used or less familiar items (consider their usage and presentation counts). Avoid repeating the same learned pool item twice in a row. Frequently use strategic questions to elicit the learner’s use of 'new1'."
        val expectedLlmResponse = "LLM generated response for existing target"

        coEvery { mockLlmService.generateDialogue(expectedLlmPrompt) } returns expectedLlmResponse

        // Act
        val resultPrompt = generateDialogueUseCase.generatePrompt(queues)

        // Assert
        assertEquals(expectedLlmResponse, resultPrompt)
    }

    @Test
    fun `generatePrompt returns congrats message if newQueue is empty`() = runTest {
        // Arrange
        val queues = Queues(
            newQueue = mutableListOf(),
            learnedPool = mutableListOf(LearningItem("id1", "token1", "cat1", "sub1", 0, 0, true))
        )

        // Act
        val resultPrompt = generateDialogueUseCase.generatePrompt(queues)

        // Assert
        assertEquals("Congratulations! You've completed your learning objectives.", resultPrompt)
    }
}