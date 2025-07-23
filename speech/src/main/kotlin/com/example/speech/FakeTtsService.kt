package com.example.speech

import com.example.domain.TtsService
import javax.inject.Inject

class FakeTtsService @Inject constructor() : TtsService {
    override suspend fun speak(text: String) {
        // Simulate successful speech without actual audio playback
        println("FakeTtsService: Speaking \"$text\"")
    }
}