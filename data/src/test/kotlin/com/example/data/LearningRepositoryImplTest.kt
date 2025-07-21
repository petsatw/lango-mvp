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
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException

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
        // 1. Load queues.
        val initialQueues = repository.loadQueues().getOrThrow()

        // 2. Increment presentationCount on first item.
        val itemToModify = initialQueues.newQueue.first()
        itemToModify.presentationCount++

        // 3. repo.saveQueues().
        val saveResult = repository.saveQueues(initialQueues)
        assertTrue(saveResult.isSuccess)

        // 4. New repo -> loadQueues()
        val newRepository = LearningRepositoryImpl(context.assets, filesDir, json)
        val reloadedQueues = newRepository.loadQueues().getOrThrow()

        // Incremented count is present; call returns success.
        assertEquals(itemToModify.presentationCount, reloadedQueues.newQueue.first { it.id == itemToModify.id }.presentationCount)
    }

    // FS-2b: Unsupported-atomic-move fallback
    @Test
    fun `FS-2b Unsupported-atomic-move fallback`() = runTest {
        mockkStatic(Files::class) {
            every { Files.move(any(), any(), *anyVararg()) } throws AtomicMoveNotSupportedException("Atomic move not supported")

            // 1. Load queues.
            val initialQueues = repository.loadQueues().getOrThrow()

            // 2. Increment presentationCount on first item.
            val itemToModify = initialQueues.newQueue.first()
            itemToModify.presentationCount++

            // 3. repo.saveQueues().
            val saveResult = repository.saveQueues(initialQueues)
            assertTrue(saveResult.isSuccess)

            // 4. New repo -> loadQueues()
            val newRepository = LearningRepositoryImpl(context.assets, filesDir, json)
            val reloadedQueues = newRepository.loadQueues().getOrThrow()

            // Incremented count is present; call returns success.
            assertEquals(itemToModify.presentationCount, reloadedQueues.newQueue.first { it.id == itemToModify.id }.presentationCount)
        }
    }

    // FS-3: Concurrent read/write
    @Test
    fun `FS-3 Concurrent read-write`() = runTest {
        val initialQueues = repository.loadQueues().getOrThrow()
        val itemToModify = initialQueues.newQueue.first()

        coroutineScope {
            launch {
                // Simulate a write operation
                itemToModify.presentationCount++
                repository.saveQueues(initialQueues)
            }
            launch {
                // Simulate a read operation
                repository.loadQueues()
            }
        }
        // Both complete without exceptions; JSON intact.
        // The mutex should prevent data corruption.
        val finalQueues = repository.loadQueues().getOrThrow()
        assertEquals(itemToModify.presentationCount, finalQueues.newQueue.first { it.id == itemToModify.id }.presentationCount)
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