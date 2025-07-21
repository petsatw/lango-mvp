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

@Config(sdk = [26])
@RunWith(RobolectricTestRunner::class)
class BootstrapSmokeTest {

    private lateinit var repository: LearningRepositoryImpl
    private lateinit var context: Context
    private lateinit var filesDir: File
    private lateinit var json: Json

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        filesDir = context.filesDir
        json = Json { ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }
        repository = LearningRepositoryImpl(context.assets, filesDir, json)

        // Ensure the files are not present from previous runs for save test
        File(filesDir, "queues").deleteRecursively()
        println("Test filesDir: ${filesDir.absolutePath}")
    }

    @Test
    fun `smoke test - initial load and save`() = runTest {
        // Load initial queues from assets
        val initialQueuesResult = repository.loadQueues()
        assertTrue(initialQueuesResult.isSuccess)
        val initialQueues = initialQueuesResult.getOrThrow()

        assertNotNull(initialQueues)
        assertEquals(3, initialQueues.newQueue.size)
        assertEquals(99, initialQueues.learnedPool.size)

        // Save the updated queues
        repository.saveQueues(initialQueues)

        // Load queues again to verify persistence
        val loadedQueuesResult = repository.loadQueues()
        assertTrue(loadedQueuesResult.isSuccess)
        val loadedQueues = loadedQueuesResult.getOrThrow()

        assertNotNull(loadedQueues)
        assertEquals(3, loadedQueues.newQueue.size)
        assertEquals(99, loadedQueues.learnedPool.size)
    }
}