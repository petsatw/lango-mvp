package com.example.data

import android.content.Context
import com.example.domain.LearningItem
import com.example.domain.LearningRepository
import com.example.domain.Queues
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

class LearningRepositoryImpl : LearningRepository {

    private val gson = Gson()

    override fun loadQueues(context: Context, filePaths: Pair<String?, String?>?): Queues {
        val newQueuePath = filePaths?.first ?: "new_queue.json"
        val learnedQueuePath = filePaths?.second ?: "learned_queue.json"

        val newQueueStream = context.assets.open(newQueuePath)
        val learnedQueueStream = context.assets.open(learnedQueuePath)

        val newQueueItems: List<LearningItem> = gson.fromJson(
            InputStreamReader(newQueueStream),
            object : TypeToken<List<LearningItem>>() {}.type
        )

        val learnedPoolItems: List<LearningItem> = gson.fromJson(
            InputStreamReader(learnedQueueStream),
            object : TypeToken<List<LearningItem>>() {}.type
        )

        return Queues(newQueueItems.toMutableList(), learnedPoolItems.toMutableList())
    }

    override fun saveQueues(context: Context, queues: Queues) {
        // Not implemented yet
    }
}
