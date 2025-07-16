package com.example.data

import android.content.Context
import com.example.domain.LearningItem
import com.example.domain.LearningRepository
import com.example.domain.Queues
import java.io.File
import java.io.InputStreamReader
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class LearningRepositoryImpl(private val context: Context) : LearningRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }
    private var testClassLoader: ClassLoader? = null

    // Setter for testClassLoader, to be used in tests
    fun setTestClassLoader(classLoader: ClassLoader) {
        this.testClassLoader = classLoader
    }

    override fun loadQueues(filePaths: Pair<String?, String?>?): Queues {
        val newQueuePath = filePaths?.first ?: "core_blocks.json"
        val learnedQueuePath = filePaths?.second ?: "learned_queue.json"

        val newQueueStream = testClassLoader?.getResourceAsStream(newQueuePath)
            ?: context.assets.open(newQueuePath)
        val learnedQueueStream = testClassLoader?.getResourceAsStream(learnedQueuePath)
            ?: context.assets.open(learnedQueuePath)

        val newQueueItems: List<LearningItem> = json.decodeFromString(InputStreamReader(newQueueStream).readText())

        val learnedPoolItems: List<LearningItem> = json.decodeFromString(InputStreamReader(learnedQueueStream).readText())

        return Queues(newQueueItems.toMutableList(), learnedPoolItems.toMutableList())
    }

    override fun saveQueues(queues: Queues) {
        val newQueueFile = File(context.filesDir, "core_blocks.json")
        val learnedQueueFile = File(context.filesDir, "learned_queue.json")

        newQueueFile.writeText(json.encodeToString(queues.newQueue))
        learnedQueueFile.writeText(json.encodeToString(queues.learnedPool))
    }
}

