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
import com.example.domain.ProcessTurnUseCase
import com.example.domain.StartSessionUseCase
import com.example.testing.TestFixtures.dummyItem
import com.example.testing.TestFixtures.queuesFixture
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
        val initialQueues = queuesFixture(
            newItems = mutableListOf(dummyItem("id1", "token1", 0, 0, false)),
            learnedItems = mutableListOf(dummyItem("id2", "token2", 0, 0, true))
        )
    }

    @Test
    fun `processTurn updates uiState to Waiting then CoachSpeaking`() = runTest {
        val initialQueues = queuesFixture(
            newItems = mutableListOf(dummyItem("id1", "token1", 0, 0, false)),
            learnedItems = mutableListOf(dummyItem("id2", "token2", 0, 0, true))
        )
        val updatedQueues = queuesFixture(
            newItems = mutableListOf(dummyItem("id1", "token1", 1, 0, false)),
            learnedItems = mutableListOf(dummyItem("id2", "token2", 0, 0, true))
        )
        val userResponse = "user says token1"
        val expectedCoachText = "Next coach response"

        coEvery { startSessionUseCase.startSession() } returns Result.success(initialQueues)
        coEvery { generateDialogueUseCase.generatePrompt(initialQueues) } returns "Initial coach text"
        coEvery { processTurnUseCase.processTurn(initialQueues, userResponse) } returns Result.success(updatedQueues)
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
        coVerify { processTurnUseCase.processTurn(initialQueues, userResponse) }
        coVerify { generateDialogueUseCase.generatePrompt(updatedQueues) }
    }

    @Test
    fun `processTurn handles mastery and updates uiState to Congrats`() = runTest {
        val initialQueues = queuesFixture(
            newItems = mutableListOf(dummyItem("id1", "token1", 2, 0, false)), // usageCount 2
            learnedItems = mutableListOf(dummyItem("id2", "token2", 0, 0, true))
        )
        val masteredQueues = queuesFixture(
            newItems = mutableListOf(), // newQueue is empty after mastery
            learnedItems = mutableListOf(dummyItem("id2", "token2", 0, 0, true), dummyItem("id1", "token1", 3, 0, true)) // usageCount 3
        )
        val userResponse = "user says token1"

        coEvery { startSessionUseCase.startSession() } returns Result.success(initialQueues)
        coEvery { generateDialogueUseCase.generatePrompt(initialQueues) } returns "Initial coach text"
        coEvery { processTurnUseCase.processTurn(initialQueues, userResponse) } returns Result.success(masteredQueues)
        coEvery { endSessionUseCase.endSession(masteredQueues) } returns Result.success(Unit)

        viewModel.uiState.test {
            assertEquals(UiState.Idle, awaitItem())
            viewModel.startSession()
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.CoachSpeaking("Initial coach text"), awaitItem())

            viewModel.processTurn(userResponse)
            assertEquals(UiState.Waiting, awaitItem())
            assertEquals(UiState.Congrats, awaitItem())
        }
        coVerify { processTurnUseCase.processTurn(initialQueues, userResponse) }
        coVerify { endSessionUseCase.endSession(masteredQueues) }
    }

    @Test
    fun `endSession updates uiState to Idle`() = runTest {
        val initialQueues = queuesFixture(
            newItems = mutableListOf(dummyItem("id1", "token1", 0, 0, false)),
            learnedItems = mutableListOf(dummyItem("id2", "token2", 0, 0, true))
        )

        coEvery { startSessionUseCase.startSession() } returns Result.success(initialQueues)
        coEvery { generateDialogueUseCase.generatePrompt(initialQueues) } returns "Initial coach text"
        coEvery { endSessionUseCase.endSession(initialQueues) } returns Result.success(Unit)

        viewModel.uiState.test {
            assertEquals(UiState.Idle, awaitItem())
            viewModel.startSession()
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.CoachSpeaking("Initial coach text"), awaitItem())

            viewModel.endSession()
            assertEquals(UiState.Idle, awaitItem())
        }
        coVerify { endSessionUseCase.endSession(initialQueues) }
    }

    @Test
    fun `startSession handles error and updates uiState to Error`() = runTest {
        val initialQueues = queuesFixture(
            newItems = mutableListOf(dummyItem("id1", "token1", 0, 0, false)),
            learnedItems = mutableListOf(dummyItem("id2", "token2", 0, 0, true))
        )
        val errorMessage = "Failed to load queues"
        coEvery { startSessionUseCase.startSession() } returns Result.failure(RuntimeException(errorMessage))

        viewModel.uiState.test {
            assertEquals(UiState.Idle, awaitItem())
            viewModel.startSession()
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Error(errorMessage), awaitItem())
        }
        coVerify { startSessionUseCase.startSession() }
    }

    @Test
    fun `processTurn handles error and updates uiState to Error`() = runTest {
        val initialQueues = queuesFixture(
            newItems = mutableListOf(dummyItem("id1", "token1", 0, 0, false)),
            learnedItems = mutableListOf(dummyItem("id2", "token2", 0, 0, true))
        )
        val errorMessage = "Failed to process turn"
        val userResponse = "some response"

        coEvery { startSessionUseCase.startSession() } returns Result.success(initialQueues)
        coEvery { generateDialogueUseCase.generatePrompt(initialQueues) } returns "Initial coach text"
        coEvery { processTurnUseCase.processTurn(initialQueues, userResponse) } returns Result.failure(RuntimeException(errorMessage))

        viewModel.uiState.test {
            assertEquals(UiState.Idle, awaitItem())
            viewModel.startSession()
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.CoachSpeaking("Initial coach text"), awaitItem())

            viewModel.processTurn(userResponse)
            assertEquals(UiState.Waiting, awaitItem())
            assertEquals(UiState.Error(errorMessage), awaitItem())
        }
        coVerify { processTurnUseCase.processTurn(initialQueues, userResponse) }
    }

    @Test
    fun `generateCoachDialogue handles error and updates uiState to Error`() = runTest {
        val initialQueues = queuesFixture(
            newItems = mutableListOf(dummyItem("id1", "token1", 0, 0, false)),
            learnedItems = mutableListOf(dummyItem("id2", "token2", 0, 0, true))
        )
        val errorMessage = "Failed to generate dialogue"

        coEvery { startSessionUseCase.startSession() } returns Result.success(initialQueues)
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