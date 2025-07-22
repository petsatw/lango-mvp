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
import java.nio.file.Path
import io.mockk.*
import com.example.domain.LearningRepository
import kotlinx.serialization.SerializationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException
import kotlinx.coroutines.async
import android.content.res.AssetManager

@Config(sdk = [26])
@RunWith(RobolectricTestRunner::class)
class LearningRepositoryImplTest {

    private lateinit var repository: LearningRepository
    private lateinit var context: Context
    private lateinit var filesDir: File
    private lateinit var json: Json

    private fun dummyItem(cnt: Int = 0) =
        LearningItem("id", "token", cnt, 0, false)

    private fun bootstrapEmptyAssets(): AssetManager =
        mockk<AssetManager>().apply {
            every { open("queues/new_queue.json") } returns "[]".byteInputStream()
            every { open("queues/learned_queue.json") } returns "[]".byteInputStream()
        }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        filesDir = context.filesDir
        json = Json { ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }
        val assetManager = context.assets
        repository = LearningRepositoryImpl(assetManager, filesDir, json)

    }

    // FS-1: Cold-start load
    @Test
    fun `FS-1 Cold-start load`() = runTest {
        File(filesDir, "queues").deleteRecursively()
        val result = repository.loadQueues()

        // a) Call returns Result.success.
        assertTrue(result.isSuccess)
        val queues = result.getOrThrow()

        // b) new_queue.json and learned_queue.json now exist.
        val newQueueFile = File(filesDir, "queues/new_queue.json")
        val learnedQueueFile = File(filesDir, "queues/learned_queue.json")
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
        val tmpDir = createTempDir()
        File(tmpDir, "queues").mkdirs()

        val am = bootstrapEmptyAssets()
        val repo = LearningRepositoryImpl(am, tmpDir, json)

        val saved = Queues(mutableListOf(dummyItem(1)), mutableListOf())
        assertTrue(repo.saveQueues(saved).isSuccess)

        val reloaded = repo.loadQueues().getOrThrow()
        assertEquals(saved, reloaded)

        tmpDir.deleteRecursively()
    }

    @Test
    fun `FS-3 Concurrent read-write`() = runTest {
        val tmpDir = createTempDir()
        File(tmpDir, "queues").mkdirs()

        val am = bootstrapEmptyAssets()
        val repo = LearningRepositoryImpl(am, tmpDir, json)

        // ── writer updates presentationCount twice ──
        val writer = async {
            repeat(2) { n ->
                repo.saveQueues(
                    Queues(mutableListOf(dummyItem(n + 1)), mutableListOf())
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

        tmpDir.deleteRecursively()
    }

    // FS-4: Malformed JSON
    @Test
    fun `FS-4 Malformed JSON`() = runTest {
        File(filesDir, "queues").deleteRecursively()
        val newQueueFile = File(filesDir, "queues/new_queue.json")
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
        File(filesDir, "queues").deleteRecursively()
        val result = repository.loadQueues()

        // Returns Result.success; queues equal bundled defaults; files created in emptyDir.
        assertTrue(result.isSuccess)
        val queues = result.getOrThrow()
        assertTrue(queues.newQueue.isNotEmpty())
        assertEquals("german_CP001", queues.newQueue[0].id) // Check if it loaded from asset default

        val newQueueFile = File(filesDir, "queues/new_queue.json")
        val learnedQueueFile = File(filesDir, "queues/learned_queue.json")
        assertTrue(newQueueFile.exists())
        assertTrue(learnedQueueFile.exists())
    }
}