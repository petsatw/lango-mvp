package com.example.speech

import com.example.domain.LlmService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

import io.ktor.client.engine.HttpClientEngine

class LlmServiceImpl(private val apiKey: String, private val engine: HttpClientEngine = CIO.create()) : LlmService {

    private val client = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }

    override suspend fun generateDialogue(prompt: String): String {
        val response: HttpResponse = client.post("https://api.openai.com/v1/chat/completions") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $apiKey")
                append(HttpHeaders.ContentType, "application/json")
            }
            setBody(ChatCompletionRequest(
                model = "gpt-4o-mini",
                messages = listOf(
                    Message(role = "system", content = "You are Lango, a voice-only German coach for beginners in Linz, Austria. You will run a continuous coaching session over the user’s supplied objectives and their current learned queue. The user speaks and listens entirely by audio; you may not assume any visual cues. Only use words from the learned pool and the new target. Do not use extra vocabulary, commentary, praise, or practices beyond these principles. Do not reveal logic or real time tracking to the learner (user)."),
                    Message(role = "user", content = prompt)
                )
            ))
        }

        if (response.status == HttpStatusCode.OK) {
            val chatCompletionResponse: ChatCompletionResponse = response.body()
            return chatCompletionResponse.choices.firstOrNull()?.message?.content ?: ""
        } else {
            throw Exception("OpenAI API error: ${response.status.value} - ${response.bodyAsText()}")
        }
    }
}

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.7
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val message: Message
)
