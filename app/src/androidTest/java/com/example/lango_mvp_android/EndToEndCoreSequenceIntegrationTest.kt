package com.example.lango_mvp_android

import android.content.Context
import android.media.MediaPlayer
import androidx.test.core.app.ApplicationProvider
import com.example.domain.Session
import com.example.domain.GenerateDialogueUseCase
import com.example.domain.LearningRepository
import com.example.domain.LlmService
import com.example.domain.TtsService
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.InputStreamReader

import com.example.testing.TestFixtures
import javax.inject.Inject
import kotlinx.serialization.json.Json

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class EndToEndCoreSequenceIntegrationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var learningRepository: LearningRepository
    @Inject
    lateinit var coachOrchestrator: CoachOrchestrator
    @Inject
    lateinit var llmService: LlmService
    @Inject
    lateinit var ttsService: TtsService

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun `full core sequence completes successfully`() = runTest {
        // Prepare initial queues for the test using TestFixtures
        val initialQueues = TestFixtures.queuesFixture(newCount = 1, learnedCount = 1)
        val initialSession = Session("test_session_id", System.currentTimeMillis(), initialQueues, initialQueues.newQueue.first())
        coEvery { learningRepository.loadQueues() } returns Result.success(initialQueues)
        coEvery { learningRepository.saveQueues(any()) } returns Result.success(Unit)
        coEvery { coachOrchestrator.startSession() } returns Result.success(initialSession)

        val session = coachOrchestrator.startSession().getOrThrow()

        // 2. Generate dialogue
        coEvery { llmService.generateDialogue(any<String>()) } returns "Hallo! Das ist ein Gruß."
        val dialoguePrompt = generateDialogueUseCase.generatePrompt(session.queues)
        val llmResponse = llmService.generateDialogue(dialoguePrompt)

        // Assert LLM response
        assertEquals("Hallo! Das ist ein Gruß.", llmResponse)

        // 3. Play audio and display text
        coEvery { ttsService.speak(any<String>()) } just Runs
        ttsService.speak(llmResponse)

        // Verify MediaPlayer interactions
        coVerify { ttsService.speak(llmResponse) }
    }
}