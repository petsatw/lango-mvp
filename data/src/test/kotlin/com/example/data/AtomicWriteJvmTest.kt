package com.example.data

import com.example.domain.LearningItem
import com.example.domain.Queues
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import io.mockk.mockk
import io.mockk.every

class AtomicWriteJvmTest {
    private val json = Json { encodeDefaults = true }

    private fun dummyItem(): LearningItem {
        return LearningItem("id", "token", 0, 0, false)
    }

    @Test
    fun atomicWrite_succeeds_on_JVM() {
        val tmpDir = createTempDir()
        File(tmpDir, "queues").mkdirs()
        val mockAssetManager = mockk<android.content.res.AssetManager>()
        every { mockAssetManager.open("queues/new_queue.json") } returns "[]".byteInputStream()
        every { mockAssetManager.open("queues/learned_queue.json") } returns "[]".byteInputStream()

        val repo = LearningRepositoryImpl(mockAssetManager, tmpDir, json)

        val queues = Queues(mutableListOf(dummyItem()), mutableListOf())
        val result = runBlocking { repo.saveQueues(queues) }

        assertTrue(result.isSuccess)
        assertTrue(File(tmpDir, "queues/new_queue.json").exists())
        tmpDir.deleteRecursively()
    }
}
