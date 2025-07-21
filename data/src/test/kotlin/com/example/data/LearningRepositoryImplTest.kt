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
import java.nio.file.AccessDeniedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.every
import io.mockk.Runs
import io.mockk.just
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.clearAllMocks
import io.mockk.unmockkAll
import io.mockk.every
import io.mockk.any
import io.mockk.answers
import io.mockk.throws

@Config(sdk = [26])
@RunWith(RobolectricTestRunner::class)
class LearningRepositoryImplTest {

    private lateinit var repository: LearningRepositoryImpl
    private lateinit var context: Context
    private lateinit var filesDir: File
    private lateinit var sampleQueues: Queues

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        filesDir = context.filesDir
        repository = LearningRepositoryImpl(context.assets, filesDir, Json { ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true })

        sampleQueues = Queues(
            newQueue = mutableListOf(LearningItem("id1", "token1", "cat1", "sub1", 0, 0, false)),
            learnedPool = mutableListOf(LearningItem("id2", "token2", "cat2", "sub2", 0, 0, true))
        )

        // Ensure the files are not present from previous runs for save test
        File(filesDir, "queues").deleteRecursively()
        println("Test filesDir: ${filesDir.absolutePath}")
    }

    @Test
    fun `loadQueues should load from assets when no files exist`() = runTest {
        val result = repository.loadQueues()

        assertTrue(result.isSuccess)
        val queues = result.getOrThrow()

        assertNotNull(queues)
        assertEquals(3, queues.newQueue.size)
        assertEquals("german_CP001", queues.newQueue[0].id)
        assertEquals("Entschuldigung", queues.newQueue[0].token)

        assertEquals(99, queues.learnedPool.size)
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

        repository.saveQueues(queuesToSave)

        val coreBlocksFile = File(filesDir, "queues/new_queue.json")
        val learnedQueueFile = File(filesDir, "queues/learned_queue.json")

        assertTrue(coreBlocksFile.exists())
        assertTrue(learnedQueueFile.exists())

        val loadedCoreBlocks = coreBlocksFile.readText()
        val loadedLearnedQueue = learnedQueueFile.readText()

        assertTrue(loadedCoreBlocks.contains("id3"))
        assertTrue(loadedCoreBlocks.contains("token3"))
        assertTrue(loadedLearnedQueue.contains("id4"))
        assertTrue(loadedLearnedQueue.contains("token4"))
    }

    @Test
    fun `loadQueues should return failure for malformed JSON`() = runTest {
        val malformedJson = "{\"newQueue\": [{\"id\": \"id1\", \"token\": \"token1\", \"cat\": \"cat1\", \"sub\": \"sub1\", \"pres\": 0, \"usage\": 0, \"learned\": false},]}" // Malformed JSON
        val newQueueFile = File(filesDir, "queues/new_queue.json")
        newQueueFile.parentFile?.mkdirs()
        newQueueFile.writeText(malformedJson)

        val result = repository.loadQueues()

        assertTrue(result.isFailure)
    }

    @Test
    fun `assetManager can open new_queue_json`() {
        val inputStream = context.assets.open("queues/new_queue.json")
        assertNotNull(inputStream)
        inputStream.close()
    }

    
}