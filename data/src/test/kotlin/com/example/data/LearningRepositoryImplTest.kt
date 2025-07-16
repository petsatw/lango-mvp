package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.LearningItem
import com.example.domain.Queues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.InputStream

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
        repository.setTestClassLoader(requireNotNull(javaClass.classLoader))

        // Ensure the files are not present from previous runs for save test
        File(filesDir, "core_blocks.json").delete()
        File(filesDir, "learned_queue.json").delete()
    }

    @Test
    fun `loadQueues should load from core_blocks and learned_queue json files from resources`() {
        val queues = repository.loadQueues(null)

        assertNotNull(queues)
        assertEquals(3, queues.newQueue.size)
        assertEquals("german_CP001", queues.newQueue[0].id)
        assertEquals("Entschuldigung", queues.newQueue[0].token)

        assertEquals(70, queues.learnedPool.size)
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

    // TODO: Add test for malformed JSON
}
