package com.example.lango_mvp_android.di

import com.example.domain.LearningRepository
import com.example.domain.LlmService
import com.example.domain.TtsService
import com.example.testing.TestFixtures.queuesFixture
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
    replaces = [RepositoryModule::class]
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
    fun provideLlmService(): LlmService {
        return mockk(relaxed = true)
    }

    @Provides
    @Singleton
    fun provideTtsService(): TtsService {
        return mockk(relaxed = true)
    }
}