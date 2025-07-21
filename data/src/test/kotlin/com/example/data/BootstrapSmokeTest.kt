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

import org.robolectric.shadows.ShadowAssetManager

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

        // Simulate some learning progress
        val newTarget = initialQueues.newQueue.first()
        newTarget.usageCount = 3 // Master the first item

        // Save the updated queues
        repository.saveQueues(initialQueues)

        // Load queues again to verify persistence
        val loadedQueuesResult = repository.loadQueues()
        assertTrue(loadedQueuesResult.isSuccess)
        val loadedQueues = loadedQueuesResult.getOrThrow()

        assertNotNull(loadedQueues)
        assertEquals(2, loadedQueues.newQueue.size) // One item should have moved to learnedPool
        assertEquals(100, loadedQueues.learnedPool.size) // One new item in learnedPool

        // Verify the mastered item is in the learned pool and marked as learned
        val masteredItem = loadedQueues.learnedPool.find { it.id == newTarget.id }
        assertNotNull(masteredItem)
        assertTrue(masteredItem!!.isLearned)
    }

    @Test
    fun `smoke test - empty queues after mastery`() = runTest {
        // Create a scenario where newQueue becomes empty after mastery
        val singleItemNewQueue = mutableListOf(
            LearningItem("test_id", "TestToken", "TestCategory", "TestSubcategory", 0, 0, false)
        )
        val emptyLearnedPool = mutableListOf<LearningItem>()
        val queues = Queues(singleItemNewQueue, emptyLearnedPool)

        // Manually set the repository's queues for this test scenario
        // This is a simplification; in a real scenario, you'd mock the asset loading
        // or provide a test-specific asset manager.
        // For now, we'll directly manipulate the queues object that the repository would load.

        // Simulate mastering the item
        queues.newQueue.first().usageCount = 3
        repository.saveQueues(queues)

        val loadedQueuesResult = repository.loadQueues()
        assertTrue(loadedQueuesResult.isSuccess)
        val loadedQueues = loadedQueuesResult.getOrThrow()

        assertTrue(loadedQueues.newQueue.isEmpty())
        assertEquals(1, loadedQueues.learnedPool.size)
        assertTrue(loadedQueues.learnedPool.first().isLearned)
    }
}