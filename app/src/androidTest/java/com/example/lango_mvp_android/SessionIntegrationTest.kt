package com.example.lango_mvp_android

import com.example.lango_coach_android.MainViewModel
import com.example.lango_coach_android.UiState

import android.content.Context
import android.media.MediaPlayer
import androidx.test.core.app.ApplicationProvider
import com.example.data.LearningRepositoryImpl
import com.example.domain.LearningRepository
import com.example.domain.EndSessionUseCase
import com.example.domain.GenerateDialogueUseCase
import com.example.domain.LearningItem
import com.example.domain.ProcessTurnUseCase
import com.example.domain.Queues
import com.example.domain.StartSessionUseCase
import com.example.speech.LlmServiceImpl
import com.example.speech.TtsServiceImpl
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.InputStreamReader
import com.example.testing.TestFixtures
import javax.inject.Inject
import kotlin.text.RegexOption

@ExperimentalCoroutinesApi
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class SessionIntegrationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var learningRepository: LearningRepository
    @Inject
    lateinit var startSessionUseCase: StartSessionUseCase
    @Inject
    lateinit var processTurnUseCase: ProcessTurnUseCase
    @Inject
    lateinit var generateDialogueUseCase: GenerateDialogueUseCase
    @Inject
    lateinit var endSessionUseCase: EndSessionUseCase
    @Inject
    lateinit var llmService: LlmService
    @Inject
    lateinit var ttsService: TtsService

    private lateinit var mainViewModel: MainViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        hiltRule.inject()
        mainViewModel = MainViewModel(
            startSessionUseCase,
            processTurnUseCase,
            generateDialogueUseCase,
            endSessionUseCase,
            testDispatcher
        )
    }

    @After
    fun tearDown() {
    }

    @Test
    fun `full session with mastery completes successfully`() = runTest {
        // Prepare initial queues for the test
        val initialQueues = TestFixtures.queuesFixture(newCount = 3, learnedCount = 99)
        coEvery { learningRepository.loadQueues() } returns Result.success(initialQueues)

        // Mock repository to return our initial queues
        // Note: For a true integration test, you might want to use actual file operations
        // or a in-memory repository that you can manipulate directly.
        // For simplicity and control in this test, we'll mock the loadQueues behavior.

        coEvery { learningRepository.saveQueues(any()) } returns Result.success(Unit)


        val expectedLlmResponses = mutableListOf(
            "Coach: Keine Ahnung. Das bedeutet 'no idea'.", // 0: Initial for BB009
            "Coach: Keine Ahnung. Wie geht es dir?", // 1: Turn 1 for BB009
            "Coach: Keine Ahnung. Was bedeutet das?", // 2: Turn 2 for BB009
            "Coach: Bis gleich. Das bedeutet 'see you soon'.", // 3: Initial for BB050 (after BB009 mastered)
            "Coach: Bis gleich. Wir sehen uns später.", // 4: Turn 1 for BB050
            "Coach: Bis gleich. Auf Wiedersehen.", // 5: Turn 2 for BB050
            "Congratulations! You've completed your learning objectives." // 6: Final (after BB050 mastered)
        )

        var responseIndex = 0
        val promptSlot = slot<String>()

        coEvery { llmService.generateDialogue(capture(promptSlot)) } answers {
            val capturedPrompt = promptSlot.captured
            val response = expectedLlmResponses[responseIndex]
            responseIndex++
            response
        }

        // Start the session
        mainViewModel.startSession()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(UiState.CoachSpeaking(expectedLlmResponses[0]), mainViewModel.uiState.value)

        // Simulate user response 1 (not mastered) - for "Keine Ahnung"
        mainViewModel.processTurn("Keine Ahnung")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(
            UiState.CoachSpeaking(expectedLlmResponses[1]),
            mainViewModel.uiState.value
        )

        // Simulate user response 2 (not mastered) - for "Keine Ahnung"
        mainViewModel.processTurn("Keine Ahnung")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(
            UiState.CoachSpeaking(expectedLlmResponses[2]),
            mainViewModel.uiState.value
        )

        // Simulate user response 3 (mastered) - for "Keine Ahnung"
        mainViewModel.processTurn("Keine Ahnung")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(
            UiState.CoachSpeaking(expectedLlmResponses[3]), // This is the initial prompt for "Bis gleich"
            mainViewModel.uiState.value
        )

        // Simulate user response 4 (not mastered) - for "Bis gleich"
        mainViewModel.processTurn("Bis gleich")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(
            UiState.CoachSpeaking(expectedLlmResponses[4]),
            mainViewModel.uiState.value
        )

        // Simulate user response 5 (not mastered) - for "Bis gleich"
        mainViewModel.processTurn("Bis gleich")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(
            UiState.CoachSpeaking(expectedLlmResponses[5]),
            mainViewModel.uiState.value
        )

        // Simulate user response 6 (mastered) - for "Bis gleich"
        mainViewModel.processTurn("Bis gleich")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(UiState.Congrats, mainViewModel.uiState.value)

        // Verify that saveQueues was called at the end
        coVerify { learningRepository.saveQueues(any()) }
    }
}