package com.example.lango_coach_android

import io.mockk.mockk
import io.mockk.every
import io.mockk.verify
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
    fun `startSession updates queues StateFlow`() = runTest {
        val expectedQueues = Queues(
            newQueue = mutableListOf(LearningItem("id1", "token1", "cat1", "sub1", 0, 0, false)),
            learnedPool = mutableListOf(LearningItem("id2", "token2", "cat2", "sub2", 0, 0, true))
        )
        every { startSessionUseCase.startSession() } returns expectedQueues

        viewModel.queues.test {
            assertEquals(null, awaitItem())
            viewModel.startSession()
            assertEquals(expectedQueues, awaitItem())
        }
        verify { startSessionUseCase.startSession() }
    }

    @Test
    fun `processTurn updates queues StateFlow and saves queues`() = runTest {
        val initialQueues = Queues(
            newQueue = mutableListOf(LearningItem("id1", "token1", "cat1", "sub1", 0, 0, false)),
            learnedPool = mutableListOf(LearningItem("id2", "token2", "cat2", "sub2", 0, 0, true))
        )
        val updatedQueues = Queues(
            newQueue = mutableListOf(),
            learnedPool = mutableListOf(LearningItem("id1", "token1", "cat1", "sub1", 0, 0, false), LearningItem("id2", "token2", "cat2", "sub2", 0, 0, true))
        )
        val presentedItem = LearningItem("id1", "token1", "cat1", "sub1", 0, 0, false)

        every { startSessionUseCase.startSession() } returns initialQueues
        every { processTurnUseCase.processTurn(initialQueues, presentedItem, true) } returns updatedQueues

        viewModel.queues.test {
            assertEquals(null, awaitItem())
            viewModel.startSession()
            assertEquals(initialQueues, awaitItem())

            viewModel.processTurn(presentedItem, true)
            assertEquals(updatedQueues, awaitItem())
        }
        verify { processTurnUseCase.processTurn(initialQueues, presentedItem, true) }
    }

    @Test
    fun `generateDialogue returns prompt from use case`() = runTest {
        val queues = Queues(
            newQueue = mutableListOf(LearningItem("id1", "token1", "cat1", "sub1", 0, 0, false)),
            learnedPool = mutableListOf(LearningItem("id2", "token2", "cat2", "sub2", 0, 0, true))
        )
        val expectedPrompt = "Test Prompt"

        every { startSessionUseCase.startSession() } returns queues
        every { generateDialogueUseCase.generatePrompt(queues) } returns expectedPrompt

        viewModel.startSession()
        val result = viewModel.generateDialogue()

        assertEquals(expectedPrompt, result)
        verify { generateDialogueUseCase.generatePrompt(queues) }
    }

    @Test
    fun `endSession clears queues StateFlow and saves queues`() = runTest {
        val initialQueues = Queues(
            newQueue = mutableListOf(LearningItem("id1", "token1", "cat1", "sub1", 0, 0, false)),
            learnedPool = mutableListOf(LearningItem("id2", "token2", "cat2", "sub2", 0, 0, true))
        )

        every { startSessionUseCase.startSession() } returns initialQueues

        viewModel.queues.test {
            assertEquals(null, awaitItem())
            viewModel.startSession()
            assertEquals(initialQueues, awaitItem())

            viewModel.endSession()
            assertEquals(null, awaitItem())
        }
        
        verify { endSessionUseCase.endSession(initialQueues) }
    }
}