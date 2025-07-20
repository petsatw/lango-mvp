package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.LearningItem
import com.example.domain.Queues
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Ignore
import java.io.File

@Ignore("Enable after FR-1 merges")
@Config(manifest=Config.NONE)
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
        repository = LearningRepositoryImpl(context, json, context.assets, filesDir)

        // Ensure the files are not present from previous runs for save test
        File(filesDir, "queues/new_queue.json").delete()
        File(filesDir, "queues/learned_queue.json").delete()
    }

    @Test
    fun `smoke test - initial load and save`() {
        // Load initial queues from assets
        val initialQueues = repository.loadQueues(null)

        assertNotNull(initialQueues)
        assertEquals(3, initialQueues.newQueue.size)
        assertEquals(99, initialQueues.learnedPool.size)

        // Simulate some learning progress
        val newTarget = initialQueues.newQueue.first()
        newTarget.usageCount = 3 // Master the first item

        // Save the updated queues
        repository.saveQueues(initialQueues)

        // Load queues again to verify persistence
        val loadedQueues = repository.loadQueues(null)

        assertNotNull(loadedQueues)
        assertEquals(2, loadedQueues.newQueue.size) // One item should have moved to learnedPool
        assertEquals(100, loadedQueues.learnedPool.size) // One new item in learnedPool

        // Verify the mastered item is in the learned pool and marked as learned
        val masteredItem = loadedQueues.learnedPool.find { it.id == newTarget.id }
        assertNotNull(masteredItem)
        assertTrue(masteredItem!!.isLearned)
    }

    @Test
    fun `smoke test - empty queues after mastery`() {
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

        val loadedQueues = repository.loadQueues(null)

        assertTrue(loadedQueues.newQueue.isEmpty())
        assertEquals(1, loadedQueues.learnedPool.size)
        assertTrue(loadedQueues.learnedPool.first().isLearned)
    }
}