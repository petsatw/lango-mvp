package com.example.domain

interface CoachOrchestrator {
    suspend fun startSession(): Result<Session>
    suspend fun processTurn(userResponseText: String): Result<Session>
    suspend fun endSession(queues: Queues): Result<Unit>
}