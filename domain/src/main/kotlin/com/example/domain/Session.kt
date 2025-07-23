package com.example.domain

import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val sessionId: String,
    val startTime: Long,
    val queues: Queues,
    val newTarget: LearningItem?
)
