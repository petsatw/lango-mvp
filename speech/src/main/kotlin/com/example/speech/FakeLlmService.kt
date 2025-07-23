package com.example.speech

import com.example.domain.LlmService
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import javax.inject.Inject

class FakeLlmService @Inject constructor(
    private val json: Json
) : LlmService {

    override suspend fun generateDialogue(prompt: String): String {
        // For simplicity, we'll just return a canned response for now.
        // In a real fake, you might have more sophisticated logic.
        val cannedResponse = """
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "Hallo! Das ist ein Gruß."
                  }
                }
              ]
            }
        """
        val chatCompletionResponse = json.decodeFromString<ChatCompletionResponse>(cannedResponse)
        return chatCompletionResponse.choices.first().message.content
    }
}