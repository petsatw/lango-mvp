package com.example.speech

import com.example.domain.TtsService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import android.media.MediaPlayer
import java.io.File
import java.io.FileOutputStream

class TtsServiceImpl(private val apiKey: String, private val engine: HttpClientEngine = CIO.create(), private val mediaPlayerFactory: () -> MediaPlayer = { MediaPlayer() }) : TtsService {

    private val client = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }

    override suspend fun speak(text: String) {
        val response: HttpResponse = client.post("https://api.openai.com/v1/audio/speech") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $apiKey")
                append(HttpHeaders.ContentType, "application/json")
            }
            setBody(mapOf(
                "model" to "tts-1",
                "voice" to "alloy",
                "input" to text
            ))
        }

        if (response.status == HttpStatusCode.OK) {
            val audioBytes = response.body<ByteArray>()
            // Save to a temporary file and play
            val tempFile = File.createTempFile("tts_audio", ".mp3")
            FileOutputStream(tempFile).use { it.write(audioBytes) }

            mediaPlayerFactory().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener { mp ->
                    mp.release()
                    tempFile.delete()
                }
            }
        } else {
            throw Exception("OpenAI TTS API error: ${response.status.value} - ${response.bodyAsText()}")
        }
    }
}