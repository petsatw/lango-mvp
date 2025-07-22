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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.InputStreamReader
import com.example.testing.TestFixtures
import kotlin.text.RegexOption

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class SessionIntegrationTest {

    private lateinit var context: Context
    private lateinit var learningRepository: LearningRepository
    private lateinit var startSessionUseCase: StartSessionUseCase
    private lateinit var processTurnUseCase: ProcessTurnUseCase
    private lateinit var generateDialogueUseCase: GenerateDialogueUseCase
    private lateinit var endSessionUseCase: EndSessionUseCase
    private lateinit var llmService: LlmServiceImpl
    private lateinit var ttsService: TtsServiceImpl
    private lateinit var mockMediaPlayer: MediaPlayer
    private lateinit var mainViewModel: MainViewModel
    private lateinit var openAiApiKey: String

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        learningRepository = mockk()
        mockMediaPlayer = mockk(relaxed = true)

        val localPropertiesFile =
            File("C:/Users/audoc/apps/lango-dev/lango-mvp-android/local.properties")
        val properties = java.util.Properties()
        if (localPropertiesFile.exists()) {
            InputStreamReader(localPropertiesFile.inputStream()).use { reader ->
                properties.load(reader)
            }
        }
        openAiApiKey = properties.getProperty("OPENAI_API_KEY")
            ?: throw IllegalStateException("OPENAI_API_KEY not found in local.properties")

        llmService = mockk(relaxed = true)
        ttsService = mockk(relaxed = true)
        val mockInitialPromptBuilder =
            mockk<com.example.domain.InitialPromptBuilder>(relaxed = true)

        startSessionUseCase = StartSessionUseCase(learningRepository)
        processTurnUseCase = ProcessTurnUseCase(learningRepository)
        generateDialogueUseCase =
            GenerateDialogueUseCase(learningRepository, llmService, mockInitialPromptBuilder)
        endSessionUseCase = EndSessionUseCase(learningRepository)

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
        val initialQueues = TestFixtures.queuesFixture(
            new = mutableListOf(
                TestFixtures.dummyItem("german_CP001", "Entschuldigung", 0, 0),
                TestFixtures.dummyItem("german_BB009", "Keine Ahnung", 0, 0),
                TestFixtures.dummyItem("german_BB050", "Bis gleich", 0, 0)
            ),
            learned = mutableListOf(
                TestFixtures.dummyItem("german_AA002", "sehr", 6, 4),
                TestFixtures.dummyItem("german_AA003", "viel", 4, 7),
                TestFixtures.dummyItem("german_AA005", "klein", 7, 9),
                TestFixtures.dummyItem("german_AA007", "neu", 4, 5),
                TestFixtures.dummyItem("german_AA008", "lang", 10, 10),
                TestFixtures.dummyItem("german_AA010", "wenig", 6, 6),
                TestFixtures.dummyItem("german_AA012", "spät", 6, 5),
                TestFixtures.dummyItem("german_AA014", "morgen", 8, 9),
                TestFixtures.dummyItem("german_AA013", "heute", 4, 6),
                TestFixtures.dummyItem("german_CS014", "Kannst du ___?", 10, 6),
                TestFixtures.dummyItem("german_CS013", "Wir müssen ___", 8, 7),
                TestFixtures.dummyItem("german_CS015", "Ich möchte ___.", 8, 7),
                TestFixtures.dummyItem("german_FW157", "schon", 8, 9),
                TestFixtures.dummyItem("german_V002", "haben", 5, 4),
                TestFixtures.dummyItem("german_V009", "kommen", 10, 10),
                TestFixtures.dummyItem("german_V012", "gehen", 6, 6),
                TestFixtures.dummyItem("german_V011", "wollen", 4, 6),
                TestFixtures.dummyItem("german_V005", "müssen", 5, 5),
                TestFixtures.dummyItem("german_V004", "können", 8, 9),
                TestFixtures.dummyItem("german_FW083", "sein", 4, 7),
                TestFixtures.dummyItem("german_V017", "finden", 9, 7),
                TestFixtures.dummyItem("german_V026", "halten", 6, 5),
                TestFixtures.dummyItem("german_V049", "beginnen", 5, 7),
                TestFixtures.dummyItem("german_N001", "Mann", 6, 4),
                TestFixtures.dummyItem("german_N003", "Kind", 7, 5),
                TestFixtures.dummyItem("german_N002", "Frau", 6, 6),
                TestFixtures.dummyItem("german_N004", "Haus", 7, 7),
                TestFixtures.dummyItem("german_N005", "Stadt", 9, 9),
                TestFixtures.dummyItem("german_N006", "Land", 10, 6),
                TestFixtures.dummyItem("german_N008", "Hund", 7, 5),
                TestFixtures.dummyItem("german_N011", "Baum", 5, 6),
                TestFixtures.dummyItem("german_N017", "See", 7, 7),
                TestFixtures.dummyItem("german_N018", "Straße", 5, 5),
                TestFixtures.dummyItem("german_N029", "Schule", 4, 8),
                TestFixtures.dummyItem("german_N040", "Papier", 7, 7),
                TestFixtures.dummyItem("german_N045", "Bus", 4, 7),
                TestFixtures.dummyItem("german_N050", "Milch", 7, 7),
                TestFixtures.dummyItem("german_N049", "Brot", 10, 7),
                TestFixtures.dummyItem("german_N051", "Wasser", 8, 10),
                TestFixtures.dummyItem("german_N052", "Apfel", 4, 7),
                TestFixtures.dummyItem("german_N055", "Fleisch", 10, 6),
                TestFixtures.dummyItem("german_N057", "Suppe", 6, 4),
                TestFixtures.dummyItem("german_N059", "Glas", 8, 6),
                TestFixtures.dummyItem("german_N079", "Musik", 5, 5),
                TestFixtures.dummyItem("german_N094", "Freund", 7, 10),
                TestFixtures.dummyItem("german_N095", "Freundin", 10, 10),
                TestFixtures.dummyItem("german_N093", "Mutter", 4, 8),
                TestFixtures.dummyItem("german_N092", "Vater", 6, 9),
                TestFixtures.dummyItem("german_FW001", "der", 10, 10),
                TestFixtures.dummyItem("german_FW002", "die", 6, 4),
                TestFixtures.dummyItem("german_FW003", "und", 5, 8),
                TestFixtures.dummyItem("german_FW008", "das", 6, 7),
                TestFixtures.dummyItem("german_FW009", "mit", 10, 5),
                TestFixtures.dummyItem("german_FW013", "auf", 10, 5),
                TestFixtures.dummyItem("german_FW017", "im", 5, 7),
                TestFixtures.dummyItem("german_FW018", "eine", 7, 7),
                TestFixtures.dummyItem("german_FW015", "ein", 7, 8),
                TestFixtures.dummyItem("german_FW014", "nicht", 9, 7),
                TestFixtures.dummyItem("german_FW024", "sie", 5, 9),
                TestFixtures.dummyItem("german_FW027", "wir", 6, 9),
                TestFixtures.dummyItem("german_FW031", "kein", 9, 8),
                TestFixtures.dummyItem("german_FW036", "man", 9, 8),
                TestFixtures.dummyItem("german_FW037", "oder", 8, 5),
                TestFixtures.dummyItem("german_FW035", "da", 9, 6),
                TestFixtures.dummyItem("german_FW034", "über", 6, 6),
                TestFixtures.dummyItem("german_FW045", "wie", 7, 6),
                TestFixtures.dummyItem("german_FW048", "ich", 9, 9),
                TestFixtures.dummyItem("german_FW049", "du", 8, 5),
                TestFixtures.dummyItem("german_FW052", "uns", 4, 9),
                TestFixtures.dummyItem("german_FW055", "dich", 7, 8),
                TestFixtures.dummyItem("german_FW069", "etwas", 10, 8),
                TestFixtures.dummyItem("german_FW070", "nichts", 9, 8),
                TestFixtures.dummyItem("german_FW071", "mein", 6, 5),
                TestFixtures.dummyItem("german_FW072", "meine", 6, 10),
                TestFixtures.dummyItem("german_FW073", "meinen", 9, 8),
                TestFixtures.dummyItem("german_FW111", "einen", 6, 8),
                TestFixtures.dummyItem("german_FW123", "diese", 10, 9),
                TestFixtures.dummyItem("german_FW125", "diesen", 6, 6),
                TestFixtures.dummyItem("german_FW158", "mal", 4, 4),
                TestFixtures.dummyItem("german_CP001", "Entschuldigung", 5, 8),
                TestFixtures.dummyItem("german_CP002", "Ich verstehe nicht", 2, 6),
                TestFixtures.dummyItem("german_CP003", "Wie sagt man ___?", 9, 1),
                TestFixtures.dummyItem("german_CP004", "Können Sie das bitte wiederholen?", 7, 3),
                TestFixtures.dummyItem("german_CP005", "Was bedeutet ___?", 0, 10),
                TestFixtures.dummyItem("german_CP006", "Sprechen Sie Englisch?", 4, 7),
                TestFixtures.dummyItem("german_CP007", "Mein Deutsch ist nicht so gut", 6, 2),
                TestFixtures.dummyItem("german_CP008", "Bitte", 1, 9),
                TestFixtures.dummyItem("german_CP009", "Danke", 8, 0),
                TestFixtures.dummyItem("german_CP008", "Bitte", 7, 5),
                TestFixtures.dummyItem("german_CP009", "Danke", 8, 6),
                TestFixtures.dummyItem("german_CP010", "Guten Morgen", 4, 9),
                TestFixtures.dummyItem("german_CP011", "Guten Tag", 5, 7),
                TestFixtures.dummyItem("german_CP012", "Gute Nacht", 6, 3),
                TestFixtures.dummyItem("german_CP013", "Wie geht’s?", 9, 8),
                TestFixtures.dummyItem("german_CP004", "Können Sie das bitte wiederholen?", 5, 8),
                TestFixtures.dummyItem("german_CP005", "Was bedeutet ___?", 7, 4),
                TestFixtures.dummyItem("german_CP014", "Mir geht’s gut, danke", 6, 9),
                TestFixtures.dummyItem("german_CP015", "Und Ihnen?", 8, 3),
                TestFixtures.dummyItem("german_CP016", "Wo ist ___?", 4, 7)
            )
        )
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
