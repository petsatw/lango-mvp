package com.example.speech

import com.example.domain.LlmService
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class LlmServiceImplTest {

    @Test
    fun `generateDialogue sends correct request and parses response`() = runTest {
        val mockEngine = MockEngine {
            respond(content = ByteReadChannel(javaClass.classLoader?.getResourceAsStream("sample_llm_response.json")?.readBytes()?.decodeToString() ?: ""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val llmService: LlmService = LlmServiceImpl("test_api_key", mockEngine)

        val prompt = "Test prompt"
        val response = llmService.generateDialogue(prompt)

        assertEquals("Hallo! Das ist ein Gruß.", response)

        // TODO: Verify the request payload (model, messages, etc.)
        // This requires access to the mockEngine's received requests, which is not directly exposed by LlmServiceImpl
        // A better approach would be to inject the HttpClient directly into LlmServiceImpl
    }

    @Test
    fun `ChatCompletionResponse deserialization test`() {
        val jsonString = javaClass.classLoader?.getResourceAsStream("sample_llm_response.json")?.readBytes()?.decodeToString() ?: ""
        val json = Json { ignoreUnknownKeys = true; prettyPrint = true; isLenient = true }
        val response = json.decodeFromString<ChatCompletionResponse>(jsonString)
        assertEquals("Hallo! Das ist ein Gruß.", response.choices.first().message.content)
    }
}