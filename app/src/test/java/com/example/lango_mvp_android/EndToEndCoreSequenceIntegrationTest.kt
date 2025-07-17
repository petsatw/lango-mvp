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

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        learningRepository = LearningRepositoryImpl(context)
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

        llmService = LlmServiceImpl(openAiApiKey, MockEngine { request ->
            when (request.url.encodedPath) {
                "/v1/chat/completions" -> {
                    respond(
                        content = ByteReadChannel("{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Hallo! Das ist ein Gruß.\"}}]}"),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                "/v1/audio/speech" -> {
                    respond(
                        content = ByteReadChannel(javaClass.classLoader?.getResourceAsStream("sample_tts_audio.mp3")?.readBytes() ?: ByteArray(0)),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "audio/mpeg")
                    )
                }
                else -> error("Unhandled request ${request.url}")
            }
        })
        ttsService = TtsServiceImpl(openAiApiKey, MockEngine { request ->
            when (request.url.encodedPath) {
                "/v1/audio/speech" -> {
                    respond(
                        content = ByteReadChannel(javaClass.classLoader?.getResourceAsStream("sample_tts_audio.mp3")?.readBytes() ?: ByteArray(0)),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "audio/mpeg")
                    )
                }
                else -> error("Unhandled request ${request.url}")
            }
        }) { mockMediaPlayer }
        generateDialogueUseCase = GenerateDialogueUseCase(learningRepository, llmService)
    }

    @Test
    fun `full core sequence completes successfully`() = runTest {
        // 1. Load JSON files into memory
        val newQueueJson = javaClass.classLoader?.getResourceAsStream("core_blocks.json")?.bufferedReader().use { it?.readText() ?: "" }
        val learnedQueueJson = javaClass.classLoader?.getResourceAsStream("learned_queue.json")?.bufferedReader().use { it?.readText() ?: "" }
        val queues = learningRepository.loadQueues(newQueueJson, learnedQueueJson)

        // 2. Generate dialogue
        val dialoguePrompt = generateDialogueUseCase.generatePrompt(queues)
        val llmResponse = llmService.generateDialogue(dialoguePrompt)

        // Assert LLM response
        assertEquals("Hallo! Das ist ein Gruß.", llmResponse)

        // 3. Play audio and display text
        ttsService.speak(llmResponse)

        // Verify MediaPlayer interactions
        verify { mockMediaPlayer.setDataSource(any<String>()) }
        verify { mockMediaPlayer.prepare() }
        verify { mockMediaPlayer.start() }

        // Simulate completion to trigger release and file deletion
        val listener = slot<MediaPlayer.OnCompletionListener>()
        verify { mockMediaPlayer.setOnCompletionListener(capture(listener)) }
        listener.captured.onCompletion(mockMediaPlayer)
    }
}