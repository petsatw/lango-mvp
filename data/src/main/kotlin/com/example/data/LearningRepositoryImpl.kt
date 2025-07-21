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
                    json.decodeFromString(newQueueFile.readText().also { println("New Queue Content: $it") })
                val learnedItems: List<LearningItem> =
                    json.decodeFromString(learnedQueueFile.readText().also { println("Learned Queue Content: $it") })
                Result.success(Queues(newItems.toMutableList(), learnedItems.toMutableList()))
            } catch (e: SerializationException) {
                println("SerializationException: ${e.message}")
                e.printStackTrace()
                Result.failure(e)
            } catch (e: IOException) {
                println("IOException: ${e.message}")
                e.printStackTrace()
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
                println("IOException during save: ${e.message}")
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    private fun ensureBootstrap() {
        if (newQueueFile.exists() && learnedQueueFile.exists()) return
        copyFromAssets()
    }

    private fun copyFromAssets() {
        println("copyFromAssets: Creating parent directories for ${newQueueFile.parentFile?.absolutePath}")
        newQueueFile.parentFile?.mkdirs()
        println("copyFromAssets: Parent directories created: ${newQueueFile.parentFile?.exists()}")

        println("copyFromAssets: Copying new_queue.json from assets")
        assetManager.open("queues/new_queue.json")
            .use { it.copyTo(newQueueFile.outputStream()) }
        println("copyFromAssets: new_queue.json copied: ${newQueueFile.exists()}")

        println("copyFromAssets: Copying learned_queue.json from assets")
        assetManager.open("queues/learned_queue.json")
            .use { it.copyTo(learnedQueueFile.outputStream()) }
        println("copyFromAssets: learned_queue.json copied: ${learnedQueueFile.exists()}")
    }

    private fun atomicWrite(target: File, text: String) {
        val tmp = File.createTempFile(target.nameWithoutExtension, ".tmp", target.parentFile)
        tmp.writeText(text)

        try {
            Files.move(
                tmp.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (ioe: IOException) {
            // Windows or exotic FS: ATOMIC_MOVE unsupported or file locked – retry non-atomic
            Files.move(
                tmp.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }
}