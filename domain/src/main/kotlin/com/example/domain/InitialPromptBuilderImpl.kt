package com.example.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class InitialPromptBuilderImpl(private val json: Json) : InitialPromptBuilder {
    override fun build(queues: Queues, sessionId: UUID): String {
        val newTarget = queues.newQueue.firstOrNull()
            ?: throw IllegalStateException("New queue cannot be empty for initial prompt construction")
        val learnedPool = queues.learnedPool

        val header = Header(
            sessionId = sessionId.toString(),
            newTarget = newTarget.toHeaderItem(),
            learnedPool = learnedPool.map { it.toHeaderItem() },
            delimiter = "—"
        )

        val body = listOf(
            "You are now “Lango,” a voice‐only German coach for beginners in Linz, Austria.",
            "Explain what '${newTarget.token}' means in a very short sentence.",
            "Give one simple example with '${newTarget.token}'."
        )

        val prompt = Prompt(header, body)
        return json.encodeToString(prompt)
    }
}

@Serializable
data class Header(
    val sessionId: String,
    val newTarget: HeaderItem,
    val learnedPool: List<HeaderItem>,
    val delimiter: String
)

@Serializable
data class HeaderItem(
    val id: String,
    val token: String,
    val presentationCount: Int? = null,
    val usageCount: Int? = null
)

@Serializable
data class Prompt(
    val header: Header,
    val body: List<String>
)

fun LearningItem.toHeaderItem(): HeaderItem {
    return HeaderItem(
        id = this.id,
        token = this.token,
        presentationCount = this.presentationCount,
        usageCount = this.usageCount
    )
}
