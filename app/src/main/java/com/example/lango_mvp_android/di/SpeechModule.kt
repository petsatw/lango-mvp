package com.example.lango_mvp_android.di

import com.example.domain.LlmService
import com.example.domain.TtsService
import com.example.speech.LlmServiceImpl
import com.example.speech.TtsServiceImpl
import com.example.speech.FakeLlmService
import com.example.speech.FakeTtsService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.lango_mvp_android.BuildConfig

@Module
@InstallIn(SingletonComponent::class)
object SpeechModule {

    @Provides
    @Singleton
    fun provideLlmService(json: Json): LlmService {
        return if (BuildConfig.DEBUG) {
            FakeLlmService(json)
        } else {
            LlmServiceImpl(BuildConfig.OPENAI_API_KEY)
        }
    }

    @Provides
    @Singleton
    fun provideTtsService(@ApplicationContext context: Context): TtsService {
        return if (BuildConfig.DEBUG) {
            FakeTtsService()
        } else {
            TtsServiceImpl(BuildConfig.OPENAI_API_KEY)
        }
    }
}