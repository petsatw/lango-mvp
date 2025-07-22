package com.example.lango_mvp_android.di

import android.content.Context
import android.content.res.AssetManager
import com.example.data.LearningRepositoryImpl
import com.example.domain.InitialPromptBuilder
import com.example.domain.LearningRepository
import com.example.domain.InitialPromptBuilderImpl
import com.example.domain.NoopInitialPromptBuilder
import com.example.data.PromptTemplateLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides @Singleton
    fun provideLearningRepository(
        assetManager: AssetManager,
        @ApplicationContext ctx: Context,
        json: Json
    ): LearningRepository =
        LearningRepositoryImpl(assetManager, ctx.filesDir, json)

    @Provides @Singleton
    fun providePromptTemplateLoader(assetManager: AssetManager): PromptTemplateLoader = PromptTemplateLoader(assetManager)

    @Provides @Singleton
    fun provideInitialPromptBuilder(json: Json, promptTemplateLoader: PromptTemplateLoader): InitialPromptBuilder = InitialPromptBuilderImpl(json, promptTemplateLoader)
}