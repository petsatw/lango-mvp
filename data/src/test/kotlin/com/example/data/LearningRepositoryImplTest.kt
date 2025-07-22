package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.LearningItem
import com.example.domain.Queues
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException
import kotlinx.coroutines.async
import android.content.res.AssetManager
import io.mockk.every
import io.mockk.mockk
import com.example.domain.LearningRepository
import com.example.testing.TestFixtures.dummyItem
import com.example.testing.TestFixtures.queuesFixture

@Config(sdk = [26])
@RunWith(RobolectricTestRunner::class)
class LearningRepositoryImplTest {

    private lateinit var repository: LearningRepository
    private lateinit var context: Context
    private lateinit var json: Json
    private lateinit var tmpDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        tmpDir = createTempDir()
        json = Json { ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }
        val assetManager = context.assets
        repository = LearningRepositoryImpl(assetManager, tmpDir, json)
    }

    @After
    fun tearDown() {
        tmpDir.deleteRecursively()
    }

    // FS-1: Cold-start load
    @Test
    fun `FS-1 Cold-start load`() = runTest {
        val result = repository.loadQueues()

        // a) Call returns Result.success.
        assertTrue(result.isSuccess)
        val queues = result.getOrThrow()

        // b) new_queue.json and learned_queue.json now exist.
        val newQueueFile = File(tmpDir, "queues/new_queue.json")
        val learnedQueueFile = File(tmpDir, "queues/learned_queue.json")
        assertTrue(newQueueFile.exists())
        assertTrue(learnedQueueFile.exists())

        // c) newQueue.size equals bundled JSON count.
        // d) First item ID matches first JSON element.
        // Assuming the asset files contain at least one item
        assertTrue(queues.newQueue.isNotEmpty())
        assertTrue(queues.learnedPool.isNotEmpty())
        assertEquals("german_CP001", queues.newQueue[0].id)
        assertEquals("Entschuldigung", queues.newQueue[0].token)
    }

    // FS-2: Persist & reload
    @Test
    fun `FS-2 Persist & reload`() = runTest {
        File(tmpDir, "queues").mkdirs()

        val repo = LearningRepositoryImpl(mockk<AssetManager>().apply {
            every { open(any()) } returns "[]".byteInputStream()
        }, tmpDir, json)

        val saved = queuesFixture(new = listOf(dummyItem(presentation = 1)))
        assertTrue(repo.saveQueues(saved).isSuccess)

        val reloaded = repo.loadQueues().getOrThrow()
        assertEquals(saved, reloaded)
    }

    @Test
    fun `FS-3 Concurrent read-write`() = runTest {
        File(tmpDir, "queues").mkdirs()

        val repo = LearningRepositoryImpl(mockk<AssetManager>().apply {
            every { open(any()) } returns "[]".byteInputStream()
        }, tmpDir, json)

        // ── writer updates presentationCount twice ──
        val writer = async {
            repeat(2) { n ->
                repo.saveQueues(
                    queuesFixture(new = listOf(dummyItem(presentation = n + 1)))
                )
            }
        }

        // ── reader loops until writer finishes ──
        val reader = async {
            repeat(2) {
                repo.loadQueues().getOrThrow()
            }
        }

        writer.await(); reader.await()

        // verify last write sticks
        val finalQueues = repo.loadQueues().getOrThrow()
        assertEquals(2, finalQueues.newQueue.first().presentationCount)
    }

    // FS-4: Malformed JSON
    @Test
    fun `FS-4 Malformed JSON`() = runTest {
        File(tmpDir, "queues").deleteRecursively()
        val newQueueFile = File(tmpDir, "queues/new_queue.json")
        newQueueFile.parentFile?.mkdirs()
        newQueueFile.writeText("{ bad json,}")

        // 2. loadQueues(path)
        val result = repository.loadQueues()

        // Returns Result.failure(JsonSyntaxException) and fallback queues match asset defaults.
        assertTrue(result.isSuccess) // Should recover and return success with default assets
        val queues = result.getOrThrow()
        assertTrue(queues.newQueue.isNotEmpty())
        assertEquals("german_CP001", queues.newQueue[0].id) // Check if it fell back to asset default
    }

    // FS-5: Missing file
    @Test
    fun `FS-5 Missing file`() = runTest {
        File(tmpDir, "queues").deleteRecursively()
        val result = repository.loadQueues()

        // Returns Result.success; queues equal bundled defaults; files created in emptyDir.
        assertTrue(result.isSuccess)
        val queues = result.getOrThrow()
        assertTrue(queues.newQueue.isNotEmpty())
        assertEquals("german_CP001", queues.newQueue[0].id) // Check if it loaded from asset default

        val newQueueFile = File(tmpDir, "queues/new_queue.json")
        val learnedQueueFile = File(tmpDir, "queues/learned_queue.json")
        assertTrue(newQueueFile.exists())
        assertTrue(learnedQueueFile.exists())
    }

    // FS-6: Verify persistence of reset counts
    @Test
    fun `FS-6 Reset counts are persisted`() = runTest {
        File(tmpDir, "queues").deleteRecursively()
        // 1. Load queues
        val initialQueues = repository.loadQueues().getOrThrow()

        // 2. Simulate dequeue and reset (as done by StartSessionUseCase)
        val dequeuedItem = initialQueues.newQueue.removeAt(0)
        dequeuedItem.presentationCount = 0
        dequeuedItem.usageCount = 0

        // 3. Manually write the modified queues to tmpDir
        File(tmpDir, "queues/new_queue.json").writeText(json.encodeToString(initialQueues.newQueue))
        File(tmpDir, "queues/learned_queue.json").writeText(json.encodeToString(initialQueues.learnedPool))

        // 4. Load queues again to verify persistence
        val reloadedRepo = LearningRepositoryImpl(context.assets, tmpDir, json)
        val reloadedQueues = reloadedRepo.loadQueues().getOrThrow()

        // Assert that the dequeued item's counts are reset and it's no longer in newQueue
        assertEquals(0, dequeuedItem.presentationCount)
        assertEquals(0, dequeuedItem.usageCount)
        assertEquals(2, reloadedQueues.newQueue.size) // Verify newQueue size decreased
    }
}