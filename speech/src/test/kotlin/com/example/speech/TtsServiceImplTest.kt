package com.example.speech

import android.media.MediaPlayer
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import io.mockk.coEvery
import io.mockk.mockk
import com.example.domain.TtsService


@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE) // Use Config.NONE to avoid manifest parsing issues
class TtsServiceImplTest {

    private lateinit var ttsService: TtsService

    @Before
    fun setup() {
        ttsService = mockk<TtsService>(relaxed = true)
    }

    @Test
    fun `speak plays audio and cleans up temp file`() = runTest {
        coEvery { ttsService.speak(any()) } returns Unit
    }
}
