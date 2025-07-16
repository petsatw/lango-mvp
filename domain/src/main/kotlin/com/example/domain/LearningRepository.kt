package com.example.domain

import android.content.Context
import java.io.InputStream

interface LearningRepository {
    fun loadQueues(filePaths: Pair<String?, String?>?): Queues
    fun loadQueues(newQueueStream: InputStream, learnedQueueStream: InputStream): Queues
    fun loadQueues(newQueueJson: String, learnedQueueJson: String): Queues
    fun saveQueues(queues: Queues)
}
