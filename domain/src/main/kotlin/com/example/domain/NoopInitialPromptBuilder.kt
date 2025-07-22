package com.example.domain

import java.util.UUID

class NoopInitialPromptBuilder : InitialPromptBuilder {
    override fun build(queues: Queues, sessionId: UUID): String {
        throw NotImplementedError("InitialPromptBuilder not yet implemented")
    }
}