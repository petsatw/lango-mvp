package com.example.domain

interface LearningRepository {
    suspend fun loadQueues(): Result<Queues>
    suspend fun saveQueues(queues: Queues): Result<Unit>
}