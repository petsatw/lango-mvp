package com.example.domain

import java.util.UUID

interface InitialPromptBuilder {
    fun build(queues: Queues, sessionId: UUID = UUID.randomUUID()): String
}