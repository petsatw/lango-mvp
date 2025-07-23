package com.example.lango_mvp_android.di

import com.example.domain.CoachOrchestrator
import com.example.domain.LearningRepository
import com.example.domain.LlmService
import com.example.domain.TtsService
import com.example.speech.FakeLlmService
import com.example.speech.FakeTtsService
import com.example.testing.TestFixtures.queuesFixture
import kotlinx.serialization.json.Json
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.coEvery
import io.mockk.mockk
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class, CoachOrchestratorModule::class]
)
obect TestAppModule {

    @Provides
    @Singleton
    fun provideLearningRepository(): LearningRepository {
        return mockk {
            coEvery { loadQueues() } returns Result.success(queuesFixture())
            coEvery { saveQueues(any()) } returns Result.success(Unit)
        }
    }

    @Provides
    @Singleton
    fun provideLlmService(json: Json): LlmService {
        return FakeLlmService(json)
    }

    @Provides
    @Singleton
    fun provideTtsService(): TtsService {
        return FakeTtsService()
    }

    @Provides
    @Singleton
    fun provideCoachOrchestrator(): CoachOrchestrator {
        return mockk(relaxed = true)
    }
}