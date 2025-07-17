package com.example.lango_coach_android

import io.mockk.mockk
import io.mockk.every
import io.mockk.verify
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.Runs
import io.mockk.just

import com.example.domain.EndSessionUseCase
import com.example.domain.GenerateDialogueUseCase
import com.example.domain.LearningItem
import com.example.domain.ProcessTurnUseCase
import com.example.domain.Queues
import com.example.domain.StartSessionUseCase
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class MainViewModelTest {

    private lateinit var startSessionUseCase: StartSessionUseCase
    private lateinit var processTurnUseCase: ProcessTurnUseCase
    private lateinit var generateDialogueUseCase: GenerateDialogueUseCase
    private lateinit var endSessionUseCase: EndSessionUseCase
    private lateinit var viewModel: MainViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        startSessionUseCase = mockk()
        processTurnUseCase = mockk()
        generateDialogueUseCase = mockk()
        endSessionUseCase = mockk(relaxUnitFun = true)

        viewModel = MainViewModel(
            startSessionUseCase,
            processTurnUseCase,
            generateDialogueUseCase,
            endSessionUseCase
        )
    }

    @Test
    fun `startSession updates uiState to Loading then CoachSpeaking`() = runTest {
        val initialQueues = Queues(
            newQueue = mutableListOf(LearningItem("id1", "token1", "cat1", "sub1", 0, 0, false)),
            learnedPool = mutableListOf(LearningItem("id2", "token2", "cat2", "sub2", 0, 0, true))
        )
        val expectedCoachText = "Hello from coach"

        every { startSessionUseCase.startSession() } returns initialQueues
        coEvery { generateDialogueUseCase.generatePrompt(initialQueues) } returns expectedCoachText

        viewModel.uiState.test {
            assertEquals(UiState.Idle, awaitItem())
            viewModel.startSession()
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.CoachSpeaking(expectedCoachText), awaitItem())
        }
        verify { startSessionUseCase.startSession() }
        coVerify { generateDialogueUseCase.generatePrompt(initialQueues) }
    }

    @Test
    fun `processTurn updates uiState to Waiting then CoachSpeaking`() = runTest {
        val initialQueues = Queues(
            newQueue = mutableListOf(LearningItem("id1", "token1", "cat1", "sub1", 0, 0, false)),
            learnedPool = mutableListOf(LearningItem("id2", "token2", "cat2", "sub2", 0, 0, true))
        )
        val updatedQueues = Queues(
            newQueue = mutableListOf(LearningItem("id1", "token1", "cat1", "sub1", 1, 0, false)),
            learnedPool = mutableListOf(LearningItem("id2", "token2", "cat2", "sub2", 0, 0, true))
        )
        val userResponse = "user says token1"
        val expectedCoachText = "Next coach response"

        every { startSessionUseCase.startSession() } returns initialQueues
        coEvery { generateDialogueUseCase.generatePrompt(initialQueues) } returns "Initial coach text"
        every { processTurnUseCase.processTurn(initialQueues, userResponse) } returns updatedQueues
        coEvery { generateDialogueUseCase.generatePrompt(updatedQueues) } returns expectedCoachText

        viewModel.uiState.test {
            assertEquals(UiState.Idle, awaitItem())
            viewModel.startSession()
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.CoachSpeaking("Initial coach text"), awaitItem())

            viewModel.processTurn(userResponse)
            assertEquals(UiState.Waiting, awaitItem())
            assertEquals(UiState.CoachSpeaking(expectedCoachText), awaitItem())
        }
        verify { processTurnUseCase.processTurn(initialQueues, userResponse) }
        coVerify { generateDialogueUseCase.generatePrompt(updatedQueues) }
    }

    @Test
    fun `processTurn handles mastery and updates uiState to Congrats`() = runTest {
        val initialQueues = Queues(
            newQueue = mutableListOf(LearningItem("id1", "token1", "cat1", "sub1", 2, 0, false)), // usageCount 2
            learnedPool = mutableListOf(LearningItem("id2", "token2", "cat2", "sub2", 0, 0, true))
        )
        val masteredQueues = Queues(
            newQueue = mutableListOf(), // newQueue is empty after mastery
            learnedPool = mutableListOf(LearningItem("id2", "token2", "cat2", "sub2", 0, 0, true), LearningItem("id1", "token1", "cat1", "sub1", 3, 0, true)) // usageCount 3
        )
        val userResponse = "user says token1"

        every { startSessionUseCase.startSession() } returns initialQueues
        coEvery { generateDialogueUseCase.generatePrompt(initialQueues) } returns "Initial coach text"
        every { processTurnUseCase.processTurn(initialQueues, userResponse) } returns masteredQueues
        every { endSessionUseCase.endSession(masteredQueues) } just Runs

        viewModel.uiState.test {
            assertEquals(UiState.Idle, awaitItem())
            viewModel.startSession()
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.CoachSpeaking("Initial coach text"), awaitItem())

            viewModel.processTurn(userResponse)
            assertEquals(UiState.Waiting, awaitItem())
            assertEquals(UiState.Congrats, awaitItem())
        }
        verify { processTurnUseCase.processTurn(initialQueues, userResponse) }
        verify { endSessionUseCase.endSession(masteredQueues) }
    }

    @Test
    fun `endSession updates uiState to Idle`() = runTest {
        val initialQueues = Queues(
            newQueue = mutableListOf(LearningItem("id1", "token1", "cat1", "sub1", 0, 0, false)),
            learnedPool = mutableListOf(LearningItem("id2", "token2", "cat2", "sub2", 0, 0, true))
        )

        every { startSessionUseCase.startSession() } returns initialQueues
        coEvery { generateDialogueUseCase.generatePrompt(initialQueues) } returns "Initial coach text"
        every { endSessionUseCase.endSession(initialQueues) } just Runs

        viewModel.uiState.test {
            assertEquals(UiState.Idle, awaitItem())
            viewModel.startSession()
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.CoachSpeaking("Initial coach text"), awaitItem())

            viewModel.endSession()
            assertEquals(UiState.Idle, awaitItem())
        }
        verify { endSessionUseCase.endSession(initialQueues) }
    }

    @Test
    fun `startSession handles error and updates uiState to Error`() = runTest {
        val errorMessage = "Failed to load queues"
        every { startSessionUseCase.startSession() } throws RuntimeException(errorMessage)

        viewModel.uiState.test {
            assertEquals(UiState.Idle, awaitItem())
            viewModel.startSession()
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Error(errorMessage), awaitItem())
        }
        verify { startSessionUseCase.startSession() }
    }

    @Test
    fun `processTurn handles error and updates uiState to Error`() = runTest {
        val initialQueues = Queues(
            newQueue = mutableListOf(LearningItem("id1", "token1", "cat1", "sub1", 0, 0, false)),
            learnedPool = mutableListOf(LearningItem("id2", "token2", "cat2", "sub2", 0, 0, true))
        )
        val errorMessage = "Failed to process turn"
        val userResponse = "some response"

        every { startSessionUseCase.startSession() } returns initialQueues
        coEvery { generateDialogueUseCase.generatePrompt(initialQueues) } returns "Initial coach text"
        every { processTurnUseCase.processTurn(initialQueues, userResponse) } throws RuntimeException(errorMessage)

        viewModel.uiState.test {
            assertEquals(UiState.Idle, awaitItem())
            viewModel.startSession()
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.CoachSpeaking("Initial coach text"), awaitItem())

            viewModel.processTurn(userResponse)
            assertEquals(UiState.Waiting, awaitItem())
            assertEquals(UiState.Error(errorMessage), awaitItem())
        }
        verify { processTurnUseCase.processTurn(initialQueues, userResponse) }
    }

    @Test
    fun `generateCoachDialogue handles error and updates uiState to Error`() = runTest {
        val initialQueues = Queues(
            newQueue = mutableListOf(LearningItem("id1", "token1", "cat1", "sub1", 0, 0, false)),
            learnedPool = mutableListOf(LearningItem("id2", "token2", "cat2", "sub2", 0, 0, true))
        )
        val errorMessage = "Failed to generate dialogue"

        every { startSessionUseCase.startSession() } returns initialQueues
        coEvery { generateDialogueUseCase.generatePrompt(initialQueues) } throws RuntimeException(errorMessage)

        viewModel.uiState.test {
            assertEquals(UiState.Idle, awaitItem())
            viewModel.startSession()
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Error(errorMessage), awaitItem())
        }
        coVerify { generateDialogueUseCase.generatePrompt(initialQueues) }
    }
}