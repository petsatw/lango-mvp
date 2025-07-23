package com.example.domain

import com.example.domain.Session
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoachOrchestratorImpl @Inject constructor(
    private val startSessionUseCase: StartSessionUseCase,
    private val processTurnUseCase: ProcessTurnUseCase,
    private val generateDialogueUseCase: GenerateDialogueUseCase,
    private val endSessionUseCase: EndSessionUseCase
) : CoachOrchestrator {

    private var currentSession: Session? = null

    override suspend fun startSession(): Result<Session> {
        return startSessionUseCase.startSession()
            .onSuccess { session ->
                currentSession = session
                // Initial coach dialogue generation will be handled by the UI layer
                // calling generateDialogueUseCase based on the session state.
            }
    }

    override suspend fun processTurn(userResponseText: String): Result<Session> {
        val session = currentSession
        if (session == null) {
            return Result.failure(IllegalStateException("Session not started"))
        }

        return processTurnUseCase.processTurn(session.queues, userResponseText)
            .onSuccess { updatedQueues ->
                currentSession = session.copy(queues = updatedQueues, newTarget = updatedQueues.newQueue.firstOrNull() ?: session.newTarget)
                if (updatedQueues.newQueue.isEmpty()) {
                    endSessionUseCase.endSession(updatedQueues)
                    currentSession = null
                }
            }.map { currentSession!! }
    }

    override suspend fun endSession(queues: Queues): Result<Unit> {
        return endSessionUseCase.endSession(queues)
    }
}