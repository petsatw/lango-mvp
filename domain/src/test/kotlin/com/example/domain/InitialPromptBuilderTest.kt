package com.example.domain

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import io.mockk.mockk
import io.mockk.coEvery
import com.example.domain.PromptTemplateLoader
import kotlinx.serialization.decodeFromString

class InitialPromptBuilderTest {

    private lateinit var initialPromptBuilder: InitialPromptBuilderImpl
    private lateinit var json: Json
    private lateinit var mockPromptTemplateLoader: PromptTemplateLoader

    @Before
    fun setup() {
        json = Json { ignoreUnknownKeys = true; prettyPrint = true }
        mockPromptTemplateLoader = mockk()
        initialPromptBuilder = InitialPromptBuilderImpl(json, mockPromptTemplateLoader)

        coEvery { mockPromptTemplateLoader.loadPromptTemplate(any()) } returns mapOf(
            "SYSTEM" to "You are now “Lango,” a voice‐only German coach for beginners in Linz, Austria.",
            "DIALOGUE" to "Explain what '{newTarget.token}' means in a very short sentence.\nGive one simple example with '{newTarget.token}'."
        )
    }


    @Test
    fun `build generates correct JSON for initial prompt`() {
        val newTarget = LearningItem("german_CP001", "Entschuldigung", 0, 0)
        val learnedPool = mutableListOf(
            LearningItem("german_AA002", "sehr", 6, 4),
            LearningItem("german_AA003", "viel", 4, 7)
        )
        val queues = Queues(mutableListOf(newTarget), learnedPool)

        val expectedBody = listOf(
            "You are now “Lango,” a voice‐only German coach for beginners in Linz, Austria.",
            "Explain what 'Entschuldigung' means in a very short sentence.
Give one simple example with 'Entschuldigung'."
        )

        val result = initialPromptBuilder.build(queues, java.util.UUID.fromString("00000000-0000-0000-0000-000000000000"))
        val actualPrompt = json.decodeFromString<Prompt>(result)

        assertEquals(expectedBody, actualPrompt.body)
    }
}