import android.media.MediaPlayer
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import io.mockk.mockk
import io.mockk.verify
import io.mockk.slot
import com.example.speech.TtsServiceImpl

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE) // Use Config.NONE to avoid manifest parsing issues
class TtsServiceImplTest {

    @Test
    fun `speak plays audio and cleans up temp file`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(javaClass.classLoader?.getResourceAsStream("sample_tts_audio.mp3")?.readBytes() ?: ByteArray(0)),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "audio/mpeg")
            )
        }

        val mockMediaPlayer = mockk<MediaPlayer>(relaxed = true)
        val ttsService = TtsServiceImpl("test_api_key", mockEngine) { mockMediaPlayer }
        val testText = "Hello, world!"

        ttsService.speak(testText)

        verify { mockMediaPlayer.setDataSource(any<String>()) }
        verify { mockMediaPlayer.prepare() }
        verify { mockMediaPlayer.start() }
        verify { mockMediaPlayer.setOnCompletionListener(any()) }

        // Simulate completion to trigger release and file deletion
        val listener = slot<MediaPlayer.OnCompletionListener>()
        verify { mockMediaPlayer.setOnCompletionListener(capture(listener)) }
        listener.captured.onCompletion(mockMediaPlayer)

        // Verify temp file deletion
        val tempFiles = File(System.getProperty("java.io.tmpdir")).listFiles { file ->
            file.name.startsWith("tts_audio") && file.name.endsWith(".mp3")
        }
        assertTrue(tempFiles == null || tempFiles.isEmpty())
    }
}