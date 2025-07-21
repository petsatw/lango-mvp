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
            try {
                ensureBootstrap()
                val newItems: List<LearningItem> =
                    json.decodeFromString(newQueueFile.readText())
                val learnedItems: List<LearningItem> =
                    json.decodeFromString(learnedQueueFile.readText())
                Result.success(Queues(newItems.toMutableList(), learnedItems.toMutableList()))
            } catch (e: SerializationException) {
                Result.failure(e)
            } catch (e: IOException) {
                Result.failure(e)
            }
        }
    }

    override suspend fun saveQueues(queues: Queues): Result<Unit> = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            try {
                atomicWrite(newQueueFile, json.encodeToString(queues.newQueue))
                atomicWrite(learnedQueueFile, json.encodeToString(queues.learnedPool))
                Result.success(Unit)
            } catch (e: IOException) {
                Result.failure(e)
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
        val tmp = File.createTempFile(target.nameWithoutExtension, ".tmp", target.parentFile)
        tmp.writeText(text)
        Files.move(tmp.toPath(), target.toPath(),
                   StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }
}