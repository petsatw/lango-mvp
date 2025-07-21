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

        val localPropertiesFile = File("C:/Users/audoc/apps/lango-dev/lango-mvp-android/local.properties")
        val properties = java.util.Properties()
        if (localPropertiesFile.exists()) {
            InputStreamReader(localPropertiesFile.inputStream()).use { reader ->
                properties.load(reader)
            }
        }
        openAiApiKey = properties.getProperty("OPENAI_API_KEY") ?: throw IllegalStateException("OPENAI_API_KEY not found in local.properties")

        llmService = mockk(relaxed = true)
        ttsService = mockk(relaxed = true)

        startSessionUseCase = StartSessionUseCase(learningRepository)
        processTurnUseCase = ProcessTurnUseCase(learningRepository)
        generateDialogueUseCase = GenerateDialogueUseCase(learningRepository, llmService)
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
        val initialNewQueue = mutableListOf(
            LearningItem("german_CP001", "Entschuldigung", 0, 0),
            LearningItem("german_BB009", "Keine Ahnung", 0, 0),
            LearningItem("german_BB050", "Bis gleich", 0, 0)
        )
        val initialLearnedPool = mutableListOf(
            LearningItem("german_AA002", "sehr", 6, 4),
            LearningItem("german_AA003", "viel", 4, 7),
            LearningItem("german_AA005", "klein", 7, 9),
            LearningItem("german_AA007", "neu", 4, 5),
            LearningItem("german_AA008", "lang", 10, 10),
            LearningItem("german_AA010", "wenig", 6, 6),
            LearningItem("german_AA012", "spät", 6, 5),
            LearningItem("german_AA014", "morgen", 8, 9),
            LearningItem("german_AA013", "heute", 4, 6),
            LearningItem("german_CS014", "Kannst du ___?", 10, 6),
            LearningItem("german_CS013", "Wir müssen ___", 8, 7),
            LearningItem("german_CS015", "Ich möchte ___.", 8, 7),
            LearningItem("german_FW157", "schon", 8, 9),
            LearningItem("german_V002", "haben", 5, 4),
            LearningItem("german_V009", "kommen", 10, 10),
            LearningItem("german_V012", "gehen", 6, 6),
            LearningItem("german_V011", "wollen", 4, 6),
            LearningItem("german_V005", "müssen", 5, 5),
            LearningItem("german_V004", "können", 8, 9),
            LearningItem("german_FW083", "sein", 4, 7),
            LearningItem("german_V017", "finden", 9, 7),
            LearningItem("german_V026", "halten", 6, 5),
            LearningItem("german_V049", "beginnen", 5, 7),
            LearningItem("german_N001", "Mann", 6, 4),
            LearningItem("german_N003", "Kind", 7, 5),
            LearningItem("german_N002", "Frau", 6, 6),
            LearningItem("german_N004", "Haus", 7, 7),
            LearningItem("german_N005", "Stadt", 9, 9),
            LearningItem("german_N006", "Land", 10, 6),
            LearningItem("german_N008", "Hund", 7, 5),
            LearningItem("german_N011", "Baum", 5, 6),
            LearningItem("german_N017", "See", 7, 7),
            LearningItem("german_N018", "Straße", 5, 5),
            LearningItem("german_N029", "Schule", 4, 8),
            LearningItem("german_N040", "Papier", 7, 7),
            LearningItem("german_N045", "Bus", 4, 7),
            LearningItem("german_N050", "Milch", 7, 7),
            LearningItem("german_N049", "Brot", 10, 7),
            LearningItem("german_N051", "Wasser", 8, 10),
            LearningItem("german_N052", "Apfel", 4, 7),
            LearningItem("german_N055", "Fleisch", 10, 6),
            LearningItem("german_N057", "Suppe", 6, 4),
            LearningItem("german_N059", "Glas", 8, 6),
            LearningItem("german_N079", "Musik", 5, 5),
            LearningItem("german_N094", "Freund", 7, 10),
            LearningItem("german_N095", "Freundin", 10, 10),
            LearningItem("german_N093", "Mutter", 4, 8),
            LearningItem("german_N092", "Vater", 6, 9),
            LearningItem("german_FW001", "der", 10, 10),
            LearningItem("german_FW002", "die", 6, 4),
            LearningItem("german_FW003", "und", 5, 8),
            LearningItem("german_FW008", "das", 6, 7),
            LearningItem("german_FW009", "mit", 10, 5),
            LearningItem("german_FW013", "auf", 10, 5),
            LearningItem("german_FW017", "im", 5, 7),
            LearningItem("german_FW018", "eine", 7, 7),
            LearningItem("german_FW015", "ein", 7, 8),
            LearningItem("german_FW014", "nicht", 9, 7),
            LearningItem("german_FW024", "sie", 5, 9),
            LearningItem("german_FW027", "wir", 6, 9),
            LearningItem("german_FW031", "kein", 9, 8),
            LearningItem("german_FW036", "man", 9, 8),
            LearningItem("german_FW037", "oder", 8, 5),
            LearningItem("german_FW035", "da", 9, 6),
            LearningItem("german_FW034", "über", 6, 6),
            LearningItem("german_FW045", "wie", 7, 6),
            LearningItem("german_FW048", "ich", 9, 9),
            LearningItem("german_FW049", "du", 8, 5),
            LearningItem("german_FW052", "uns", 4, 9),
            LearningItem("german_FW055", "dich", 7, 8),
            LearningItem("german_FW069", "etwas", 10, 8),
            LearningItem("german_FW070", "nichts", 9, 8),
            LearningItem("german_FW071", "mein", 6, 5),
            LearningItem("german_FW072", "meine", 6, 10),
            LearningItem("german_FW073", "meinen", 9, 8),
            LearningItem("german_FW111", "einen", 6, 8),
            LearningItem("german_FW123", "diese", 10, 9),
            LearningItem("german_FW125", "diesen", 6, 6),
            LearningItem("german_FW158", "mal", 4, 4),
            LearningItem("german_CP001", "Entschuldigung", 5, 8),
            LearningItem("german_CP002", "Ich verstehe nicht", 2, 6),
            LearningItem("german_CP003", "Wie sagt man ___?", 9, 1),
            LearningItem("german_CP004", "Können Sie das bitte wiederholen?", 7, 3),
            LearningItem("german_CP005", "Was bedeutet ___?", 0, 10),
            LearningItem("german_CP006", "Sprechen Sie Englisch?", 4, 7),
            LearningItem("german_CP007", "Mein Deutsch ist nicht so gut", 6, 2),
            LearningItem("german_CP008", "Bitte", 1, 9),
            LearningItem("german_CP009", "Danke", 8, 0),
            LearningItem("german_CP008", "Bitte", 7, 5),
            LearningItem("german_CP009", "Danke", 8, 6),
            LearningItem("german_CP010", "Guten Morgen", 4, 9),
            LearningItem("german_CP011", "Guten Tag", 5, 7),
            LearningItem("german_CP012", "Gute Nacht", 6, 3),
            LearningItem("german_CP013", "Wie geht’s?", 9, 8),
            LearningItem("german_CP004", "Können Sie das bitte wiederholen?", 5, 8),
            LearningItem("german_CP005", "Was bedeutet ___?", 7, 4),
            LearningItem("german_CP014", "Mir geht’s gut, danke", 6, 9),
            LearningItem("german_CP015", "Und Ihnen?", 8, 3),
            LearningItem("german_CP016", "Wo ist ___?", 4, 7)
        )
        val initialQueues = Queues(initialNewQueue, initialLearnedPool)
        coEvery { learningRepository.loadQueues() } returns Result.success(initialQueues)

        // Mock repository to return our initial queues
        // Note: For a true integration test, you might want to use actual file operations
        // or a in-memory repository that you can manipulate directly.
        // For simplicity and control in this test, we'll mock the loadQueues behavior.
        
        coEvery { learningRepository.saveQueues(any()) } returns Result.success(Unit)

        
        


        val initialCoachText = "Coach: Entschuldigung. Das bedeutet 'sorry'. Zum Beispiel: Entschuldigung, ich bin spät."
        val secondCoachText = "Coach: Entschuldigung. Wie geht es dir?"
        val thirdCoachText = "Coach: Keine Ahnung. Das bedeutet 'no idea'. Zum Beispiel: Ich habe keine Ahnung."
        val fourthCoachText = "Coach: Keine Ahnung. Wie geht es dir?"
        val fifthCoachText = "Coach: Bis gleich. Das bedeutet 'see you soon'."
        val sixthCoachText = "Coach: Bis gleich. Wir sehen uns später."
        val congratsText = "Congratulations! You've completed your learning objectives."

        val promptSlot = slot<String>()

        coEvery { llmService.generateDialogue(capture(promptSlot)) } answers {
            when {
                promptSlot.captured.contains("Say 'Entschuldigung' by itself.") -> initialCoachText
                promptSlot.captured.contains("Generate natural German dialogue using only 'Entschuldigung'") -> secondCoachText
                promptSlot.captured.contains("Say 'Keine Ahnung' by itself.") -> thirdCoachText
                promptSlot.captured.contains("Generate natural German dialogue using only 'Keine Ahnung'") -> fourthCoachText
                promptSlot.captured.contains("Say 'Bis gleich' by itself.") -> fifthCoachText
                promptSlot.captured.contains("Generate natural German dialogue using only 'Bis gleich'") -> sixthCoachText
                promptSlot.captured == "Congratulations! You've completed your learning objectives." -> congratsText
                else -> throw IllegalArgumentException("Unexpected prompt: ${promptSlot.captured}")
            }
        }


        // Start the session
        mainViewModel.startSession()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(UiState.CoachSpeaking("Coach: Entschuldigung. Das bedeutet 'sorry'. Zum Beispiel: Entschuldigung, ich bin spät."), mainViewModel.uiState.value)

        // Simulate user response 1 (not mastered)
        mainViewModel.processTurn("Entschuldigung")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(UiState.CoachSpeaking("Coach: Entschuldigung. Wie geht es dir?"), mainViewModel.uiState.value)

        // Simulate user response 2 (not mastered)
        mainViewModel.processTurn("Entschuldigung")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(UiState.CoachSpeaking("Coach: Entschuldigung. Wie geht es dir?"), mainViewModel.uiState.value)

        // Simulate user response 3 (mastered)
        mainViewModel.processTurn("Entschuldigung")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(UiState.CoachSpeaking("Coach: Keine Ahnung. Das bedeutet 'no idea'. Zum Beispiel: Ich habe keine Ahnung."), mainViewModel.uiState.value)

        // Simulate user response 4 (not mastered)
        mainViewModel.processTurn("Keine Ahnung")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(UiState.CoachSpeaking("Coach: Keine Ahnung. Wie geht es dir?"), mainViewModel.uiState.value)

        // Simulate user response 5 (not mastered)
        mainViewModel.processTurn("Keine Ahnung")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(UiState.CoachSpeaking("Coach: Keine Ahnung. Wie geht es dir?"), mainViewModel.uiState.value)

        // Simulate user response 6 (mastered)
        mainViewModel.processTurn("Keine Ahnung")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(UiState.CoachSpeaking("Coach: Bis gleich. Das bedeutet 'see you soon'."), mainViewModel.uiState.value)

        // Simulate user response 7 (not mastered)
        mainViewModel.processTurn("Bis gleich")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(UiState.CoachSpeaking("Coach: Bis gleich. Wir sehen uns später."), mainViewModel.uiState.value)

        // Simulate user response 8 (not mastered)
        mainViewModel.processTurn("Bis gleich")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(UiState.CoachSpeaking("Coach: Bis gleich. Wir sehen uns später."), mainViewModel.uiState.value)

        // Simulate user response 9 (mastered)
        mainViewModel.processTurn("Bis gleich")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(UiState.Congrats, mainViewModel.uiState.value)

        // Verify that saveQueues was called at the end
        coVerify { learningRepository.saveQueues(any()) }
    }
}
