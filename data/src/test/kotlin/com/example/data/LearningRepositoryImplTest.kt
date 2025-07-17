package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.LearningItem
import com.example.domain.Queues
import kotlinx.serialization.SerializationException

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class LearningRepositoryImplTest {

    private lateinit var repository: LearningRepositoryImpl
    private lateinit var context: Context
    private lateinit var filesDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        filesDir = context.filesDir
        repository = LearningRepositoryImpl(context)

        // Ensure the files are not present from previous runs for save test
        File(filesDir, "core_blocks.json").delete()
        File(filesDir, "learned_queue.json").delete()
    }

    @Test
    fun `loadQueues should load from core_blocks and learned_queue json files from resources`() {
        val newQueueContent = checkNotNull(javaClass.classLoader?.getResourceAsStream("core_blocks.json")).bufferedReader().use { it.readText() }
        val learnedQueueContent = checkNotNull(javaClass.classLoader?.getResourceAsStream("learned_queue.json")).bufferedReader().use { it.readText() }

        val queues = repository.loadQueues(newQueueContent, learnedQueueContent)

        assertNotNull(queues)
        assertEquals(3, queues.newQueue.size)
        assertEquals("german_CP001", queues.newQueue[0].id)
        assertEquals("Entschuldigung", queues.newQueue[0].token)

        assertEquals(99, queues.learnedPool.size)
        assertEquals("german_AA002", queues.learnedPool[0].id)
        assertEquals("sehr", queues.learnedPool[0].token)
    }

    @Test
    fun `saveQueues should save to core_blocks and learned_queue json files in internal storage`() {
        val newQueue = mutableListOf(
            LearningItem("id3", "token3", "cat3", "sub3", 0, 0, false)
        )
        val learnedPool = mutableListOf(
            LearningItem("id4", "token4", "cat4", "sub4", 2, 2, true)
        )
        val queuesToSave = Queues(newQueue, learnedPool)

        repository.saveQueues(queuesToSave)

        val coreBlocksFile = File(filesDir, "core_blocks.json")
        val learnedQueueFile = File(filesDir, "learned_queue.json")

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
    fun `loadQueues should handle malformed JSON for newQueue`() {
        val malformedJson = "{\"newQueue\": [{\"id\": \"id1\", \"token\": \"token1\", \"cat\": \"cat1\", \"sub\": \"sub1\", \"pres\": 0, \"usage\": 0, \"learned\": false},]}" // Malformed JSON
        val learnedQueueContent = checkNotNull(javaClass.classLoader?.getResourceAsStream("learned_queue.json")).bufferedReader().use { it.readText() }

        var exceptionThrown = false
        try {
            repository.loadQueues(malformedJson, learnedQueueContent)
        } catch (e: Exception) {
            exceptionThrown = true
            // Optionally, assert on the type of exception if known (e.g., JsonSyntaxException)
            // assertTrue(e is JsonSyntaxException)
        }
        assertTrue(exceptionThrown)
    }

    @Test
    fun `loadQueues should handle malformed JSON for learnedPool`() {
        val newQueueContent = checkNotNull(javaClass.classLoader?.getResourceAsStream("core_blocks.json")).bufferedReader().use { it.readText() }
        val malformedJson = "{\"learnedPool\": [{\"id\": \"id1\", \"token\": \"token1\", \"cat\": \"cat1\", \"sub\": \"sub1\", \"pres\": 0, \"usage\": 0, \"learned\": false},]}" // Malformed JSON

        var exceptionThrown = false
        try {
            repository.loadQueues(newQueueContent, malformedJson)
        } catch (e: Exception) {
            e.printStackTrace()
            exceptionThrown = true
        }
        assertTrue(exceptionThrown)
    }
}
