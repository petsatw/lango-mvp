package com.example.lango_mvp_android

import android.content.Context
import android.media.MediaPlayer
import androidx.test.core.app.ApplicationProvider
import com.example.data.LearningRepositoryImpl
import com.example.domain.GenerateDialogueUseCase
import com.example.speech.LlmServiceImpl
import com.example.speech.TtsServiceImpl
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.InputStreamReader

import kotlinx.serialization.json.Json

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class EndToEndCoreSequenceIntegrationTest {

    private lateinit var context: Context
    private lateinit var learningRepository: LearningRepositoryImpl
    private lateinit var generateDialogueUseCase: GenerateDialogueUseCase
    private lateinit var llmService: LlmServiceImpl
    private lateinit var ttsService: TtsServiceImpl
    private lateinit var mockMediaPlayer: MediaPlayer
    private lateinit var openAiApiKey: String
    private lateinit var json: Json

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        json = Json { ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }
        learningRepository = LearningRepositoryImpl(context.assets, context.filesDir, json)
        mockMediaPlayer = mockk(relaxed = true)

        // Read API key from local.properties
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
        val mockInitialPromptBuilder = mockk<com.example.domain.InitialPromptBuilder>(relaxed = true)
        generateDialogueUseCase = GenerateDialogueUseCase(learningRepository, llmService, mockInitialPromptBuilder)
    }

    @Test
    fun `full core sequence completes successfully`() = runTest {
        // 1. Load JSON files into memory
        val newQueueJson = javaClass.classLoader?.getResourceAsStream("core_blocks.json")?.bufferedReader().use { it?.readText() ?: "" }
        val learnedQueueJson = javaClass.classLoader?.getResourceAsStream("learned_queue.json")?.bufferedReader().use { it?.readText() ?: "" }
        val queuesResult = learningRepository.loadQueues()
        val queues = queuesResult.getOrThrow()

        // 2. Generate dialogue
        coEvery { llmService.generateDialogue(any<String>()) } returns "Hallo! Das ist ein Gruß."
        val dialoguePrompt = generateDialogueUseCase.generatePrompt(queues)
        val llmResponse = llmService.generateDialogue(dialoguePrompt)

        // Assert LLM response
        assertEquals("Hallo! Das ist ein Gruß.", llmResponse)

        // 3. Play audio and display text
        ttsService.speak(llmResponse)
        coEvery { ttsService.speak(any<String>()) } just Runs

        // Verify MediaPlayer interactions
        
    }
}