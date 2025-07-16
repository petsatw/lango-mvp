package com.example.data

import android.content.Context
import com.example.domain.LearningItem
import com.example.domain.LearningRepository
import com.example.domain.Queues
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.SerializationException

class LearningRepositoryImpl(private val context: Context) : LearningRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }

    override fun loadQueues(filePaths: Pair<String?, String?>?): Queues {
        val newQueuePath = filePaths?.first ?: "core_blocks.json"
        val learnedQueuePath = filePaths?.second ?: "learned_queue.json"

        val newQueueStream = context.assets.open(newQueuePath)
        val learnedQueueStream = context.assets.open(learnedQueuePath)

        return loadQueues(newQueueStream, learnedQueueStream)
    }

    override fun loadQueues(newQueueStream: InputStream, learnedQueueStream: InputStream): Queues {
        val newQueueText = InputStreamReader(newQueueStream).readText()
        val learnedQueueText = InputStreamReader(learnedQueueStream).readText()

        return loadQueues(newQueueText, learnedQueueText)
    }

    override fun loadQueues(newQueueJson: String, learnedQueueJson: String): Queues {
        lateinit var newQueueItems: List<LearningItem>
        lateinit var learnedPoolItems: List<LearningItem>

        try {
            newQueueItems = json.decodeFromString(newQueueJson)
        } catch (e: SerializationException) {
            newQueueItems = emptyList()
        }

        try {
            learnedPoolItems = json.decodeFromString(learnedQueueJson)
        } catch (e: SerializationException) {
            learnedPoolItems = emptyList()
        }

        return Queues(newQueueItems.toMutableList(), learnedPoolItems.toMutableList())
    }

    override fun saveQueues(queues: Queues) {
        val newQueueFile = File(context.filesDir, "core_blocks.json")
        val learnedQueueFile = File(context.filesDir, "learned_queue.json")

        newQueueFile.writeText(json.encodeToString(queues.newQueue))
        learnedQueueFile.writeText(json.encodeToString(queues.learnedPool))
    }
}

