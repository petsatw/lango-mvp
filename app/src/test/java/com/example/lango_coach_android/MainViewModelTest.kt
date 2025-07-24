package com.example.lango_coach_android

import com.example.domain.Session

import io.mockk.mockk
import io.mockk.every
import io.mockk.verify
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.Runs
import io.mockk.just

import com.example.domain.CoachOrchestrator
import com.example.domain.GenerateDialogueUseCase
import com.example.domain.LearningItem
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

    private lateinit var coachOrchestrator: CoachOrchestrator
    private lateinit var generateDialogueUseCase: GenerateDialogueUseCase
    private lateinit var viewModel: MainViewModel
    private lateinit var initialSession: Session

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coachOrchestrator = mockk()
        generateDialogueUseCase = mockk()

        val initialQueues = queuesFixture(
            newItems = mutableListOf(dummyItem("id1", "token1", 0, 0, false)),
            learnedItems = mutableListOf(dummyItem("id2", "token2", 0, 0, true))
        )
        // Ensure newTarget is non-null for tests that rely on it
        initialSession = Session("sessionId", System.currentTimeMillis(), initialQueues, dummyItem("id1", "token1", 0, 0, false))

        viewModel = MainViewModel(
            coachOrchestrator,
            generateDialogueUseCase,
            testDispatcher
        )
    }

    @Test
    fun `startSession updates uiState to Loading then CoachSpeaking`() = runTest {
        coEvery { coachOrchestrator.startSession() } returns Result.success(initialSession)
        coEvery { generateDialogueUseCase.generatePrompt(initialSession.queues) } returns "Initial coach text"

        viewModel.uiState.test {
            assertEquals(UiState.Idle, awaitItem())
            viewModel.startSession()
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.CoachSpeaking("Initial coach text"), awaitItem())
        }
        coVerify { coachOrchestrator.startSession() }
        coVerify { generateDialogueUseCase.generatePrompt(initialSession.queues) }
    }

    @Test
    fun `processTurn updates uiState to Waiting then CoachSpeaking`() = runTest {
        val initialQueues = queuesFixture(newCount = 1, learnedCount = 1)
        // Ensure newTarget is non-null for this test
        val localInitialSession = Session("sessionId", System.currentTimeMillis(), initialQueues, dummyItem("id1", "token1", 0, 0, false))
        val updatedQueues = queuesFixture(newCount = 1, learnedCount = 1)
        val updatedSession = initialSession.copy(queues = updatedQueues, newTarget = updatedQueues.newQueue.firstOrNull() ?: initialSession.newTarget)
        val userResponse = "user says token1"
        val expectedCoachText = "Next coach response"

        coEvery { coachOrchestrator.startSession() } returns Result.success(initialSession)
        coEvery { generateDialogueUseCase.generatePrompt(initialSession.queues) } returns "Initial coach text"
        coEvery { coachOrchestrator.processTurn(userResponse) } returns Result.success(updatedSession)
        coEvery { generateDialogueUseCase.generatePrompt(updatedSession.queues) } returns expectedCoachText

        viewModel.uiState.test {
            assertEquals(UiState.Idle, awaitItem())
            viewModel.startSession()
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.CoachSpeaking("Initial coach text"), awaitItem())

            viewModel.processTurn(userResponse)
            assertEquals(UiState.Waiting, awaitItem())
            assertEquals(UiState.CoachSpeaking(expectedCoachText), awaitItem())
        }
        coVerify { coachOrchestrator.processTurn(userResponse) }
        coVerify { generateDialogueUseCase.generatePrompt(updatedSession.queues) }
    }

    @Test
    fun `processTurn handles mastery and updates uiState to Congrats`() = runTest {
        val initialQueues = queuesFixture(newCount = 1, learnedCount = 1)
        // Ensure newTarget is non-null for this test
        val localInitialSession = Session("sessionId", System.currentTimeMillis(), initialQueues, dummyItem("id1", "token1", 0, 0, false))
        val masteredItem = requireNotNull(localInitialSession.newTarget) {
            "newTarget should not be null in this test setup"
        }.copy(usageCount = 3, isLearned = true)
        val masteredQueues = queuesFixture(newItems = emptyList(), learnedItems = listOf(masteredItem))
        val masteredSession = localInitialSession.copy(queues = masteredQueues, newTarget = null) // newTarget is null when newQueue is empty
        val userResponse = "user says token1"

        coEvery { coachOrchestrator.startSession() } returns Result.success(localInitialSession)
        coEvery { generateDialogueUseCase.generatePrompt(localInitialSession.queues) } returns "Initial coach text"
        coEvery { coachOrchestrator.processTurn(userResponse) } returns Result.success(masteredSession)
        coEvery { coachOrchestrator.endSession(masteredQueues) } returns Result.success(Unit)

        viewModel.uiState.test {
            assertEquals(UiState.Idle, awaitItem())
            viewModel.startSession()
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.CoachSpeaking("Initial coach text"), awaitItem())

            viewModel.processTurn(userResponse)
            assertEquals(UiState.Waiting, awaitItem())
            assertEquals(UiState.Congrats, awaitItem())
        }
        coVerify { coachOrchestrator.processTurn(userResponse) }
    }

    @Test
    fun `endSession updates uiState to Idle`() = runTest {
        val initialQueues = queuesFixture(
            newItems = mutableListOf(dummyItem("id1", "token1", 0, 0, false)),
            learnedItems = mutableListOf(dummyItem("id2", "token2", 0, 0, true))
        )
        // Ensure newTarget is non-null for this test
        val localInitialSession = Session("sessionId", System.currentTimeMillis(), initialQueues, dummyItem("id1", "token1", 0, 0, false))

        coEvery { coachOrchestrator.startSession() } returns Result.success(initialSession)
        coEvery { generateDialogueUseCase.generatePrompt(initialSession.queues) } returns "Initial coach text"
        coEvery { coachOrchestrator.endSession(initialSession.queues) } returns Result.success(Unit)

        viewModel.uiState.test {
            assertEquals(UiState.Idle, awaitItem())
            viewModel.startSession()
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.CoachSpeaking("Initial coach text"), awaitItem())

            viewModel.endSession()
            assertEquals(UiState.Idle, awaitItem())
        }
        coVerify { coachOrchestrator.endSession(initialSession.queues) }
    }

    @Test
    fun `startSession handles error and updates uiState to Error`() = runTest {
        val errorMessage = "Failed to load queues"
        coEvery { coachOrchestrator.startSession() } returns Result.failure(RuntimeException(errorMessage))

        viewModel.uiState.test {
            assertEquals(UiState.Idle, awaitItem())
            viewModel.startSession()
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Error(errorMessage), awaitItem())
        }
        coVerify { coachOrchestrator.startSession() }
    }

    @Test
    fun `processTurn handles error and updates uiState to Error`() = runTest {
        val initialQueues = queuesFixture(
            newItems = mutableListOf(dummyItem("id1", "token1", 0, 0, false)),
            learnedItems = mutableListOf(dummyItem("id2", "token2", 0, 0, true))
        )
        // Ensure newTarget is non-null for this test
        val localInitialSession = Session("sessionId", System.currentTimeMillis(), initialQueues, dummyItem("id1", "token1", 0, 0, false))
        val errorMessage = "Failed to process turn"
        val userResponse = "some response"

        coEvery { coachOrchestrator.startSession() } returns Result.success(initialSession)
        coEvery { generateDialogueUseCase.generatePrompt(initialSession.queues) } returns "Initial coach text"
        coEvery { coachOrchestrator.processTurn(userResponse) } returns Result.failure(RuntimeException(errorMessage))

        viewModel.uiState.test {
            assertEquals(UiState.Idle, awaitItem())
            viewModel.startSession()
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.CoachSpeaking("Initial coach text"), awaitItem())

            viewModel.processTurn(userResponse)
            assertEquals(UiState.Waiting, awaitItem())
            assertEquals(UiState.Error(errorMessage), awaitItem())
        }
        coVerify { coachOrchestrator.processTurn(userResponse) }
    }

    @Test
    fun `generateCoachDialogue handles error and updates uiState to Error`() = runTest {
        val initialQueues = queuesFixture(
            newItems = mutableListOf(dummyItem("id1", "token1", 0, 0, false)),
            learnedItems = mutableListOf(dummyItem("id2", "token2", 0, 0, true))
        )
        // Ensure newTarget is non-null for this test
        val localInitialSession = Session("sessionId", System.currentTimeMillis(), initialQueues, dummyItem("id1", "token1", 0, 0, false))
        val errorMessage = "Failed to generate dialogue"

        coEvery { coachOrchestrator.startSession() } returns Result.success(initialSession)
        coEvery { generateDialogueUseCase.generatePrompt(initialSession.queues) } throws RuntimeException(errorMessage)

        viewModel.uiState.test {
            assertEquals(UiState.Idle, awaitItem())
            viewModel.startSession()
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Error(errorMessage), awaitItem())
        }
        coVerify { generateDialogueUseCase.generatePrompt(initialSession.queues) }
    }
}