package com.example.domain

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import com.example.domain.PromptTemplateLoader

class InitialPromptBuilderImpl(
    private val json: Json,
    private val promptTemplateLoader: PromptTemplateLoader
) : InitialPromptBuilder {
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

        val promptTemplates = runBlocking { promptTemplateLoader.loadPromptTemplate("PROMPT.md") }
        val systemPrompt = promptTemplates["SYSTEM"] ?: throw IllegalStateException("SYSTEM prompt not found")
        val dialoguePromptTemplate = promptTemplates["DIALOGUE"] ?: throw IllegalStateException("DIALOGUE prompt not found")

        val dialogueBody = dialoguePromptTemplate
            .replace("{newTarget.token}", newTarget.token)

        val body = listOf(systemPrompt, dialogueBody)

        val prompt = Prompt(header, body.toList())
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
