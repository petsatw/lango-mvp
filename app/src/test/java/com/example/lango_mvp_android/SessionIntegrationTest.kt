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
            LearningItem("german_CP001", "Entschuldigung", "Blocks", "fixed", 0, 0, false),
            LearningItem("german_BB009", "Keine Ahnung", "Blocks", "fixed", 0, 0, false),
            LearningItem("german_BB050", "Bis gleich", "Blocks", "fixed", 0, 0, false)
        )
        val initialLearnedPool = mutableListOf(
            LearningItem("german_AA002", "sehr", "Adjectives/Adverbs", "adverb", 6, 4, true),
            LearningItem("german_AA003", "viel", "Adjectives/Adverbs", "adjective", 4, 7, true),
            LearningItem("german_AA005", "klein", "Adjectives/Adverbs", "adjective", 7, 9, true),
            LearningItem("german_AA007", "neu", "Adjectives/Adverbs", "adjective", 4, 5, true),
            LearningItem("german_AA008", "lang", "Adjectives/Adverbs", "adjective", 10, 10, true),
            LearningItem("german_AA010", "wenig", "Adjectives/Adverbs", "adjective", 6, 6, true),
            LearningItem("german_AA012", "spät", "Adjectives/Adverbs", "adverb", 6, 5, true),
            LearningItem("german_AA014", "morgen", "Adjectives/Adverbs", "adverb", 8, 9, true),
            LearningItem("german_AA013", "heute", "Adjectives/Adverbs", "adverb", 4, 6, true),
            LearningItem("german_CS014", "Kannst du ___?", "Core Stems", "parametric", 10, 6, true),
            LearningItem("german_CS013", "Wir müssen ___", "Core Stems", "parametric", 8, 7, true),
            LearningItem("german_CS015", "Ich möchte ___.", "Core Stems", "parametric", 8, 7, true),
            LearningItem("german_FW157", "schon", "Function Words", "particle", 8, 9, true),
            LearningItem("german_V002", "haben", "Verbs", "auxiliary", 5, 4, true),
            LearningItem("german_V009", "kommen", "Verbs", "action", 10, 10, true),
            LearningItem("german_V012", "gehen", "Verbs", "action", 6, 6, true),
            LearningItem("german_V011", "wollen", "Verbs", "auxiliary", 4, 6, true),
            LearningItem("german_V005", "müssen", "Verbs", "auxiliary", 5, 5, true),
            LearningItem("german_V004", "können", "Verbs", "auxiliary", 8, 9, true),
            LearningItem("german_FW083", "sein", "Function Words", "pronoun", 4, 7, true),
            LearningItem("german_V017", "finden", "Verbs", "action", 9, 7, true),
            LearningItem("german_V026", "halten", "Verbs", "action", 6, 5, true),
            LearningItem("german_V049", "beginnen", "Verbs", "action", 5, 7, true),
            LearningItem("german_N001", "Mann", "Nouns", "masculine", 6, 4, true),
            LearningItem("german_N003", "Kind", "Nouns", "neuter", 7, 5, true),
            LearningItem("german_N002", "Frau", "Nouns", "feminine", 6, 6, true),
            LearningItem("german_N004", "Haus", "Nouns", "neuter", 7, 7, true),
            LearningItem("german_N005", "Stadt", "Nouns", "feminine", 9, 9, true),
            LearningItem("german_N006", "Land", "Nouns", "neuter", 10, 6, true),
            LearningItem("german_N008", "Hund", "Nouns", "masculine", 7, 5, true),
            LearningItem("german_N011", "Baum", "Nouns", "masculine", 5, 6, true),
            LearningItem("german_N017", "See", "Nouns", "masculine", 7, 7, true),
            LearningItem("german_N018", "Straße", "Nouns", "feminine", 5, 5, true),
            LearningItem("german_N029", "Schule", "Nouns", "feminine", 4, 8, true),
            LearningItem("german_N040", "Papier", "Nouns", "neuter", 7, 7, true),
            LearningItem("german_N045", "Bus", "Nouns", "masculine", 4, 7, true),
            LearningItem("german_N050", "Milch", "Nouns", "feminine", 7, 7, true),
            LearningItem("german_N049", "Brot", "Nouns", "neuter", 10, 7, true),
            LearningItem("german_N051", "Wasser", "Nouns", "neuter", 8, 10, true),
            LearningItem("german_N052", "Apfel", "Nouns", "masculine", 4, 7, true),
            LearningItem("german_N055", "Fleisch", "Nouns", "neuter", 10, 6, true),
            LearningItem("german_N057", "Suppe", "Nouns", "feminine", 6, 4, true),
            LearningItem("german_N059", "Glas", "Nouns", "neuter", 8, 6, true),
            LearningItem("german_N079", "Musik", "Nouns", "feminine", 5, 5, true),
            LearningItem("german_N094", "Freund", "Nouns", "masculine", 7, 10, true),
            LearningItem("german_N095", "Freundin", "Nouns", "feminine", 10, 10, true),
            LearningItem("german_N093", "Mutter", "Nouns", "feminine", 4, 8, true),
            LearningItem("german_N092", "Vater", "Nouns", "masculine", 6, 9, true),
            LearningItem("german_FW001", "der", "Function Words", "article", 10, 10, true),
            LearningItem("german_FW002", "die", "Function Words", "article", 6, 4, true),
            LearningItem("german_FW003", "und", "Function Words", "conjunction", 5, 8, true),
            LearningItem("german_FW008", "das", "Function Words", "article", 6, 7, true),
            LearningItem("german_FW009", "mit", "Function Words", "preposition", 10, 5, true),
            LearningItem("german_FW013", "auf", "Function Words", "preposition", 10, 5, true),
            LearningItem("german_FW017", "im", "Function Words", "preposition", 5, 7, true),
            LearningItem("german_FW018", "eine", "Function Words", "article", 7, 7, true),
            LearningItem("german_FW015", "ein", "Function Words", "article", 7, 8, true),
            LearningItem("german_FW014", "nicht", "Function Words", "particle", 9, 7, true),
            LearningItem("german_FW024", "sie", "Function Words", "pronoun", 5, 9, true),
            LearningItem("german_FW027", "wir", "Function Words", "pronoun", 6, 9, true),
            LearningItem("german_FW031", "kein", "Function Words", "article", 9, 8, true),
            LearningItem("german_FW036", "man", "Function Words", "pronoun", 9, 8, true),
            LearningItem("german_FW037", "oder", "Function Words", "conjunction", 8, 5, true),
            LearningItem("german_FW035", "da", "Function Words", "particle", 9, 6, true),
            LearningItem("german_FW034", "über", "Function Words", "preposition", 6, 6, true),
            LearningItem("german_FW045", "wie", "Function Words", "conjunction", 7, 6, true),
            LearningItem("german_FW048", "ich", "Function Words", "pronoun", 9, 9, true),
            LearningItem("german_FW049", "du", "Function Words", "pronoun", 8, 5, true),
            LearningItem("german_FW052", "uns", "Function Words", "pronoun", 4, 9, true),
            LearningItem("german_FW055", "dich", "Function Words", "pronoun", 7, 8, true),
            LearningItem("german_FW069", "etwas", "Function Words", "pronoun", 10, 8, true),
            LearningItem("german_FW070", "nichts", "Function Words", "pronoun", 9, 8, true),
            LearningItem("german_FW071", "mein", "Function Words", "pronoun", 6, 5, true),
            LearningItem("german_FW072", "meine", "Function Words", "pronoun", 6, 10, true),
            LearningItem("german_FW073", "meinen", "Function Words", "pronoun", 9, 8, true),
            LearningItem("german_FW111", "einen", "Function Words", "article", 6, 8, true),
            LearningItem("german_FW123", "diese", "Function Words", "pronoun", 10, 9, true),
            LearningItem("german_FW125", "diesen", "Function Words", "pronoun", 6, 6, true),
            LearningItem("german_FW158", "mal", "Function Words", "particle", 4, 4, true),
            LearningItem("german_CP001", "Entschuldigung", "Blocks", "fixed", 5, 8, true),
            LearningItem("german_CP002", "Ich verstehe nicht", "Blocks", "fixed", 2, 6, true),
            LearningItem("german_CP003", "Wie sagt man ___?", "Blocks", "parametric", 9, 1, true),
            LearningItem("german_CP004", "Können Sie das bitte wiederholen?", "Blocks", "fixed", 7, 3, true),
            LearningItem("german_CP005", "Was bedeutet ___?", "Blocks", "parametric", 0, 10, true),
            LearningItem("german_CP006", "Sprechen Sie Englisch?", "Blocks", "fixed", 4, 7, true),
            LearningItem("german_CP007", "Mein Deutsch ist nicht so gut", "Blocks", "fixed", 6, 2, true),
            LearningItem("german_CP008", "Bitte", "Blocks", "fixed", 1, 9, true),
            LearningItem("german_CP009", "Danke", "Blocks", "fixed", 8, 0, true),
            LearningItem("german_CP008", "Bitte", "Blocks", "fixed", 7, 5, true),
            LearningItem("german_CP009", "Danke", "Blocks", "fixed", 8, 6, true),
            LearningItem("german_CP010", "Guten Morgen", "Blocks", "fixed", 4, 9, true),
            LearningItem("german_CP011", "Guten Tag", "Blocks", "fixed", 5, 7, true),
            LearningItem("german_CP012", "Gute Nacht", "Blocks", "fixed", 6, 3, true),
            LearningItem("german_CP013", "Wie geht’s?", "Blocks", "fixed", 9, 8, true),
            LearningItem("german_CP004", "Können Sie das bitte wiederholen?", "Blocks", "fixed", 5, 8, true),
            LearningItem("german_CP005", "Was bedeutet ___?", "Blocks", "parametric", 7, 4, true),
            LearningItem("german_CP014", "Mir geht’s gut, danke", "Blocks", "fixed", 6, 9, true),
            LearningItem("german_CP015", "Und Ihnen?", "Blocks", "fixed", 8, 3, true),
            LearningItem("german_CP016", "Wo ist ___?", "Blocks", "parametric", 4, 7, true)
        )
        val initialQueues = Queues(initialNewQueue, initialLearnedPool)
        coEvery { learningRepository.loadQueues(any()) } returns initialQueues

        // Mock repository to return our initial queues
        // Note: For a true integration test, you might want to use actual file operations
        // or a in-memory repository that you can manipulate directly.
        // For simplicity and control in this test, we'll mock the loadQueues behavior.
        
        coEvery { learningRepository.saveQueues(any()) } answers { /* do nothing or verify state */ }

        
        


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
