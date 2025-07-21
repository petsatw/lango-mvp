package com.example.domain

import java.io.InputStream

interface LearningRepository {
    suspend fun loadQueues(): Result<Queues>
    suspend fun saveQueues(queues: Queues): Result<Unit>
}