package com.example.domain

import android.content.Context

interface LearningRepository {
    fun loadQueues(filePaths: Pair<String?, String?>?): Queues
    fun saveQueues(queues: Queues)
}
