package com.example.domain

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class InitialPromptBuilderTest {

    private lateinit var initialPromptBuilder: InitialPromptBuilderImpl
    private lateinit var json: Json

    @Before
    fun setup() {
        json = Json { ignoreUnknownKeys = true; prettyPrint = true }
        initialPromptBuilder = InitialPromptBuilderImpl(json)
    }

    @Test
    fun `build generates correct JSON for initial prompt`() {
        val newTarget = LearningItem("german_CP001", "Entschuldigung", 0, 0)
        val learnedPool = mutableListOf(
            LearningItem("german_AA002", "sehr", 6, 4),
            LearningItem("german_AA003", "viel", 4, 7)
        )
        val queues = Queues(mutableListOf(newTarget), learnedPool)

        val expectedJson = """
{
    "header": {
        "sessionId": "<uuid>",
        "newTarget": {
            "id": "german_CP001",
            "token": "Entschuldigung",
            "presentationCount": 0,
            "usageCount": 0
        },
        "learnedPool": [
            {
                "id": "german_AA002",
                "token": "sehr",
                "presentationCount": 6,
                "usageCount": 4
            },
            {
                "id": "german_AA003",
                "token": "viel",
                "presentationCount": 4,
                "usageCount": 7
            }
        ],
        "delimiter": "—"
    },
    "body": [
        "You are now “Lango,” a voice‐only German coach for beginners in Linz, Austria.",
        "Explain what 'Entschuldigung' means in a very short sentence.",
        "Give one simple example with 'Entschuldigung'."
    ]
}""".trimIndent()

        val result = initialPromptBuilder.build(queues, java.util.UUID.fromString("00000000-0000-0000-0000-000000000000"))

        // Replace the dynamic sessionId for comparison
        val regex = "\"sessionId\": \"[a-fA-F0-9-]+\"".toRegex()
        val cleanedResult = regex.replace(result, "\"sessionId\": \"<uuid>\"")

        assertEquals(expectedJson, cleanedResult)
    }
}