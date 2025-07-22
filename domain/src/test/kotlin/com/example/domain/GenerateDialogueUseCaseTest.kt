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
    private lateinit var mockInitialPromptBuilder: InitialPromptBuilder
    private lateinit var generateDialogueUseCase: GenerateDialogueUseCase

    @Before
    fun setup() {
        mockLearningRepository = mockk()
        mockLlmService = mockk()
        mockInitialPromptBuilder = mockk()
        generateDialogueUseCase = GenerateDialogueUseCase(mockLearningRepository, mockLlmService, mockInitialPromptBuilder)
    }

    @Test
    fun `generatePrompt uses InitialPromptBuilder for new target introduction`() = runTest {
        // Arrange
        val newTarget = LearningItem("id1", "token1", 0, 0)
        val learnedItems = mutableListOf(
            LearningItem("id3", "token3", 0, 0)
        )
        val queues = Queues(mutableListOf(newTarget), learnedItems)

        val mockedInitialPrompt = "mocked initial prompt"
        val mockedLlmResponse = "mocked LLM response"

        coEvery { mockInitialPromptBuilder.build(queues, any()) } returns mockedInitialPrompt
        coEvery { mockLlmService.generateDialogue(mockedInitialPrompt) } returns mockedLlmResponse

        // Act
        val resultPrompt = generateDialogueUseCase.generatePrompt(queues)

        // Assert
        assertEquals(mockedLlmResponse, resultPrompt)
        coVerify { mockInitialPromptBuilder.build(queues, any()) }
        coVerify { mockLlmService.generateDialogue(mockedInitialPrompt) }
        assertEquals(1, newTarget.presentationCount)
    }

    @Test
    fun `generatePrompt creates correct prompt string for existing target`() = runTest {
        // Arrange
        val newTarget = LearningItem("id1", "token1", 1, 1)
        val learnedItems = mutableListOf(
            LearningItem("id3", "token3", 0, 0)
        )
        val queues = Queues(mutableListOf(newTarget), learnedItems)

        val expectedLlmPrompt = "Generate natural German dialogue using only 'token1' and items from the learned pool: token3. Bias towards less used or less familiar items (consider their usage and presentation counts). Avoid repeating the same learned pool item twice in a row. Frequently use strategic questions to elicit the learner’s use of 'token1'."
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
            learnedPool = mutableListOf(LearningItem("id1", "token1", 0, 0))
        )

        // Act
        val resultPrompt = generateDialogueUseCase.generatePrompt(queues)

        // Assert
        assertEquals("Congratulations! You've completed your learning objectives.", resultPrompt)
    }
}