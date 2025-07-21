package com.example.data

import android.content.res.AssetManager
import com.example.domain.LearningItem
import com.example.domain.LearningRepository
import com.example.domain.Queues
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException

@Singleton
class LearningRepositoryImpl @Inject constructor(
    private val assetManager: AssetManager,
    private val baseDir: File,
    private val json: Json
) : LearningRepository {

    private val ioMutex = Mutex()
    private val newQueueFile get() = File(baseDir, "queues/new_queue.json")
    private val learnedQueueFile get() = File(baseDir, "queues/learned_queue.json")

    override suspend fun loadQueues(): Result<Queues> = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            runCatching {
                ensureBootstrap()
                val newItems: List<LearningItem> = json.decodeFromString(newQueueFile.readText())
                val learnedItems: List<LearningItem> = json.decodeFromString(learnedQueueFile.readText())
                Queues(newItems.toMutableList(), learnedItems.toMutableList())
            }.recoverCatching {
                restoreFromAssets()
            }
        }
    }

    override suspend fun saveQueues(queues: Queues): Result<Unit> = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            return@withLock runCatching {
                atomicWrite(newQueueFile, json.encodeToString(queues.newQueue))
                atomicWrite(learnedQueueFile, json.encodeToString(queues.learnedPool))
            }
        }
    }

    private fun ensureBootstrap() {
        if (newQueueFile.exists() && learnedQueueFile.exists()) return
        copyFromAssets()
    }

    private fun copyFromAssets() {
        newQueueFile.parentFile?.mkdirs()
        assetManager.open("queues/new_queue.json").use { it.copyTo(newQueueFile.outputStream()) }
        assetManager.open("queues/learned_queue.json").use { it.copyTo(learnedQueueFile.outputStream()) }
    }

    private fun atomicWrite(target: File, text: String) {
        val tmp = File.createTempFile(target.name, ".tmp", target.parentFile)
        tmp.writeText(text)
        try {
            Files.move(
                tmp.toPath(), target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (e: AtomicMoveNotSupportedException) {
            // Falls back to a regular, still thread-safe replace.
            Files.copy(
                tmp.toPath(), target.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    private fun restoreFromAssets(): Queues {
        copyFromAssets()
        val newItems: List<LearningItem> = json.decodeFromString(newQueueFile.readText())
        val learnedItems: List<LearningItem> = json.decodeFromString(learnedQueueFile.readText())
        return Queues(newItems.toMutableList(), learnedItems.toMutableList())
    }
}