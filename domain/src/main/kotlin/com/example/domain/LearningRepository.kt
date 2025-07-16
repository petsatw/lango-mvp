package com.example.domain

import android.content.Context

interface LearningRepository {
    fun loadQueues(context: Context, filePaths: Pair<String?, String?>?): Queues
    fun saveQueues(context: Context, queues: Queues)
}
