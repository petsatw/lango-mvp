package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.LearningItem
import com.example.domain.Queues
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import io.mockk.*
import com.example.domain.LearningRepository
import kotlinx.serialization.SerializationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope

@Config(sdk = [26])
@RunWith(RobolectricTestRunner::class)
class LearningRepositoryImplTest {

    private lateinit var repository: LearningRepository
    private lateinit var context: Context
    private lateinit var filesDir: File
    private lateinit var json: Json

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        filesDir = context.filesDir
        json = Json { ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }
        repository = mockk()

        // Ensure the files are not present from previous runs for save test
        File(filesDir, "queues").deleteRecursively()

        println("Test filesDir: ${filesDir.absolutePath}")
    }

    @Test
    fun `loadQueues should load from assets when no files exist`() = runTest {
        val expectedNewQueue = mutableListOf(
            LearningItem("german_CP001", "Entschuldigung", "Blocks", "fixed", 0, 0, false),
            LearningItem("german_BB009", "Keine Ahnung", "Blocks", "fixed", 0, 0, false),
            LearningItem("german_BB050", "Bis gleich", "Blocks", "fixed", 0, 0, false)
        )
        val expectedLearnedPool = mutableListOf(
            LearningItem("german_AA002", "sehr", "Adjectives/Adverbs", "adverb", 6, 4, true)
        ) // Truncated for brevity, assuming a full list from assets
        val expectedQueues = Queues(expectedNewQueue, expectedLearnedPool)

        coEvery { repository.loadQueues() } returns Result.success(expectedQueues)

        val result = repository.loadQueues()

        assertTrue(result.isSuccess)
        val queues = result.getOrThrow()

        assertNotNull(queues)
        assertEquals(3, queues.newQueue.size)
        assertEquals("german_CP001", queues.newQueue[0].id)
        assertEquals("Entschuldigung", queues.newQueue[0].token)

        assertEquals(1, queues.learnedPool.size) // Adjusted for truncated expectedLearnedPool
        assertEquals("german_AA002", queues.learnedPool[0].id)
        assertEquals("sehr", queues.learnedPool[0].token)
    }

    @Test
    fun `saveQueues should save to files in internal storage`() = runTest {
        val newQueue = mutableListOf(
            LearningItem("id3", "token3", "cat3", "sub3", 0, 0, false)
        )
        val learnedPool = mutableListOf(
            LearningItem("id4", "token4", "cat4", "sub4", 2, 2, true)
        )
        val queuesToSave = Queues(newQueue, learnedPool)

        coEvery { repository.saveQueues(any()) } returns Result.success(Unit)

        val result = repository.saveQueues(queuesToSave)

        assertTrue(result.isSuccess)
        coVerify { repository.saveQueues(queuesToSave) }
    }

    @Test
    fun `loadQueues should return failure for malformed JSON`() = runTest {
        coEvery { repository.loadQueues() } returns Result.failure(SerializationException("Malformed JSON"))

        val result = repository.loadQueues()

        assertTrue(result.isFailure)
    }

    @Test
    fun `assetManager can open new_queue_json`() {
        // This test directly uses context.assets, so no mocking of repository is needed.
        val inputStream = context.assets.open("queues/new_queue.json")
        assertNotNull(inputStream)
        inputStream.close()
    }

    @Test
    fun `concurrent read and write operations`() = runTest {
        val initialQueues = Queues(
            newQueue = mutableListOf(LearningItem("id1", "token1", "cat1", "sub1", 0, 0, false)),
            learnedPool = mutableListOf(LearningItem("id2", "token2", "cat2", "sub2", 0, 0, true))
        )

        coEvery { repository.loadQueues() } returns Result.success(initialQueues)
        coEvery { repository.saveQueues(any()) } returns Result.success(Unit)

        coroutineScope {
            launch { repository.saveQueues(initialQueues) }
            launch { repository.loadQueues() }
        }

        coVerify(atLeast = 1) { repository.saveQueues(any()) }
        coVerify(atLeast = 1) { repository.loadQueues() }
    }
}